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

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

public class HybridServer implements AutoCloseable {
  private static final int SERVICE_PORT = 8888;
  private static final String DEFAULT_PAGE = "<!DOCTYPE html>\n"
    + "<html lang=\"es\">\n"
    + "  <head>\n"
    + "    <meta charset=\"UTF-8\"/>\n"
    + "    <title>Servidor Hibrido</title>\n"
    + "  </head>\n"
    + "  <body>\n"
    + "    <h1>Bienvenido</h1>\n"
    + "    <p>Este servidor siempre devuelve la misma pagina.</p>\n"
    + "  </body>\n"
    + "</html>\n";

  private final Map<String, String> pages;
  private final String responseBody;

  private Thread serverThread;
  private boolean stop;

  public HybridServer() {
    this(createDefaultPages());
  }

  public HybridServer(Map<String, String> pages) {
    this.pages = new LinkedHashMap<>();
    if (pages != null) {
      this.pages.putAll(pages);
    }
    if (this.pages.isEmpty()) {
      this.pages.put("/", DEFAULT_PAGE);
    }
    this.responseBody = selectDefaultPage(this.pages);
  }

  public HybridServer(Properties properties) {
    this(propertiesToMap(properties));
  }

  public int getPort() {
    return SERVICE_PORT;
  }

  public void start() {
    this.serverThread = new Thread() {
      @Override
      public void run() {
        try (final ServerSocket serverSocket = new ServerSocket(SERVICE_PORT)) {
          while (true) {
            try (Socket socket = serverSocket.accept()) {
              if (stop)
                break;

              handleClient(socket);
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };

    this.stop = false;
    this.serverThread.start();
  }

  private void handleClient(Socket socket) throws IOException {
    HTTPResponse response = createErrorResponse(HTTPResponseStatus.S400, "La peticion no es valida");

    try {
      final HTTPRequest request = new HTTPRequest(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      response = createOkResponse(request.getHttpVersion());
    } catch (HTTPParseException e) {
      // keep default error response when parsing fails
    }

    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
      response.print(writer);
    }
  }

  private HTTPResponse createOkResponse(String httpVersion) {
    final HTTPResponse response = new HTTPResponse();
    response.setVersion(httpVersion);
    response.setStatus(HTTPResponseStatus.S200);
    response.setContent(this.responseBody);
    response.putParameter(HTTPHeaders.CONNECTION.getHeader(), "close");
    response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime() + "; charset=UTF-8");
    return response;
  }

  private HTTPResponse createErrorResponse(HTTPResponseStatus status, String message) {
    final HTTPResponse response = new HTTPResponse();
    response.setStatus(status);
    response.setContent("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>" + status.getStatus()
      + "</title></head><body><h1>" + status.getCode() + " " + status.getStatus() + "</h1><p>"
      + message + "</p></body></html>\n");
    response.putParameter(HTTPHeaders.CONNECTION.getHeader(), "close");
    response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime() + "; charset=UTF-8");
    return response;
  }

  private static Map<String, String> createDefaultPages() {
    final Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put("/", DEFAULT_PAGE);
    defaults.put("index.html", DEFAULT_PAGE);
    return defaults;
  }

  private static String selectDefaultPage(Map<String, String> pages) {
    if (pages.containsKey("/")) {
      return pages.get("/");
    }

    for (Map.Entry<String, String> entry : pages.entrySet()) {
      if (entry.getValue() != null) {
        return entry.getValue();
      }
    }

    return DEFAULT_PAGE;
  }

  private static Map<String, String> propertiesToMap(Properties properties) {
    final Map<String, String> map = new LinkedHashMap<>();
    if (properties != null) {
      for (String name : properties.stringPropertyNames()) {
        map.put(name, properties.getProperty(name));
      }
    }
    return map;
  }

  @Override
  public void close() {
    this.stop = true;

    try (Socket socket = new Socket("localhost", SERVICE_PORT)) {
      // Esta conexión se hace, simplemente, para "despertar" el hilo servidor
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      this.serverThread.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    this.serverThread = null;
  }
}
