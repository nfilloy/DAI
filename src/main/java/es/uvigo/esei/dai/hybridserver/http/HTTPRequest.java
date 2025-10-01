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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HTTPRequest {
  private final HTTPRequestMethod method;
  private final String resourceChain;
  private final String[] resourcePath;
  private final String resourceName;
  private final Map<String, String> resourceParameters;
  private final String httpVersion;
  private final Map<String, String> headerParameters;
  private final String content;
  private final int contentLength;

  public HTTPRequest(Reader reader) throws IOException, HTTPParseException {
    if (reader == null) {
      throw new HTTPParseException("Reader cannot be null");
    }

    final BufferedReader bufferedReader = reader instanceof BufferedReader
      ? (BufferedReader) reader
      : new BufferedReader(reader);

    final String requestLine = bufferedReader.readLine();
    if (requestLine == null || requestLine.isEmpty()) {
      throw new HTTPParseException("Empty request line");
    }

    final String[] parts = requestLine.split("\\s+");
    if (parts.length < 3) {
      throw new HTTPParseException("Invalid request line: " + requestLine);
    }

    try {
      this.method = HTTPRequestMethod.valueOf(parts[0]);
    } catch (IllegalArgumentException ex) {
      throw new HTTPParseException("Unsupported HTTP method: " + parts[0], ex);
    }

    this.resourceChain = parts[1];
    this.httpVersion = parts[2];
    if (!this.httpVersion.toUpperCase().startsWith("HTTP/")) {
      throw new HTTPParseException("Unsupported HTTP version: " + this.httpVersion);
    }

    final Map<String, String> headers = new LinkedHashMap<>();
    String line;
    while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
      final int separatorIndex = line.indexOf(':');
      if (separatorIndex <= 0) {
        throw new HTTPParseException("Malformed header line: " + line);
      }

      final String name = line.substring(0, separatorIndex).trim();
      final String value = line.substring(separatorIndex + 1).trim();
      headers.put(name, value);
    }

    final int length = parseContentLength(headers);
    this.contentLength = length;

    if (length > 0) {
      final char[] buffer = new char[length];
      int read = 0;
      while (read < length) {
        final int n = bufferedReader.read(buffer, read, length - read);
        if (n == -1) {
          throw new HTTPParseException("Unexpected end of stream while reading content");
        }
        read += n;
      }
      this.content = new String(buffer);
    } else {
      this.content = "";
    }

    final String[] resourceParts = this.resourceChain.split("\\?", 2);
    final String rawPath = resourceParts[0];
    final String queryString = resourceParts.length > 1 ? resourceParts[1] : "";

    final String[] segments = Arrays.stream(rawPath.split("/"))
      .filter(segment -> !segment.isEmpty())
      .toArray(String[]::new);
    this.resourcePath = segments;
    this.resourceName = segments.length > 0 ? segments[segments.length - 1] : "";

    final Map<String, String> queryParameters = new LinkedHashMap<>();
    parseParameters(queryString, queryParameters);

    if (this.method == HTTPRequestMethod.POST && length > 0 && isFormUrlEncoded(headers)) {
      parseParameters(this.content, queryParameters);
    }

    this.resourceParameters = Collections.unmodifiableMap(queryParameters);
    this.headerParameters = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
  }

  public HTTPRequestMethod getMethod() {
    return this.method;
  }

  public String getResourceChain() {
    return this.resourceChain;
  }

  public String[] getResourcePath() {
    return Arrays.copyOf(this.resourcePath, this.resourcePath.length);
  }

  public String getResourceName() {
    return this.resourceName;
  }

  public Map<String, String> getResourceParameters() {
    return this.resourceParameters;
  }

  public String getHttpVersion() {
    return this.httpVersion;
  }

  public Map<String, String> getHeaderParameters() {
    return this.headerParameters;
  }

  public String getContent() {
    return this.content;
  }

  public int getContentLength() {
    return this.contentLength;
  }

  private static int parseContentLength(Map<String, String> headers) throws HTTPParseException {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (HTTPHeaders.CONTENT_LENGTH.getHeader().equalsIgnoreCase(entry.getKey())) {
        try {
          return Integer.parseInt(entry.getValue());
        } catch (NumberFormatException ex) {
          throw new HTTPParseException("Invalid Content-Length value", ex);
        }
      }
    }

    return 0;
  }

  private static boolean isFormUrlEncoded(Map<String, String> headers) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (HTTPHeaders.CONTENT_TYPE.getHeader().equalsIgnoreCase(entry.getKey())) {
        return entry.getValue().toLowerCase().startsWith(MIME.FORM.getMime());
      }
    }

    return false;
  }

  private static void parseParameters(String raw, Map<String, String> target) throws HTTPParseException {
    if (raw == null || raw.isEmpty()) {
      return;
    }

    final String[] pairs = raw.split("&");
    for (String pair : pairs) {
      if (pair.isEmpty()) {
        continue;
      }

      final String[] nv = pair.split("=", 2);
      final String name = urlDecode(nv[0]);
      final String value = nv.length > 1 ? urlDecode(nv[1]) : "";
      target.put(name, value);
    }
  }

  private static String urlDecode(String value) throws HTTPParseException {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      throw new HTTPParseException("Unable to decode parameter", ex);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder().append(this.getMethod().name()).append(' ')
      .append(this.getResourceChain()).append(' ').append(this.getHttpVersion()).append("\r\n");

    for (Map.Entry<String, String> param : this.getHeaderParameters().entrySet()) {
      sb.append(param.getKey()).append(": ").append(param.getValue()).append("\r\n");
    }

    if (this.getContentLength() > 0) {
      sb.append("\r\n").append(this.getContent());
    }

    return sb.toString();
  }
}
