package com.conectabrasil.infrastructure.stellar;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.stellar.sdk.Address;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.operations.InvokeHostFunctionOperation;
import org.stellar.sdk.xdr.SCMapEntry;
import org.stellar.sdk.xdr.SCVal;
import org.stellar.sdk.xdr.SCValType;
import org.stellar.sdk.xdr.SorobanAuthorizationEntry;
import org.stellar.sdk.xdr.Uint32;
import org.stellar.sdk.xdr.Uint64;
import org.stellar.sdk.xdr.XdrDataInputStream;
import org.stellar.sdk.xdr.XdrUnsignedHyperInteger;
import org.stellar.sdk.xdr.XdrUnsignedInteger;


@Service
public class SorobanContractService {

    private final StellarConfig stellarConfig;
    private final Server horizon; // Horizon p/ contas e sequência
    private final SorobanServer soroban; // Soroban RPC
    private final Network network;

    public SorobanContractService(StellarConfig cfg) {
        this.stellarConfig = cfg;
        this.horizon = new Server(cfg.getHorizonUrl());
        this.soroban = new SorobanServer(cfg.getSorobanRpcUrl()); // ex.: http://localhost:8000/soroban/rpc
        this.network = new Network(cfg.getNetworkPassphrase());
    }

    /**
     * Monta a transação de invocação do contrato:
     * buy_order(owner: Address, package_id: u32)
     * Simula para obter footprint/resources, injeta os dados Soroban e
     * retorna o Envelope XDR em base64 **sem assinatura** (front assina).
     *
     * @param ownerAccountId G..., conta do usuário (fonte da tx e arg owner)
     * @param packageId      id do pacote (u32)
     * @return base64 do TransactionEnvelope (unsigned)
     */
    public String buildBuyOrderUnsignedXdr(String ownerAccountId, int packageId) throws Exception {
        // 0) Carrega a conta no formato certo (TransactionBuilderAccount) via Soroban
        // RPC
        TransactionBuilderAccount source = soroban.getAccount(ownerAccountId); // SorobanServer
        // se a conta não existir, o SDK lança AccountNotFoundException

        // 1) Argumentos do contrato
        SCVal ownerArg = new Address(ownerAccountId).toSCVal();
        SCVal pkgArg = u32(packageId);

        // 2) Operação de invocação (ajuste para usar seu helper se preferir)
        InvokeHostFunctionOperation op = InvokeHostFunctionOperation
                .invokeContractFunctionOperationBuilder(stellarConfig.getContractAddress(), "buy_order",
                        java.util.List.of(ownerArg, pkgArg))
                .build();

        // 3) Monta a transação (apenas UMA vez) — não assina
        Transaction toSimulate = new TransactionBuilder(source, network)
                .addOperation(op)
                .setBaseFee(100) // base fee; resource fee virá do prepare
                .setTimeout(120)
                .build();

        // 4) Simula → coleta possíveis authorizations exigidas pelo contrato/SAC
        var simulation = soroban.simulateTransaction(toSimulate);
        if (simulation.getError() != null) {
            throw new RuntimeException("simulate error: " + simulation.getError());
        }

        List<SorobanAuthorizationEntry> authorizations = Collections.emptyList();
        if (Objects.nonNull(simulation.getResults()) && !simulation.getResults().isEmpty()
                && simulation.getResults().get(0).getAuth() != null
                && !simulation.getResults().get(0).getAuth().isEmpty()) {
            var authB64 = simulation.getResults().get(0).getAuth();
            authorizations = decodeAuthBase64(authB64);
        } else {
            throw new RuntimeException("No authorizations found");
        }

        // 5) Recria a operação COM auth (se houver)
        var opBuilder = InvokeHostFunctionOperation
                .invokeContractFunctionOperationBuilder(stellarConfig.getContractAddress(), "buy_order",
                        List.of(ownerArg, pkgArg));

        if (!authorizations.isEmpty()) {
            opBuilder.auth(authorizations);
        }
        var operation = opBuilder.build();

        // 6) Recarrega a conta para não queimar a sequence
        source = soroban.getAccount(ownerAccountId);

        // 7) Prepara (injeta sorobanData + resource fee). Ainda SEM assinar.
        var unsigned = new TransactionBuilder(source, network)
                .addOperation(op)
                .setBaseFee(100)
                .setTimeout(120)
                .build();

        unsigned = soroban.prepareTransaction(unsigned);

        // 8) Retorna XDR base64 NÃO ASSINADO (front assina e envia)
        return unsigned.toEnvelopeXdrBase64();

        // 4) Prepara (simula + injeta SorobanTransactionData + resource fee)
        // toSimulate = soroban.prepareTransaction(toSimulate);

        // 5) Retorna XDR base64 não assinado (o front assina e envia)
        // return toSimulate.toEnvelopeXdrBase64();
    }

    /**
     * Versão de teste: monta transação para grant(caller, owner, order_id)
     * Ajuste principal: order_id em U128 (não U32). Usa hi=0, lo=orderId.
     */
    public String buildGrantUnsignedXdrForTest(String callerAccountId, String ownerAccountId, long orderId)
            throws Exception {
        // 0) Carrega a conta fonte (caller) via Soroban RPC
        TransactionBuilderAccount source = soroban.getAccount(callerAccountId);

        // 1) Args do contrato
        SCVal callerArg = new Address(callerAccountId).toSCVal();
        SCVal ownerArg = new Address(ownerAccountId).toSCVal();
        SCVal orderIdArg = u128Lo(orderId); // <<< AJUSTE: U128 (hi=0, lo=orderId)

        // 2) Operação de invocação (sem auth ainda)
        InvokeHostFunctionOperation opNoAuth = InvokeHostFunctionOperation
                .invokeContractFunctionOperationBuilder(
                        stellarConfig.getContractAddress(),
                        "grant",
                        java.util.List.of(callerArg, ownerArg, orderIdArg))
                .build();

        // 3) Simula para coletar possíveis authorizations
        Transaction toSimulate = new TransactionBuilder(source, network)
                .addOperation(opNoAuth)
                .setBaseFee(100)
                .setTimeout(120)
                .build();

        var simulation = soroban.simulateTransaction(toSimulate);
        if (simulation.getError() != null) {
            throw new RuntimeException("simulate error: " + simulation.getError());
        }

        // 4) Decodifica auth (se houver)
        List<SorobanAuthorizationEntry> authorizations = Collections.emptyList();
        if (simulation.getResults() != null && !simulation.getResults().isEmpty()
                && simulation.getResults().get(0).getAuth() != null
                && !simulation.getResults().get(0).getAuth().isEmpty()) {
            authorizations = decodeAuthBase64(simulation.getResults().get(0).getAuth());
        }

        // 5) Recria operação COM auth (quando houver)
        var opBuilder = InvokeHostFunctionOperation
                .invokeContractFunctionOperationBuilder(
                        stellarConfig.getContractAddress(),
                        "grant",
                        java.util.List.of(callerArg, ownerArg, orderIdArg));
        if (!authorizations.isEmpty()) {
            opBuilder.auth(authorizations);
        }
        var opWithAuth = opBuilder.build();

        // 6) Recarrega a conta para não queimar sequence e prepara
        source = soroban.getAccount(callerAccountId);
        Transaction unsigned = new TransactionBuilder(source, network)
                .addOperation(opWithAuth) // <<< usa a operação COM AUTH
                .setBaseFee(100)
                .setTimeout(120)
                .build();

        unsigned = soroban.prepareTransaction(unsigned);

        // 7) Retorna XDR não assinado (carteira assina e envia)
        return unsigned.toEnvelopeXdrBase64();
    }

    // -------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------
    /** Helper: cria SCVal U128 com hi=0 e lo=valor (para orderIds pequenos). */
    private static SCVal u128Lo(long lo) {
        // hi = 0
        Uint64 hi = new Uint64();
        hi.setUint64(new XdrUnsignedHyperInteger(0L));
        // lo = valor
        Uint64 lo64 = new Uint64();
        lo64.setUint64(new XdrUnsignedHyperInteger(lo));

        org.stellar.sdk.xdr.UInt128Parts parts = new org.stellar.sdk.xdr.UInt128Parts();
        parts.setHi(hi);
        parts.setLo(lo64);

        SCVal v = new SCVal();
        v.setDiscriminant(SCValType.SCV_U128);
        v.setU128(parts);
        return v;
    }

    private static List<SorobanAuthorizationEntry> decodeAuthBase64(List<String> authB64) throws Exception {
        if (authB64 == null || authB64.isEmpty())
            return Collections.emptyList();
        List<SorobanAuthorizationEntry> out = new ArrayList<>(authB64.size());
        for (String b64 : authB64) {
            byte[] bytes = Base64.getDecoder().decode(b64);
            try (XdrDataInputStream xin = new XdrDataInputStream(new ByteArrayInputStream(bytes))) {
                out.add(SorobanAuthorizationEntry.decode(xin));
            }
        }
        return out;
    }

    private static SCVal u32(int v) {
        Uint32 u = new Uint32();
        u.setUint32(new XdrUnsignedInteger(v));

        SCVal sc = new SCVal();
        sc.setDiscriminant(SCValType.SCV_U32);
        sc.setU32(u);
        return sc;
    }

    private static SCVal u64(long v) {
        Uint64 u = new Uint64();
        u.setUint64(new XdrUnsignedHyperInteger(v));

        SCVal sc = new SCVal();
        sc.setDiscriminant(SCValType.SCV_U64);
        sc.setU64(u);
        return sc;
    }

    /**
     * Invoca a função get_all_packages do contrato Stellar
     * 
     * @return Lista de pacotes disponíveis
     */
    public List<Object> getAllPackages() throws Exception {
        try {
            // Usa uma conta válida do Stellar para simulação
            // Para view functions, podemos usar qualquer conta válida
            String validAccount = "GBQXJ5OUUJO4DYJ43ZT7FFU4NM4TQGBHLEN7G553WIPQMTN4LX45ZL35";
            TransactionBuilderAccount source = soroban.getAccount(validAccount);

            // Cria operação para invocar get_all_packages (sem argumentos)
            InvokeHostFunctionOperation op = InvokeHostFunctionOperation
                    .invokeContractFunctionOperationBuilder(
                            stellarConfig.getContractAddress(),
                            "get_all_packages",
                            Collections.emptyList())
                    .build();

            // Monta transação para simulação
            Transaction toSimulate = new TransactionBuilder(source, network)
                    .addOperation(op)
                    .setBaseFee(100)
                    .setTimeout(120)
                    .build();

            // Simula a transação
            var simulation = soroban.simulateTransaction(toSimulate);
            if (simulation.getError() != null) {
                throw new RuntimeException("Simulation error: " + simulation.getError());
            }

            // Extrai o resultado da simulação
            if (simulation.getResults() != null && !simulation.getResults().isEmpty()) {
                var result = simulation.getResults().get(0);
                if (result.getXdr() != null) {
                    // Faz o parsing do resultado SCVal
                    return parsePackagesFromSCVal(result.getXdr());
                }
            }

            return Collections.emptyList();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar pacotes do contrato: " + e.getMessage(), e);
        }
    }

    /**
     * Faz o parsing do resultado SCVal para extrair a lista de pacotes
     * Formato esperado: [[id, {duration_secs, is_popular, name, price,
     * speed_message}]]
     */
    private List<Object> parsePackagesFromSCVal(String xdrResult) throws Exception {
        try {
            // Decodifica o XDR base64
            byte[] xdrBytes = Base64.getDecoder().decode(xdrResult);
            XdrDataInputStream xdrStream = new XdrDataInputStream(new ByteArrayInputStream(xdrBytes));

            // Lê o SCVal do resultado
            SCVal resultVal = SCVal.decode(xdrStream);

            List<Object> packages = new ArrayList<>();

            // Verifica se é um vetor (lista)
            if (resultVal.getDiscriminant() == SCValType.SCV_VEC) {
                SCVal[] vec = resultVal.getVec().getSCVec();
                if (vec != null && vec.length > 0) {
                    for (SCVal item : vec) {
                        // Cada item deve ser um vetor [id, package_data]
                        if (item.getDiscriminant() == SCValType.SCV_VEC) {
                            SCVal[] packageVec = item.getVec().getSCVec();
                            if (packageVec != null && packageVec.length >= 2) {
                                Map<String, Object> packageData = new HashMap<>();

                                // Primeiro elemento é o ID
                                if (packageVec[0].getDiscriminant() == SCValType.SCV_U32) {
                                    packageData.put("id", packageVec[0].getU32().getUint32().getNumber().longValue());
                                }

                                // Segundo elemento é o mapa com os dados do pacote
                                if (packageVec[1].getDiscriminant() == SCValType.SCV_MAP) {
                                    SCMapEntry[] map = packageVec[1].getMap().getSCMap();
                                    if (map != null) {
                                        for (SCMapEntry entry : map) {
                                            String key = extractStringFromSCVal(entry.getKey());
                                            Object value = extractValueFromSCVal(entry.getVal());
                                            if (key != null && value != null) {
                                                packageData.put(key, value);
                                            }
                                        }
                                    }
                                }

                                if (!packageData.isEmpty()) {
                                    packages.add(packageData);
                                }
                            }
                        }
                    }
                }
            }

            return packages;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer parsing do resultado SCVal: " + e.getMessage(), e);
        }
    }

    /**
     * Busca os pacotes de um usuário específico
     * Invoca a função get_user_packages do contrato Stellar
     * 
     * @param userAddress Endereço Stellar do usuário
     * @return Lista de pacotes do usuário
     * @throws Exception se houver erro na invocação
     */
    public List<Object> getUserPackages(String userAddress) throws Exception {
        try {
            // Cria uma conta mock temporária para a simulação
            TransactionBuilderAccount sourceAccount = new TransactionBuilderAccount() {
                private long sequenceNumber = 0;
                private KeyPair keyPair = KeyPair.random();

                @Override
                public KeyPair getKeyPair() {
                    return keyPair;
                }

                @Override
                public String getAccountId() {
                    return keyPair.getAccountId();
                }

                @Override
                public Long getSequenceNumber() {
                    return sequenceNumber;
                }

                @Override
                public Long getIncrementedSequenceNumber() {
                    return ++sequenceNumber;
                }

                @Override
                public void incrementSequenceNumber() {
                    sequenceNumber++;
                }

                @Override
                public void setSequenceNumber(long seqNum) {
                    this.sequenceNumber = seqNum;
                }
            };

            // Converte o endereço do usuário para Address
            Address ownerAddress = new Address(userAddress);

            // Cria os argumentos para a função get_user_packages
            SCVal[] args = new SCVal[] {
                    ownerAddress.toSCVal()
            };

            // Monta a operação de invocação
            InvokeHostFunctionOperation operation = InvokeHostFunctionOperation
                    .invokeContractFunctionOperationBuilder(
                            stellarConfig.getContractAddress(),
                            "get_user_packages",
                            java.util.List.of(args))
                    .build();

            // Constrói a transação
            Transaction transaction = new TransactionBuilder(sourceAccount, network)
                    .addOperation(operation)
                    .setTimeout(30)
                    .setBaseFee(100)
                    .build();

            // Simula a transação
            var response = soroban.simulateTransaction(transaction);

            if (response.getError() != null) {
                throw new RuntimeException("Erro na simulação: " + response.getError());
            }

            // Extrai o resultado
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                String result = response.getResults().get(0).getXdr();
                return parseUserPackagesFromSCVal(result);
            }

            return Collections.emptyList();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar pacotes do usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Faz o parsing do resultado SCVal para extrair a lista de pacotes do usuário
     * Formato esperado: [["package_id", package_id_num, is_active]]
     */
    private List<Object> parseUserPackagesFromSCVal(String xdrResult) throws Exception {
        try {
            // Decodifica o XDR base64
            byte[] xdrBytes = Base64.getDecoder().decode(xdrResult);
            XdrDataInputStream xdrStream = new XdrDataInputStream(new ByteArrayInputStream(xdrBytes));

            // Lê o SCVal do resultado
            SCVal resultVal = SCVal.decode(xdrStream);

            List<Object> userPackages = new ArrayList<>();

            // Verifica se é um vetor (lista)
            if (resultVal.getDiscriminant() == SCValType.SCV_VEC) {
                SCVal[] vec = resultVal.getVec().getSCVec();
                if (vec != null && vec.length > 0) {
                    for (SCVal item : vec) {
                        // Cada item deve ser um vetor [package_id_string, package_id_num, is_active]
                        if (item.getDiscriminant() == SCValType.SCV_VEC) {
                            SCVal[] packageVec = item.getVec().getSCVec();
                            if (packageVec != null && packageVec.length >= 3) {
                                Map<String, Object> packageData = new HashMap<>();

                                // Primeiro elemento é o order_id como U128
                                if (packageVec[0].getDiscriminant() == SCValType.SCV_U128) {
                                    packageData.put("order_id",
                                            packageVec[0].getU128().getLo().getUint64().getNumber().longValue());
                                }

                                // Segundo elemento é o package_id como número
                                if (packageVec[1].getDiscriminant() == SCValType.SCV_U32) {
                                    packageData.put("package_id",
                                            packageVec[1].getU32().getUint32().getNumber().longValue());
                                }

                                // Terceiro elemento é se está ativo
                                if (packageVec[2].getDiscriminant() == SCValType.SCV_BOOL) {
                                    packageData.put("is_active", packageVec[2].getB());
                                }

                                if (!packageData.isEmpty()) {
                                    userPackages.add(packageData);
                                }
                            }
                        }
                    }
                }
            }

            return userPackages;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer parsing do resultado SCVal: " + e.getMessage(), e);
        }
    }

    /**
     * Busca o valor restante de uma ordem específica
     */
    public Object getRemainingByOrder(String ownerAddress, long orderId) throws Exception {
        try {
            // Cria uma conta temporária para a transação
            KeyPair keyPair = KeyPair.random();
            TransactionBuilderAccount account = new TransactionBuilderAccount() {
                private long sequenceNumber = 0L;

                @Override
                public String getAccountId() {
                    return keyPair.getAccountId();
                }

                @Override
                public KeyPair getKeyPair() {
                    return keyPair;
                }

                @Override
                public Long getSequenceNumber() {
                    return sequenceNumber;
                }

                @Override
                public Long getIncrementedSequenceNumber() {
                    return sequenceNumber + 1;
                }

                @Override
                public void incrementSequenceNumber() {
                    sequenceNumber++;
                }

                @Override
                public void setSequenceNumber(long sequenceNumber) {
                    this.sequenceNumber = sequenceNumber;
                }
            };

            // Cria os parâmetros da função usando métodos estáticos
            SCVal ownerParam = new Address(ownerAddress).toSCVal();
            SCVal orderIdParam = u128Lo(orderId);
            SCVal nowParam = u64(System.currentTimeMillis() / 1000);

            // Cria a operação de invocação
            InvokeHostFunctionOperation operation = InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
                    stellarConfig.getContractAddress(),
                    "remaining_by_order",
                    java.util.Arrays.asList(ownerParam, orderIdParam, nowParam)).build();

            // Constrói a transação
            Transaction transaction = new TransactionBuilder(account, network)
                    .addOperation(operation)
                    .setTimeout(30)
                    .setBaseFee(100)
                    .build();

            // Simula a transação
            org.stellar.sdk.responses.sorobanrpc.SimulateTransactionResponse response = soroban
                    .simulateTransaction(transaction);

            if (response.getResults() != null && !response.getResults().isEmpty()) {
                String xdrResult = response.getResults().get(0).getXdr();
                return parseRemainingFromSCVal(xdrResult);
            }

            return 0;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar valor restante da ordem: " + e.getMessage(), e);
        }
    }

    /**
     * Faz o parsing do resultado SCVal para extrair o valor restante
     */
    private Object parseRemainingFromSCVal(String xdrResult) throws Exception {
        try {
            // Decodifica o XDR base64
            byte[] xdrBytes = Base64.getDecoder().decode(xdrResult);
            XdrDataInputStream xdrStream = new XdrDataInputStream(new ByteArrayInputStream(xdrBytes));

            // Lê o SCVal do resultado
            SCVal resultVal = SCVal.decode(xdrStream);

            // Extrai o valor baseado no tipo
            Object remainingValue = extractValueFromSCVal(resultVal);

            // Retorna um objeto JSON amigável para o frontend
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("remaining", remainingValue != null ? remainingValue : 0);
            response.put("timestamp", System.currentTimeMillis());
            return response;

        } catch (Exception e) {
            java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("remaining", 0);
            errorResponse.put("error", "Failed to parse remaining value");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
    }

    /**
     * Extrai string de um SCVal
     */
    private String extractStringFromSCVal(SCVal val) {
        if (val.getDiscriminant() == SCValType.SCV_SYMBOL) {
            return val.getSym().getSCSymbol().toString();
        } else if (val.getDiscriminant() == SCValType.SCV_STRING) {
            return val.getStr().getSCString().toString();
        }
        return null;
    }

    /**
     * Extrai valor de um SCVal de forma limpa
     */
    private Object extractValueFromSCVal(SCVal val) {
        switch (val.getDiscriminant()) {
            case SCV_U32:
                return val.getU32().getUint32().getNumber().longValue();
            case SCV_U64:
                return val.getU64().getUint64().getNumber().longValue();
            case SCV_I32:
                return val.getI32().getInt32();
            case SCV_I64:
                return val.getI64().getInt64();
            case SCV_I128:
                // Para I128, extrair apenas o valor low (suficiente para preços)
                return val.getI128().getLo().getUint64().getNumber().longValue();
            case SCV_BOOL:
                return val.getB();
            case SCV_STRING:
                return val.getStr().getSCString().toString();
            case SCV_SYMBOL:
                return val.getSym().getSCSymbol().toString();
            default:
                return null;
        }
    }

}

