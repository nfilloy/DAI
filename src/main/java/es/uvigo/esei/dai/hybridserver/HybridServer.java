/**
 *  HybridServer
 *  Copyright (C) 2025 Miguel Reboiro-Jato
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.uvigo.esei.dai.hybridserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

public class HybridServer implements AutoCloseable {
  private static final int DEFAULT_PORT = 8888;
  private static final int DEFAULT_POOL_SIZE = Math.max(1, Runtime.getRuntime().availableProcessors());
  private static final String HTML_RESOURCE = "html";
  private static final String DEFAULT_PAGE = "<!DOCTYPE html>\n"
    + "<html lang=\"es\">\n"
    + "  <head>\n"
    + "    <meta charset=\"UTF-8\"/>\n"
    + "    <title>Hybrid Server</title>\n"
    + "  </head>\n"
    + "  <body>\n"
    + "    <h1>Hybrid Server</h1>\n"
    + "    <p>Bienvenido a Hybrid Server.</p>\n"
    + "  </body>\n"
    + "</html>\n";

  private final PageRepository pageRepository;
  private final String welcomePage;
  private final int workerThreads;
  private final int port;

  private ExecutorService executor;
  private Thread serverThread;
  private volatile boolean stop;

  public HybridServer() {
    this(DEFAULT_PORT, DEFAULT_PAGE, createInMemoryRepository(defaultPages()), DEFAULT_POOL_SIZE);
  }

  public HybridServer(Map<String, String> pages) {
    this(DEFAULT_PORT, DEFAULT_PAGE, createInMemoryRepository(pages != null && !pages.isEmpty() ? pages : defaultPages()), DEFAULT_POOL_SIZE);
  }

  public HybridServer(Properties properties) {
    final int port = parsePort(properties);
    final String welcome = extractWelcomePage(properties);
    final int poolSize = parseWorkerThreads(properties);
    final PageRepository repository = createRepository(properties);

    this.port = port;
    this.welcomePage = welcome;
    this.pageRepository = repository;
    this.workerThreads = Math.max(1, poolSize);
  }

  private HybridServer(int port, String welcomePage, PageRepository repository, int workerThreads) {
    this.port = port;
    this.welcomePage = welcomePage != null ? welcomePage : DEFAULT_PAGE;
    this.pageRepository = repository;
    this.workerThreads = Math.max(1, workerThreads);
  }

  public int getPort() {
    return this.port;
  }

  public void start() {
    if (this.executor != null) {
      throw new IllegalStateException("Server already started");
    }

    this.stop = false;
    this.executor = Executors.newFixedThreadPool(this.workerThreads);
    this.serverThread = new Thread(() -> {
      try (ServerSocket serverSocket = new ServerSocket(this.port)) {
        while (true) {
          final Socket socket = serverSocket.accept();
          if (stop) {
            socket.close();
            break;
          }

          this.executor.execute(() -> this.processClient(socket));
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }, "HybridServer-Acceptor");

    this.serverThread.start();
  }

  private void processClient(Socket socket) {
    try (Socket client = socket;
      InputStreamReader reader = new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

      HTTPResponse response;

      try {
        final HTTPRequest request = new HTTPRequest(reader);
        response = this.handleRequest(request);
      } catch (HTTPParseException e) {
        response = createErrorResponse(HTTPHeaders.HTTP_1_1.getHeader(), HTTPResponseStatus.S400, "La peticion no es valida");
      } catch (Exception unexpected) {
        response = createErrorResponse(HTTPHeaders.HTTP_1_1.getHeader(), HTTPResponseStatus.S500, "Error interno del servidor");
        unexpected.printStackTrace();
      }

      response.print(writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private HTTPResponse handleRequest(HTTPRequest request) {
    switch (request.getMethod()) {
      case GET:
        return handleGet(request);
      case POST:
        return handlePost(request);
      case DELETE:
        return handleDelete(request);
      default:
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S405, "Metodo no soportado");
    }
  }

  private HTTPResponse handleGet(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length == 0) {
      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, this.welcomePage);
    }

    if (path.length == 1 && HTML_RESOURCE.equalsIgnoreCase(path[0])) {
      final String uuid = request.getResourceParameters().get("uuid");
      if (uuid == null || uuid.isBlank()) {
        return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, renderPageList());
      }

      final String page = this.pageRepository.getPage(uuid);
      if (page == null) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
      }

      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, page);
    }

    return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
  }

  private HTTPResponse handlePost(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !HTML_RESOURCE.equalsIgnoreCase(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String content = request.getResourceParameters().get("html");
    if (content == null || content.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Contenido invalido");
    }

    final String uuid = this.pageRepository.createPage(content);
    final String body = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina almacenada</title></head><body>")
      .append("<p>La pagina se ha almacenado correctamente.</p>")
      .append("<p><a href=\"html?uuid=").append(uuid).append("\">")
      .append(uuid).append("</a></p>")
      .append("</body></html>")
      .toString();

    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private HTTPResponse handleDelete(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !HTML_RESOURCE.equalsIgnoreCase(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String uuid = request.getResourceParameters().get("uuid");
    if (uuid == null || uuid.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Identificador invalido");
    }

    if (!this.pageRepository.deletePage(uuid)) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
    }

    final String body = "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina eliminada</title></head><body><p>La pagina se ha eliminado.</p></body></html>";
    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private String renderPageList() {
    final StringBuilder sb = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Paginas disponibles</title></head><body>")
      .append("<h1>Paginas disponibles</h1><ul>");

    for (String uuid : this.pageRepository.listUuids()) {
      sb.append("<li><a href=\"html?uuid=").append(uuid).append("\">").append(uuid).append("</a></li>");
    }

    return sb.append("</ul></body></html>").toString();
  }

  private static HTTPResponse createHtmlResponse(String version, HTTPResponseStatus status, String content) {
    final HTTPResponse response = new HTTPResponse();
    response.setVersion(version != null ? version : HTTPHeaders.HTTP_1_1.getHeader());
    response.setStatus(status);
    response.setContent(content);
    response.putParameter(HTTPHeaders.CONNECTION.getHeader(), "close");
    if (content != null) {
      response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime());
    }
    return response;
  }

  private static HTTPResponse createErrorResponse(String version, HTTPResponseStatus status, String message) {
    final String body = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>")
      .append(status.getStatus()).append("</title></head><body><h1>")
      .append(status.getCode()).append(' ').append(status.getStatus()).append("</h1><p>")
      .append(message).append("</p></body></html>")
      .toString();

    return createHtmlResponse(version, status, body);
  }

  @Override
  public void close() {
    if (this.serverThread == null) {
      return;
    }

    this.stop = true;

    try (Socket socket = new Socket("localhost", this.port)) {
      // Despierta al hilo aceptador
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    this.serverThread = null;
    shutdownExecutor();
  }

  private void shutdownExecutor() {
    if (this.executor == null) {
      return;
    }

    this.executor.shutdown();
    try {
      if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
        this.executor.shutdownNow();
        this.executor.awaitTermination(5, TimeUnit.SECONDS);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      this.executor.shutdownNow();
    }

    this.executor = null;
  }

  private static Map<String, String> extractPages(Properties properties) {
    if (properties == null) {
      return new LinkedHashMap<>();
    }

    final Map<String, String> pages = new LinkedHashMap<>();
    for (String name : properties.stringPropertyNames()) {
      if (name.startsWith("page.")) {
        final String uuid = name.substring("page.".length()).trim();
        if (!uuid.isEmpty()) {
          pages.put(uuid, properties.getProperty(name));
        }
      }
    }
    return pages;
  }

  private static PageRepository createRepository(Properties properties) {
    if (properties != null) {
      final String databaseUrl = properties.getProperty("db.url");
      if (databaseUrl != null && !databaseUrl.isBlank()) {
        final String user = properties.getProperty("db.user");
        final String password = properties.getProperty("db.password");
        return new DatabasePageRepository(databaseUrl, user, password);
      }
    }

    final Map<String, String> pages = extractPages(properties);
    if (pages.isEmpty()) {
      pages.putAll(defaultPages());
    }

    return createInMemoryRepository(pages);
  }

  private static PageRepository createInMemoryRepository(Map<String, String> pages) {
    return new InMemoryPageRepository(pages);
  }

  private static Map<String, String> defaultPages() {
    final Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put("12345", "pagina 12345");
    defaults.put("9898", "paguina 9898");
    return defaults;
  }

  private static String extractWelcomePage(Properties properties) {
    if (properties == null) {
      return DEFAULT_PAGE;
    }

    final String value = properties.getProperty("welcome.page");
    return value != null && !value.isBlank() ? value : DEFAULT_PAGE;
  }

  private static int parsePort(Properties properties) {
    if (properties == null) {
      return DEFAULT_PORT;
    }

    final String value = properties.getProperty("port");
    if (value == null) {
      return DEFAULT_PORT;
    }

    try {
      final int port = Integer.parseInt(value.trim());
      return port > 0 ? port : DEFAULT_PORT;
    } catch (NumberFormatException ex) {
      return DEFAULT_PORT;
    }
  }

  private static int parseWorkerThreads(Properties properties) {
    if (properties == null) {
      return DEFAULT_POOL_SIZE;
    }

    final String value = properties.getProperty("numClients");
    if (value == null) {
      return DEFAULT_POOL_SIZE;
    }

    try {
      final int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : DEFAULT_POOL_SIZE;
    } catch (NumberFormatException ex) {
      return DEFAULT_POOL_SIZE;
    }
  }
}
