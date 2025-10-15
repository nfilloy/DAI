package es.uvigo.esei.dai.hybridserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

class ClientHandler implements Runnable {
  private final Socket socket;
  private final PageRepository repository;
  private final String welcomePage;

  ClientHandler(Socket socket, PageRepository repository, String welcomePage) {
    this.socket = socket;
    this.repository = repository;
    this.welcomePage = welcomePage;
  }

  @Override
  public void run() {
    try (Socket client = this.socket;
      InputStreamReader reader = new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

      HTTPResponse response;

      try {
        final HTTPRequest request = new HTTPRequest(reader);
        response = handleRequest(request);
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
    final HTTPRequestMethod method = request.getMethod();

    switch (method) {
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

    if (path.length == 1 && "html".equalsIgnoreCase(path[0])) {
      final String uuid = request.getResourceParameters().get("uuid");
      if (uuid == null || uuid.isBlank()) {
        return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, renderPageList());
      }

      final String page = this.repository.getPage(uuid);
      if (page == null) {
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
      }

      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, page);
    }

    return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
  }

  private HTTPResponse handlePost(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !"html".equalsIgnoreCase(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final Map<String, String> params = request.getResourceParameters();
    final String content = params.get("html");
    if (content == null || content.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Contenido invalido");
    }

    final String uuid = this.repository.createPage(content);
    final String body = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina almacenada</title></head><body>")
      .append("<p>La pagina se ha almacenado correctamente.</p>")
      .append("<p><a href=\"html?uuid=").append(uuid).append("\">").append(uuid).append("</a></p>")
      .append("</body></html>")
      .toString();

    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private HTTPResponse handleDelete(HTTPRequest request) {
    final String[] path = request.getResourcePath();
    if (path.length != 1 || !"html".equalsIgnoreCase(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String uuid = request.getResourceParameters().get("uuid");
    if (uuid == null || uuid.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Identificador invalido");
    }

    if (!this.repository.deletePage(uuid)) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
    }

    final String body = "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina eliminada</title></head>"
      + "<body><p>La pagina se ha eliminado.</p></body></html>";

    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private String renderPageList() {
    final List<String> uuids = this.repository.listUuids();
    final StringBuilder sb = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Paginas disponibles</title></head><body>")
      .append("<h1>Paginas disponibles</h1><ul>");

    for (String uuid : uuids) {
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
}
