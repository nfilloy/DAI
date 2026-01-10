/**
 * ClientHandler - Manejador de Conexiones de Clientes
 * 
 * Esta clase implementa Runnable y se ejecuta en el pool de hilos.
 * Cada instancia maneja UNA conexión HTTP individual.
 * 
 * Flujo de ejecución:
 * 1. Lee la petición HTTP del socket
 * 2. Delega el procesamiento al PageRepository
 * 3. Escribe la respuesta HTTP al socket
 * 4. Cierra la conexión
 * 
 * IMPORTANTE para el examen:
 * - Si hay error de parsing → 400 Bad Request
 * - Si hay cualquier otra excepción → 500 Internal Server Error
 * - "El servidor deberá manejar correctamente los errores"
 */
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

  private final Socket socket; // Conexión TCP con el cliente
  private final Supplier<PageRepository> repositoryFactory; // Fábrica de repositorios (permite múltiples
                                                            // implementaciones)
  private final String welcomePage; // Página de bienvenida para servir en /

  /**
   * Constructor
   * 
   * @param socket            - Socket de la conexión con el cliente
   * @param repositoryFactory - Fábrica que crea/devuelve el repositorio a usar
   * @param welcomePage       - HTML de la página de bienvenida
   */
  ClientHandler(Socket socket, Supplier<PageRepository> repositoryFactory, String welcomePage) {
    this.socket = socket;
    this.repositoryFactory = repositoryFactory;
    this.welcomePage = welcomePage;
  }

  /**
   * Método principal - ejecutado por un hilo del pool
   * 
   * Procesa una petición HTTP completa:
   * 1. Abre streams de lectura/escritura con UTF-8
   * 2. Parsea la petición HTTP
   * 3. Delega al repositorio para generar respuesta
   * 4. Envía respuesta al cliente
   * 5. Cierra todo (try-with-resources)
   */
  @Override
  public void run() {
    // try-with-resources para cerrar automáticamente socket, reader y writer
    try (Socket client = this.socket;
        // Reader para leer la petición HTTP
        InputStreamReader reader = new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8);
        // Writer para escribir la respuesta HTTP
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {

      // Obtener el repositorio (DatabasePageRepository o InMemoryPageRepository)
      final PageRepository repository = this.repositoryFactory.get();
      if (repository == null) {
        throw new IllegalStateException("Repository factory returned null");
      }

      HTTPResponse response;

      try {
        // PASO 1: Parsear la petición HTTP
        // HTTPRequest lee del reader y extrae: método, recurso, parámetros, cabeceras,
        // cuerpo
        final HTTPRequest request = new HTTPRequest(reader);

        // PASO 2: Delegar al repositorio
        // El repositorio decide qué hacer según el método y recurso
        response = repository.handle(request, this.welcomePage);

      } catch (HTTPParseException e) {
        // ERROR DE PARSING: La petición no es HTTP válida
        // "si la solicitud es incorrecta, deberá responder con código
        // 400"
        response = createErrorResponse(HTTPHeaders.HTTP_1_1.getHeader(), HTTPResponseStatus.S400,
            "La peticion no es valida");

      } catch (Exception unexpected) {
        // ERROR INESPERADO: Cualquier otra excepción
        // "si se produce un error en el servidor, responder con código
        // 500"
        response = createErrorResponse(HTTPHeaders.HTTP_1_1.getHeader(), HTTPResponseStatus.S500,
            "Error interno del servidor");
        unexpected.printStackTrace(); // Para debug
      }

      // PASO 3: Enviar respuesta al cliente
      response.print(writer);

    } catch (IOException e) {
      // Error de I/O (cliente cerró conexión, etc.)
      e.printStackTrace();
    }
  }

  /**
   * Crea una respuesta HTTP de error con página HTML
   * 
   * @param version - Versión HTTP (normalmente HTTP/1.1)
   * @param status  - Código de estado (400, 404, 500, etc.)
   * @param message - Mensaje descriptivo del error
   * @return HTTPResponse configurada con el error
   */
  private static HTTPResponse createErrorResponse(String version, HTTPResponseStatus status, String message) {
    // Generar HTML para el error
    final String body = new StringBuilder()
        .append("<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\"/><title>")
        .append(status.getStatus()).append("</title></head><body><h1>")
        .append(status.getCode()).append(' ').append(status.getStatus()).append("</h1><p>")
        .append(message).append("</p></body></html>")
        .toString();

    return createHtmlResponse(version, status, body);
  }

  /**
   * Crea una respuesta HTTP con contenido HTML
   * 
   * @param version - Versión HTTP
   * @param status  - Código de estado
   * @param content - Contenido HTML
   * @return HTTPResponse configurada
   */
  private static HTTPResponse createHtmlResponse(String version, HTTPResponseStatus status, String content) {
    final HTTPResponse response = new HTTPResponse();
    response.setVersion(version != null ? version : HTTPHeaders.HTTP_1_1.getHeader());
    response.setStatus(status);
    response.setContent(content);

    // Connection: close - no soportamos keep-alive
    response.putParameter(HTTPHeaders.CONNECTION.getHeader(), "close");

    // Content-Type: text/html
    if (content != null) {
      response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime());
    }
    return response;
  }
}
