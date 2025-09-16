# Conecta Brasil Backend

Backend API for the Conecta Brasil project, developed in Java Spring Boot with Stellar blockchain integration.

## 🚀 Deploy

The application is deployed on Google Cloud Run:

**Production URL:** https://conecta-brasil-api-379307696946.us-central1.run.app

## 📚 API Documentation

### Swagger UI
Access the interactive API documentation:
- **Local:** http://localhost:8080/swagger-ui/index.html
- **Production:** https://conecta-brasil-api-379307696946.us-central1.run.app/swagger-ui/index.html

### Postman Collections
Import the collections to test the API:
- **Collections Folder:** [collection/](./collection/)
- **Main File:** [conecta_brasil_backend.postman_collection.json](./collection/conecta_brasil_backend.postman_collection.json)

## 🏗️ Project Architecture

### Folder Structure
```
src/main/java/com/conectabrasil/
├── adapter/
│   └── inboud/rest/          # REST Controllers
├── application/
│   └── usecase/              # Application use cases
├── config/                   # Spring configurations
├── domain/                   # Domain entities
└── infrastructure/
    └── stellar/              # Stellar integration
```

### Technologies Used
- **Java 21**
- **Spring Boot 3.5.5**
- **Stellar SDK 2.0.0**
- **Maven**
- **Docker**

## 🌟 Features

### Main Endpoints

#### Packages
- `GET /packages` - List all available packages
- `GET /packages/user/{userAddress}` - Packages for a specific user
- `GET /packages/remaining/{ownerAddress}/{orderId}` - Remaining time for an order
- `GET /packages/order-session/{ownerAddress}/{orderId}` - Order session information

#### Order Operations
- `POST /packages/start-order` - Start an order
- `POST /packages/pause-order` - Pause an order

### Stellar Integration

The project integrates with the Stellar blockchain through Soroban (smart contracts):

- **Network:** Stellar Testnet/Mainnet
- **Contracts:** Soroban smart contracts
- **Operations:** Contract function invocation
- **Transactions:** Simulation and network submission

## 🛠️ Local Development

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker (optional)

### Running Locally

```bash
# Clone the repository
git clone <repository-url>
cd conecta-brasil-backend

# Run with Maven
./mvnw spring-boot:run

# Or compile and run the JAR
./mvnw package
java -jar target/conectabrasil-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
# Build the image
docker build -t conecta-brasil-backend .

# Run the container
docker run -p 8080:8080 conecta-brasil-backend
```

## ⚙️ Configuration

### Environment Variables

```properties
# Stellar configurations
STELLAR_NETWORK_PASSPHRASE=Test SDF Network ; September 2015
STELLAR_HORIZON_URL=https://horizon-testnet.stellar.org
STELLAR_SOROBAN_RPC_URL=https://soroban-testnet.stellar.org
STELLAR_CONTRACT_ADDRESS=<contract-address>

# Application configurations
SERVER_PORT=8080
```

## 🔧 Business Rules

### Packages
- Each package has a unique ID and specific information
- Users can have multiple active packages
- Packages have states (active/inactive)

### Orders
- Orders are identified by a unique ID (U128)
- Can be started and paused
- Have controlled session time
- Integrate with Stellar smart contracts

### Stellar Transactions
- All operations are simulated before submission
- Support for authorizations when necessary
- Stellar network-specific error handling
- Parsing of SCVal results to JSON formats

## 🧪 Testing

```bash
# Run tests
./mvnw test

# Run with coverage report
./mvnw test jacoco:report
```

## 📦 Build and Deploy

### Local Build
```bash
./mvnw clean package
```

### Docker Deploy
```bash
# Multi-platform build
docker buildx build --platform linux/amd64 -t conecta-brasil-backend .

# Push to registry
docker buildx build --platform linux/amd64 -t "$IMAGE" . --push
```

## 🤝 Contributing

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is under the MIT license. See the `LICENSE` file for more details.

## 📞 Contact

For questions or support, contact us through the official Conecta Brasil project channels.

---

**Developer**
- Huggo Oliveira
- LinkedIn: [Huggo Oliveira](https://www.linkedin.com/in/huggo-oliveira/)
- GitHub: [Huggo Oliveira](https://github.com/huggo-oliveira)

**Conecta Brasil** - Connecting Brazil through Stellar blockchain technology.