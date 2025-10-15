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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Launcher {
  public static void main(String[] args) {
    Properties properties = null;

    if (args.length > 0) {
      final String configPath = args[0];
      try (InputStream input = new FileInputStream(configPath)) {
        properties = new Properties();
        properties.load(input);
      } catch (IOException e) {
        System.err.println("No se pudo cargar el fichero de configuracion '" + configPath + "'. Se usara la configuracion por defecto.");
        properties = null;
      }
    }

    try (HybridServer server = properties != null ? new HybridServer(properties) : new HybridServer()) {
      server.start();
      System.out.println("Servidor escuchando en http://localhost:" + server.getPort());
      System.out.println("Pulsa ENTER para parar...");
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
