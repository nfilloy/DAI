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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import es.uvigo.esei.dai.hybridserver.step7.HtmlMultipleServersTestCase;
import es.uvigo.esei.dai.hybridserver.step7.TransformedMultipleServersTestCase;
import es.uvigo.esei.dai.hybridserver.step7.WelcomeAndListsMultipleServersTestCase;
import es.uvigo.esei.dai.hybridserver.step7.XmlMultipleServersTestCase;
import es.uvigo.esei.dai.hybridserver.step7.XsdMultipleServersTestCase;
import es.uvigo.esei.dai.hybridserver.step7.XsltMultipleServersTestCase;

@Suite
@SelectClasses({
	WelcomeAndListsMultipleServersTestCase.class,
	HtmlMultipleServersTestCase.class,
	XmlMultipleServersTestCase.class,
	XsdMultipleServersTestCase.class,
	XsltMultipleServersTestCase.class,
	TransformedMultipleServersTestCase.class
})
public class Step7TestSuite {

}
