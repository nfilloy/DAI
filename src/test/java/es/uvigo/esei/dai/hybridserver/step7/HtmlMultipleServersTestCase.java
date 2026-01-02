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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import es.uvigo.esei.dai.hybridserver.http.MIME;

public class HtmlMultipleServersTestCase
extends MultipleServersTestCase {

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testGetLocales(int serverIndex) throws IOException {
		for (String uuid : getLocalHtmlUUIDs(serverIndex)) {
			final String url = String.format("%s/html?uuid=%s",
				serversHTTPURL[serverIndex], uuid);
			
			assertThat(getContentWithType(url, MIME.TEXT_HTML.getMime()), containsString(uuid));
		}
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testGetRemotes(int serverIndex) throws IOException {
		for (String uuid : getRemoteHtmlUUIDs(serverIndex)) {
			final String url = String.format("%s/html?uuid=%s",
				serversHTTPURL[serverIndex], uuid);
			
			assertThat(getContentWithType(url, MIME.TEXT_HTML.getMime()), containsString(uuid));
		}
	}

	@ParameterizedTest
	@MethodSource("listServerIndexes")
	public final void testInvalid(int serverIndex) throws IOException {
		for (String uuid : generateInvalidUUIDs()) {
			final String url = String.format("%s/html?uuid=%s",
				serversHTTPURL[serverIndex], uuid);

			assertThat(getStatus(url), is(equalTo(404)));
		}
	}
}
