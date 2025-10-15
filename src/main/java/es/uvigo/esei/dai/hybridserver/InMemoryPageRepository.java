package es.uvigo.esei.dai.hybridserver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

class InMemoryPageRepository implements PageRepository {
  private final Map<String, String> pages;

  InMemoryPageRepository(Map<String, String> initialPages) {
    final Map<String, String> copy = new LinkedHashMap<>();

    if (initialPages != null) {
      initialPages.forEach((key, value) -> {
        if (key != null && !key.isBlank() && value != null) {
          copy.put(key, value);
        }
      });
    }

    this.pages = Collections.synchronizedMap(copy);
  }

  @Override
  public HTTPResponse handle(HTTPRequest request, String welcomePage) {
    final HTTPRequestMethod method = request.getMethod();

    switch (method) {
      case GET:
        return handleGet(request, welcomePage);
      case POST:
        return handlePost(request);
      case DELETE:
        return handleDelete(request);
      default:
        return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S405, "Metodo no soportado");
    }
  }

  private HTTPResponse handleGet(HTTPRequest request, String welcomePage) {
    final String[] path = request.getResourcePath();
    if (path.length == 0) {
      return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, welcomePage);
    }

    if (path.length == 1 && "html".equalsIgnoreCase(path[0])) {
      final String uuid = request.getResourceParameters().get("uuid");
      if (uuid == null || uuid.isBlank()) {
        return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, renderPageList());
      }

      final String page = getPage(uuid);
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

    final String content = request.getResourceParameters().get("html");
    if (content == null || content.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Contenido invalido");
    }

    final String uuid = createPage(content);
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
    if (path.length != 1 || !"html".equalsIgnoreCase(path[0])) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Recurso no soportado");
    }

    final String uuid = request.getResourceParameters().get("uuid");
    if (uuid == null || uuid.isBlank()) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S400, "Identificador invalido");
    }

    if (!deletePage(uuid)) {
      return createErrorResponse(request.getHttpVersion(), HTTPResponseStatus.S404, "La pagina solicitada no existe");
    }

    final String body = "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Pagina eliminada</title></head><body><p>La pagina se ha eliminado.</p></body></html>";
    return createHtmlResponse(request.getHttpVersion(), HTTPResponseStatus.S200, body);
  }

  private String renderPageList() {
    final StringBuilder sb = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>Paginas disponibles</title></head><body>")
      .append("<h1>Paginas disponibles</h1><ul>");

    synchronized (this.pages) {
      for (String uuid : this.pages.keySet()) {
        sb.append("<li><a href=\"html?uuid=").append(uuid).append("\">")
          .append(uuid).append("</a></li>");
      }
    }

    return sb.append("</ul></body></html>").toString();
  }

  private String getPage(String uuid) {
    synchronized (this.pages) {
      return this.pages.get(uuid);
    }
  }

  private String createPage(String content) {
    synchronized (this.pages) {
      String uuid;
      do {
        uuid = UUID.randomUUID().toString();
      } while (this.pages.containsKey(uuid));

      this.pages.put(uuid, content);
      return uuid;
    }
  }

  private boolean deletePage(String uuid) {
    synchronized (this.pages) {
      return this.pages.remove(uuid) != null;
    }
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
