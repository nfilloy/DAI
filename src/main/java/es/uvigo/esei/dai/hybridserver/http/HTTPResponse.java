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
package es.uvigo.esei.dai.hybridserver.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class HTTPResponse {
  public HTTPResponse() {
    // TODO Completar
  }

  public HTTPResponseStatus getStatus() {
    // TODO Completar
    return null;
  }

  public void setStatus(HTTPResponseStatus status) {
    // TODO Completar
  }

  public String getVersion() {
    // TODO Completar
    return null;
  }

  public void setVersion(String version) {
    // TODO Completar
  }

  public String getContent() {
    // TODO Completar
    return null;
  }

  public void setContent(String content) {
    // TODO Completar
  }

  public Map<String, String> getParameters() {
    // TODO Completar
    return null;
  }

  public String putParameter(String name, String value) {
    // TODO Completar
    return null;
  }

  public boolean containsParameter(String name) {
    // TODO Completar
    return false;
  }

  public String removeParameter(String name) {
    // TODO Completar
    return null;
  }

  public void clearParameters() {
    // TODO Completar
  }

  public List<String> listParameters() {
    // TODO Completar
    return null;
  }

  public void print(Writer writer) throws IOException {
    // TODO Completar
  }

  @Override
  public String toString() {
    try (final StringWriter writer = new StringWriter()) {
      this.print(writer);

      return writer.toString();
    } catch (IOException e) {
      throw new RuntimeException("Unexpected I/O exception", e);
    }
  }
}
