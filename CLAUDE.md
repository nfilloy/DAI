# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Hybrid Server** project (Servidor Híbrido de Documentos Estructurados) for serving structured documents (HTML and XML) through a hybrid client/server and P2P architecture. This is an academic project for the "Distributed Applications and Internet" course at Universidad de Vigo.

**Current State:** First delivery completed (HTML support with database storage). Second delivery (XML, XSLT, XSD, and P2P) is pending.

## Build and Run Commands

### Building and Testing
- `mvn test` - Run tests and generate report at `target/site/surefire-report.html`
- `mvn package` - Create deliverable tarball `hybrid-server-<group.name>.r1.tar.gz` at project root
- `mvn clean` - Clean build artifacts

### Running the Server
- `mvn exec:java` - Run server with default configuration
- `mvn exec:java -Dexec.args="config.props"` - Run server with custom configuration file
- `mvn exec:java -Dexec.args="path/to/config.props"` - Run server with config at specific path

The server entry point is `es.uvigo.esei.dai.hybridserver.Launcher`.

### Database Setup
To set up the MySQL database for testing:
```bash
mysql -u root -p < db/mysql-setup.sql
```

This creates:
- Database: `hstestdb` (utf8mb4)
- User: `hsdb` with password `hsdbpass`
- Table: `HTML` with columns `uuid` (CHAR(36), primary key) and `content` (TEXT)

## Configuration

The server accepts a single configuration file parameter. If no configuration is provided or loading fails, default values are used.

### Configuration Format (First Delivery)
Properties file format with UTF-8 encoding:
```
numClients=50
port=8888
db.url=jdbc:mysql://localhost:3306/hstestdb
db.user=hsdb
db.password=hsdbpass
```

### Default Values
- Port: 8888
- Max concurrent clients: 50
- Database: jdbc:mysql://localhost:3306/hstestdb (user: hsdb, password: hsdbpass)

### Future Configuration (Second Delivery)
Configuration will be migrated to XML format with XSD validation (`configuration.xsd`) and XSLT presentation (`configuration.xslt`).

## Architecture

### Component Overview

**HybridServer** - Main server class managing:
- ServerSocket accepting client connections on configured port
- ExecutorService thread pool (sized by `numClients` config) for concurrent client handling
- PageRepository factory pattern for switching between in-memory and database storage
- Welcome page serving (must work without database connection)

**ClientHandler** - Request processor (Runnable):
- Parses HTTP requests from socket input stream
- Delegates to PageRepository for business logic
- Handles exceptions and returns appropriate HTTP error responses (400, 404, 500)
- Ensures socket and streams are closed properly

**PageRepository** - Interface with two implementations:
- `InMemoryPageRepository` - Simple HashMap-based storage (used for step 2/testing)
- `DatabasePageRepository` - MySQL-backed storage with connection-per-request pattern
- Both implement `handle(HTTPRequest, String welcomePage) -> HTTPResponse`

**HTTP Package** (`es.uvigo.esei.dai.hybridserver.http`):
- `HTTPRequest` - Parses incoming HTTP requests (GET/POST/DELETE), extracts method, resource path, query parameters, headers, body
- `HTTPResponse` - Builds HTTP responses with status codes, headers, content
- `HTTPRequestMethod` - Enum for GET, POST, DELETE, PUT, OPTIONS, HEAD, TRACE
- `HTTPResponseStatus` - Enum for status codes (S200, S400, S404, S500, etc.)
- `HTTPHeaders` - Common HTTP header constants
- `MIME` - MIME type constants (TEXT_HTML, FORM, etc.)
- `HTTPParseException` - Exception for malformed requests

### Request Flow
1. Client connects to server socket
2. HybridServer submits connection to thread pool executor
3. ClientHandler reads and parses HTTP request
4. ClientHandler obtains PageRepository from factory
5. PageRepository handles request based on method and resource
6. ClientHandler writes HTTPResponse back to client
7. Connection closed

### Storage Pattern
The server uses a Supplier-based factory pattern:
- If database config is present: creates new DatabasePageRepository per request (connection-per-request)
- If no database config: creates singleton InMemoryPageRepository shared across requests
- This allows graceful fallback when database is unavailable

### Error Handling Requirements
- Invalid requests return 400 Bad Request
- Missing resources return 404 Not Found
- Server errors (exceptions) return 500 Internal Server Error
- Server must continue running after errors
- Welcome page must work even if database is down

## HTTP API (First Delivery)

### GET /
Returns welcome page with "Hybrid Server" title and authors (Nicolas Filloy Montesino, Jose Carlos Peña Otero). Must work without database.

### GET /html
Lists all HTML pages with clickable links to each page by UUID.

### GET /html?uuid=<uuid>
Returns HTML page with specified UUID. Returns 404 if not found.

### POST /html
Creates new HTML page. Body parameter: `html` (application/x-www-form-urlencoded). Generates UUID using `java.util.UUID`. Returns page with link to newly created resource.

### DELETE /html?uuid=<uuid>
Deletes HTML page with specified UUID. Returns confirmation page. Returns 404 if not found.

## Code Constraints

### DO NOT MODIFY:
- Method signatures in provided classes (Launcher, HybridServer, PageRepository, HTTP package)
- Package structure
- Method names or visibility of existing public/package methods

### Allowed Modifications:
- Method bodies where indicated with comments
- Adding private helper methods within classes
- Implementation details in PageRepository implementations

### No External Libraries
Only use dependencies already in pom.xml:
- Jakarta XML WS API (for future P2P web services)
- MySQL Connector/J
- JAX-WS Runtime
- JUnit, Hamcrest, DBUnit (test only)

## Testing

Tests are in `src/test/java/es/uvigo/esei/dai/hybridserver/`:
- Test suite: `HybridServerFirstReleaseTestSuite.java`
- Tests validate HTTP protocol compliance, content-type headers, error codes
- Database tests require MySQL running with proper setup
- Tests include specific implementation requirements (e.g., exact Content-Type values)

The test configuration in pom.xml includes `testFailureIgnore=true`, so builds succeed even if tests fail, but failing tests may result in grade penalties.

## Future Work (Second Delivery)

The second delivery will add:
1. **XML Support** - GET/POST/DELETE operations on `/xml` resource
2. **XSLT/XSD Support** - Transformation and validation for XML documents
3. **P2P System** - Web services for inter-server communication and distributed document lookup
4. **XML Configuration** - Replace properties file with XML config validated against XSD schema

When implementing P2P, servers will query peers for unknown UUIDs and aggregate results in listing pages, showing both local and remote resources.
