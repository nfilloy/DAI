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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HTTPResponse {
  private HTTPResponseStatus status;
  private String version;
  private String content;
  private final Map<String, String> parameters;

  public HTTPResponse() {
    this.status = HTTPResponseStatus.S200;
    this.version = HTTPHeaders.HTTP_1_1.getHeader();
    this.content = null;
    this.parameters = new LinkedHashMap<>();
  }

  public HTTPResponseStatus getStatus() {
    return this.status;
  }

  public void setStatus(HTTPResponseStatus status) {
    this.status = status;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getContent() {
    return this.content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Map<String, String> getParameters() {
    return Collections.unmodifiableMap(this.parameters);
  }

  public String putParameter(String name, String value) {
    return this.parameters.put(name, value);
  }

  public boolean containsParameter(String name) {
    return this.parameters.containsKey(name);
  }

  public String removeParameter(String name) {
    return this.parameters.remove(name);
  }

  public void clearParameters() {
    this.parameters.clear();
  }

  public List<String> listParameters() {
    return Collections.unmodifiableList(new ArrayList<>(this.parameters.keySet()));
  }

  public void print(Writer writer) throws IOException {
    final HTTPResponseStatus status = this.status != null ? this.status : HTTPResponseStatus.S200;
    final String version = this.version != null ? this.version : HTTPHeaders.HTTP_1_1.getHeader();
    final String body = this.content;

    final Map<String, String> headers = new LinkedHashMap<>(this.parameters);

    if (body != null) {
      final int bodyLength = body.getBytes(StandardCharsets.UTF_8).length;
      headers.putIfAbsent(HTTPHeaders.CONTENT_LENGTH.getHeader(), Integer.toString(bodyLength));
    }

    writer.write(version);
    writer.write(' ');
    writer.write(Integer.toString(status.getCode()));
    writer.write(' ');
    writer.write(status.getStatus());
    writer.write("\r\n");

    for (Map.Entry<String, String> param : headers.entrySet()) {
      writer.write(param.getKey());
      writer.write(": ");
      writer.write(param.getValue());
      writer.write("\r\n");
    }

    writer.write("\r\n");
    if (body != null) {
      writer.write(body);
    }
    writer.flush();
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
