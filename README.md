# Conecta Brasil Backend

API backend para o projeto Conecta Brasil, desenvolvida em Java Spring Boot com integração à blockchain Stellar.

## 🚀 Deploy

A aplicação está deployada no Google Cloud Run:

**URL de Produção:** https://conecta-brasil-api-379307696946.us-central1.run.app

## 📚 Documentação da API

### Swagger UI
Acesse a documentação interativa da API:
- **Local:** http://localhost:8080/swagger-ui/index.html
- **Produção:** https://conecta-brasil-api-379307696946.us-central1.run.app/swagger-ui/index.html

### Collections Postman
Importe as collections para testar a API:
- **Pasta de Collections:** [collection/](./collection/)
- **Arquivo Principal:** [conecta_brasil_backend.postman_collection.json](./collection/conecta_brasil_backend.postman_collection.json)

## 🏗️ Arquitetura do Projeto

### Estrutura de Pastas
```
src/main/java/com/conectabrasil/
├── adapter/
│   └── inboud/rest/          # Controllers REST
├── application/
│   └── usecase/              # Casos de uso da aplicação
├── config/                   # Configurações do Spring
├── domain/                   # Entidades de domínio
└── infrastructure/
    └── stellar/              # Integração com Stellar
```

### Tecnologias Utilizadas
- **Java 21**
- **Spring Boot 3.5.5**
- **Stellar SDK 2.0.0**
- **Maven**
- **Docker**

## 🌟 Funcionalidades

### Endpoints Principais

#### Pacotes
- `GET /packages` - Lista todos os pacotes disponíveis
- `GET /packages/user/{userAddress}` - Pacotes de um usuário específico
- `GET /packages/remaining/{ownerAddress}/{orderId}` - Tempo restante de uma ordem
- `GET /packages/order-session/{ownerAddress}/{orderId}` - Informações da sessão de uma ordem

#### Operações de Ordem
- `POST /packages/start-order` - Inicia uma ordem
- `POST /packages/pause-order` - Pausa uma ordem

### Integração Stellar

O projeto integra com a blockchain Stellar através do Soroban (smart contracts):

- **Rede:** Testnet/Mainnet Stellar
- **Contratos:** Soroban smart contracts
- **Operações:** Invocação de funções de contrato
- **Transações:** Simulação e envio para a rede

## 🛠️ Desenvolvimento Local

### Pré-requisitos
- Java 21+
- Maven 3.9+
- Docker (opcional)

### Executando Localmente

```bash
# Clone o repositório
git clone <repository-url>
cd conecta-brasil-backend

# Execute com Maven
./mvnw spring-boot:run

# Ou compile e execute o JAR
./mvnw package
java -jar target/conectabrasil-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Build da imagem
docker build -t conecta-brasil-backend .

# Execute o container
docker run -p 8080:8080 conecta-brasil-backend
```

## ⚙️ Configuração

### Variáveis de Ambiente

```properties
# Configurações Stellar
STELLAR_NETWORK_PASSPHRASE=Test SDF Network ; September 2015
STELLAR_HORIZON_URL=https://horizon-testnet.stellar.org
STELLAR_SOROBAN_RPC_URL=https://soroban-testnet.stellar.org
STELLAR_CONTRACT_ADDRESS=<contract-address>

# Configurações da aplicação
SERVER_PORT=8080
```

## 🔧 Regras de Negócio

### Pacotes
- Cada pacote possui um ID único e informações específicas
- Usuários podem ter múltiplos pacotes ativos
- Pacotes têm estados (ativo/inativo)

### Ordens
- Ordens são identificadas por um ID único (U128)
- Podem ser iniciadas e pausadas
- Possuem tempo de sessão controlado
- Integram com smart contracts Stellar

### Transações Stellar
- Todas as operações são simuladas antes do envio
- Suporte a authorizations quando necessário
- Tratamento de erros específicos da rede Stellar
- Parsing de resultados SCVal para formatos JSON

## 🧪 Testes

```bash
# Execute os testes
./mvnw test

# Execute com relatório de cobertura
./mvnw test jacoco:report
```

## 📦 Build e Deploy

### Build Local
```bash
./mvnw clean package
```

### Deploy Docker
```bash
# Build multi-platform
docker buildx build --platform linux/amd64 -t conecta-brasil-backend .

# Push para registry
docker buildx build --platform linux/amd64 -t "$IMAGE" . --push
```

## 🤝 Contribuição


1. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
2. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
3. Push para a branch (`git push origin feature/AmazingFeature`)
4. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 📞 Contato

Para dúvidas ou suporte, entre em contato através dos canais oficiais do projeto Conecta Brasil.

---

**Developer**
- Huggo Oliveira
- LinkedIn: [Huggo Oliveira](https://www.linkedin.com/in/huggo-oliveira/)
- GitHub: [Huggo Oliveira](https://github.com/huggo-oliveira)

**Conecta Brasil** - Conectando o Brasil através da tecnologia blockchain Stellar.