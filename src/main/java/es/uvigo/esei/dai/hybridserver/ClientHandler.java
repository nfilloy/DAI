package es.uvigo.esei.dai.hybridserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

class ClientHandler implements Runnable {
  private final Socket socket;
  private final Supplier<PageRepository> repositoryFactory;
  private final String welcomePage;

  ClientHandler(Socket socket, Supplier<PageRepository> repositoryFactory, String welcomePage) {
    this.socket = socket;
    this.repositoryFactory = repositoryFactory;
    this.welcomePage = welcomePage;
  }

  @Override
  public void run() {
    try (Socket client = this.socket;
      InputStreamReader reader = new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8);
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

      final PageRepository repository = this.repositoryFactory.get();
      if (repository == null) {
        throw new IllegalStateException("Repository factory returned null");
      }

      HTTPResponse response;

      try {
        final HTTPRequest request = new HTTPRequest(reader);
        response = repository.handle(request, this.welcomePage);
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

  private static HTTPResponse createErrorResponse(String version, HTTPResponseStatus status, String message) {
    final String body = new StringBuilder()
      .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>")
      .append(status.getStatus()).append("</title></head><body><h1>")
      .append(status.getCode()).append(' ').append(status.getStatus()).append("</h1><p>")
      .append(message).append("</p></body></html>")
      .toString();

    return createHtmlResponse(version, status, body);
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
}
