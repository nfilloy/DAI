package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;

interface PageRepository {
  HTTPResponse handle(HTTPRequest request, String welcomePage);
}
