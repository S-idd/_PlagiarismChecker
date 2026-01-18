# PlagiarismChecker

PlagiarismChecker is an advanced plagiarism detection tool for source code, built with Spring Boot. It analyzes code similarity with robust algorithms, including cosine similarity and trigram analysis, and supports multi-language file comparison for educational and professional use.

## Features

- **Multi-language Support**: Supports various programming languages including Java, Python, C++, JavaScript, and more
- **File Upload**: Upload single or multiple code files for analysis
- **Similarity Detection**: Advanced similarity calculation using cosine similarity and trigram analysis
- **Batch Processing**: Compare multiple files simultaneously
- **REST API**: Complete RESTful API for integration with other systems
- **Caching**: Built-in caching for improved performance
- **Pagination**: Efficient handling of large datasets
- **Validation**: Comprehensive input validation and error handling
- **Database Storage**: Persistent storage of uploaded files and analysis results

## Technology Stack

- **Backend**: Spring Boot 3.3.5
- **Java Version**: 24
- **Database**: JPA/Hibernate with configurable database support
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, AssertJ
- **Caching**: Spring Cache
- **Validation**: Bean Validation (JSR-303)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Database (H2 for development by default, configurable for PostgreSQL and other relational databases)

## Installation & Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd PlagiarismChecker
   ```

2. **Navigate to the project directory**
   ```bash
   cd PlagiarismChecker
   ```

3. **Build the project**
   ```bash
   ./mvnw clean install
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8080`

## API Endpoints

### File Upload

#### Upload Single File
```http
POST /api/code-files/upload
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (code file to upload)
- language: String (programming language)
```

#### Upload Multiple Files
```http
POST /api/code-files/upload-batch
Content-Type: multipart/form-data

Parameters:
- files: List<MultipartFile>
- language: String (programming language)
```

### File Management

#### Get All Files
```http
GET /api/code-files?page=0&size=10
```

#### Get File by ID
```http
GET /api/code-files/{id}
```

#### Delete All Files
```http
DELETE /api/code-files
```

### Similarity Analysis

#### Calculate Similarity Between Two Files
```http
GET /api/code-files/similarity/{fileId1}/{fileId2}
```

#### Compare File Against All Others
```http
GET /api/code-files/{fileId}/compare-all?page=0&size=10&language=java&minSimilarity=0.5
```

#### Batch Comparison
```http
POST /api/code-files/{targetFileId}/compare-batch
Content-Type: application/json

Body:
{
  "fileIds": [1, 2, 3],
  "languageFilter": "java",
  "minSimilarity": 0.5
}
```

## Supported File Types

- **Java**: `.java`
- **Python**: `.py`
- **C++**: `.cpp`, `.cc`, `.cxx`
- **C**: `.c`
- **JavaScript**: `.js`
- **TypeScript**: `.ts`
- **C#**: `.cs`
- **Go**: `.go`
- **Rust**: `.rs`
- **PHP**: `.php`

## Configuration

### Application Properties

The application can be configured through `application.properties`:

```properties
# Server configuration
server.port=8080

# Database configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# Caching
spring.cache.type=simple
```

## Algorithm Details

### Similarity Detection

The plagiarism checker uses a combination of techniques:

1. **Content Normalization**: Removes comments, normalizes whitespace, and standardizes formatting
2. **Trigram Generation**: Creates character-based trigrams for fine-grained analysis
3. **Cosine Similarity**: Calculates similarity using vector space model
4. **Language-Specific Processing**: Tailored analysis for different programming languages

### Performance Features

- **Caching**: Results are cached to improve response times
- **Batch Processing**: Efficient handling of multiple file comparisons
- **Pagination**: Large result sets are paginated for better performance
- **Content Length Limits**: Files are limited to 50,000 characters for optimal processing

## Testing

Run the test suite:

```bash
./mvnw test
```

## Development

### Folder Structure

```
PlagiarismChecker/
├── .gitattributes
├── .gitignore
├── bin/
│   ├── .gitattributes
│   ├── .gitignore
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── cls
├── logs/
│   ├── application.log
│   ├── application.log.2025-06-07.0.gz
│   ├── application.log.2025-06-08.0.gz
│   ├── application.log.2025-06-11.0.gz
│   └── application.log.2025-06-12.0.gz
├── mvnw
├── mvnw.cmd
└── pom.xml
```

### Project Structure (Java Source)

```
src/
├── main/
│   ├── java/
│   │   └── com/example/PlagiarismChecker/
│   │       ├── Config/          # Configuration classes
│   │       ├── Controller/      # REST controllers
│   │       ├── Service/         # Business logic
│   │       ├── Repository/      # Data access layer
│   │       └── model/           # Entity classes
│   └── resources/
│       └── application.properties
└── test/                        # Test classes
```

### Key Classes

- `CodeFileController`: REST API endpoints
- `CodeFileService`: Core business logic and similarity algorithms
- `CustomCosineSimilarity`: Similarity calculation implementation
- `CodeFile`: Entity model for uploaded files
- `SimilarityResult`: Result model for similarity comparisons

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please open an issue in the repository.
