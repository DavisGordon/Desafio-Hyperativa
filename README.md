# Desafio Hyperativa - Payment API (Secure Edition)

This project is a robust Payment API designed for high security and scalability, implementing best practices for handling sensitive credit card data.

## Highlights

* ☕ **Java 21 & Spring Boot 3.2**: Leveraging the latest features for performance and stability.
* 🛡️ **Spring Security + JWT + BCrypt**: Comprehensive security model with stateful data protection and stateless session management.
* 🗄️ **Flyway Migrations**: Database schema managed as code for consistent deployments.
* 🚀 **Batch Processing**: Efficient processing of large files using Java Streams.
* 🔒 **AES-256 Encryption**: Sensitive data (credit card numbers) is encrypted at rest.

## How to Run (Docker - Recommended)

The easiest way to run the entire stack (Application + Database) is using Docker Compose.

1.  Make sure you have Docker and Docker Compose installed.
2.  Run the following command in the root directory:

    ```bash
    docker-compose up --build
    ```

    *This command will build the Java application and start both the MySQL database and the API server. The application will wait for the database to be ready before starting.*

## Default Credentials (Crucial)

The application uses Flyway to automatically seed the database with an initial admin user.

* **User:** `admin`
* **Password:** `admin123`

## 📚 API Documentation (Swagger UI)

Once the application is running, you can access the interactive API documentation and test endpoints directly from your browser:

👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

**Note:** Since the API is secured, remember to:
1.  Call the `/auth/login` endpoint first to get a token.
2.  Click the **Authorize** button at the top of the Swagger page.
3.  Paste the token (Bearer Token) to unlock the protected endpoints.

## 🧪 Testing Resources

To facilitate testing of the batch upload feature, a sample file is included in the repository.

* **File Location:** `src/test/resources/cards-to-process.txt`
* **Usage:** You can use this file to test the `POST /api/cards/upload` endpoint via Swagger or Postman.

## API Endpoints

Here are the main endpoints available:

### Authentication
* `POST /auth/login`
    * Authenticates a user and returns a JWT token.

### Cards
* `POST /api/cards`
    * Creates a new credit card manually.
* `POST /api/cards/upload`
    * Uploads a batch TXT file for processing.
* `GET /api/cards/search`
    * Looks up a card by its number (requires authentication).

## Development

If you prefer to run locally without Docker for development:

1.  Ensure you have a MySQL instance running.
2.  Update `application.properties` (or export environment variables) with your database credentials.
3.  Run: `mvn spring-boot:run`