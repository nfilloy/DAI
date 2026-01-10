/**
 * HybridServer - Punto de entrada de la aplicación
 * 
 * Esta clase es la que se ejecuta cuando arrancas el servidor.
 * Se encarga de:
 * 1. Validar los parámetros de línea de comandos
 * 2. Cargar la configuración (XML o Properties)
 * 3. Crear el servidor con la configuración adecuada
 * 4. Iniciar el servidor y esperar a que el usuario pulse ENTER para pararlo
 */
package es.uvigo.esei.dai.hybridserver;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Launcher {

  /**
   * Método principal - punto de entrada de la aplicación
   * 
   * Uso: java es.uvigo.esei.dai.hybridserver.Launcher [archivo_configuracion]
   * 
   * @param args - Puede ser vacío (usa valores por defecto) o contener la ruta
   *             a un archivo de configuración (.props o .xml)
   */
  public static void main(String[] args) {
    // ERROR: Si hay más de un parámetro, mostrar mensaje de error y salir
    // Requisito: "En el caso de que se reciba más de un parámetro
    // deberá mostrarse un mensaje de error"
    if (args.length > 1) {
      System.err.println("Error: Demasiados parametros.");
      System.err.println("Uso: java es.uvigo.esei.dai.hybridserver.Launcher [archivo_configuracion]");
      System.exit(1);
    }

    HybridServer server = null;

    // CASO 1: Se proporciona un archivo de configuración
    if (args.length > 0) {
      final String configPath = args[0];

      // CASO 1A: Archivo XML (segunda entrega - B4)
      // Los archivos XML deben validarse contra configuration.xsd
      if (configPath.toLowerCase().endsWith(".xml")) {
        try (FileReader reader = new FileReader(configPath)) {
          // XMLConfigurationLoader valida el XML contra el XSD antes de cargarlo
          XMLConfigurationLoader loader = new XMLConfigurationLoader();
          Configuration config = loader.load(reader);
          server = new HybridServer(config);
        } catch (Exception e) {
          System.err.println("Error al cargar el fichero de configuracion XML '" + configPath + "': " + e.getMessage());
          System.exit(1);
        }
      }
      // CASO 1B: Archivo Properties (primera entrega - A6)
      else {
        try (InputStream input = new FileInputStream(configPath)) {
          Properties properties = new Properties();
          properties.load(input);
          server = new HybridServer(properties);
        } catch (IOException e) {
          // Si falla la carga, usar configuración por defecto
          System.err.println("No se pudo cargar el fichero de configuracion '" + configPath
              + "'. Se usara la configuracion por defecto.");
          server = new HybridServer();
        }
      }
    }
    // CASO 2: Sin parámetros -> usar valores por defecto
    // Requisito: "En el caso de que no se le pase un fichero de configuración,
    // se utilizarán los siguientes parámetros como parámetros por defecto"
    else {
      server = new HybridServer();
    }

    // Iniciar servidor y esperar ENTER para parar
    try {
      server.start();
      System.out.println("Servidor escuchando en http://localhost:" + server.getPort());
      System.out.println("Pulsa ENTER para parar...");
      System.in.read(); // Bloquea hasta que el usuario pulse ENTER
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      // Siempre cerrar el servidor al terminar (libera recursos)
      if (server != null) {
        server.close();
      }
    }
  }
}
