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
package es.uvigo.esei.dai.hybridserver.step7;

import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.getContentWithType;
import static es.uvigo.esei.dai.hybridserver.utils.TestUtils.getStatus;
import static es.uvigo.esei.dai.hybridserver.utils.matchers.EqualsToIgnoringSpacesAndCaseMatcher.equalsToIgnoringSpacesAndCase;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import es.uvigo.esei.dai.hybridserver.http.MIME;

public class TransformedMultipleServersTestCase
extends MultipleServersTestCase {
	private static final String[] CT_XMLS = new String[] {
		"ddcab7d0-636c-1111-8db3-685b35c84fb4",
		"ddcab7d0-636c-2222-8db3-685b35c84fb4",
		"ea118888-6908-3333-9620-685b35c84fb4",
		"ea118888-6908-4444-9620-685b35c84fb4"
	};
	private static final String[] CT_XSLTS = new String[] {
		"f260dfee-636c-3333-1111-685b35c84fb4",
		"f260dfee-636c-4444-2222-685b35c84fb4",
		"1fd26c94-6909-2222-3333-685b35c84fb4",
		"1fd26c94-6909-1111-4444-685b35c84fb4"
	};
	private static final String[] CT_INVALID_XSLTS = new String[] {
		"1fd26c94-6909-2222-9a75-685b35c84fb4",
		"1fd26c94-6909-1111-9a75-685b35c84fb4",
		"f260dfee-636c-4444-bbdd-685b35c84fb4",
		"f260dfee-636c-3333-bbdd-685b35c84fb4"
	};
	private static final String[] CT_RESULTS = new String[] {
		"files/sample1_3_1.html",
		"files/sample1_4_2.html",
		"files/sample2_2_3.html",
		"files/sample2_1_4.html"
	};

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testGetLocales(int serverIndex) throws IOException {
		final String[] xmls = getLocalXmlUUIDs(serverIndex);
		final String[] xslts = getLocalXsltUUIDs(serverIndex);
		final String[] contents = getLocalTransformedFiles(serverIndex);
		
		testMultipleGets(xmls, xslts, contents, serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testGetRemotes(int serverIndex) throws IOException {
		final String[] xmls = getRemoteXmlUUIDs(serverIndex);
		final String[] xslts = getRemoteXsltUUIDs(serverIndex);
		final String[] contents = getRemoteTransformedFiles(serverIndex);
		
		testMultipleGets(xmls, xslts, contents, serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testCrossTransformations(int serverIndex) throws IOException {
		final String[] xmls = CT_XMLS;
		final String[] xslts = CT_XSLTS;
		final String[] contents = getFileContents(CT_RESULTS);
		
		testMultipleGets(xmls, xslts, contents, serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testBadXmlTransformations(int serverIndex) throws IOException {
		final String[] xslts = getAllXsltUUIDs();
		final String[] xmls = generateInvalidUUIDs(xslts.length);
		
		testMultipleErrorGets(xmls, xslts, 404, serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testBadXsltTransformations(int serverIndex) throws IOException {
		final String[] xmls = getAllXmlUUIDs();
		final String[] xslts = generateInvalidUUIDs(xmls.length);
		
		testMultipleErrorGets(xmls, xslts, 404, serverIndex);
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testInvalidXsltTransformations(int serverIndex) throws IOException {
		final String[] xmls = CT_XMLS;
		final String[] xslts = CT_INVALID_XSLTS;
		
		testMultipleErrorGets(xmls, xslts, 400, serverIndex);
	}

	protected void testMultipleGets(
		final String[] xmls, final String[] xslts, final String[] expectedContents, final int serverIndex
	) throws IOException {
		for (int i = 0; i < xmls.length; i++) {
			final String xmlUuid = xmls[i];
			final String xsltUuid = xslts[i];
			final String expectedContent = expectedContents[i];
			
			final String url = getResourceURL(xmlUuid, xsltUuid, serverIndex);
			final String content = getContentWithType(url, MIME.TEXT_HTML.getMime());
			
			assertThat(expectedContent, is(equalsToIgnoringSpacesAndCase(content)));
		}
	}

	protected void testMultipleErrorGets(
		final String[] xmls, final String[] xslts, final int expectedError, final int serverIndex
	) throws IOException {
		for (int i = 0; i < xmls.length; i++) {
			final String xmlUuid = xmls[i];
			final String xsltUuid = xslts[i];
			
			final String url = getResourceURL(xmlUuid, xsltUuid, serverIndex);

			assertThat(getStatus(url), is(equalTo(expectedError)));
		}
	}
	
	protected String getResourceURL(String xmlUuid, String xsltUuid, int serverIndex) {
		return String.format("%s/xml?uuid=%s&xslt=%s",
			serversHTTPURL[serverIndex], xmlUuid, xsltUuid
		);
	}
}
