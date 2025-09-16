package com.conectabrasil.infrastructure.stellar;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.stellar.sdk.Address;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.SorobanServer;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.operations.InvokeHostFunctionOperation;
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
}

