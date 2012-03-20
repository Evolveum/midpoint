/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.util;

import javax.xml.XMLConstants;

/**
 * @author semancik
 *
 */
public class UglyHacks {

	public static String forceXsiNsDeclaration(String originalXml) {
		int iOpeningBracket = -1;
		while (true) {
			iOpeningBracket = originalXml.indexOf('<', iOpeningBracket+1);
			if (iOpeningBracket < 0) {
				// No element ?
				return originalXml;
			}
			if (Character.isLetter(originalXml.charAt(iOpeningBracket+1))) {
				break;
			}
			// Processing instruction, skip it
		}
		int iClosingBracket = originalXml.indexOf('>', iOpeningBracket);
		if (iClosingBracket < 0) {
			// Element not closed?
			return originalXml;
		}
		String firstElement = originalXml.substring(iOpeningBracket, iClosingBracket);
		// Not perfect, but should be good enough. All this is a hack anyway
		if (firstElement.indexOf("xmlns:xsi") >= 0) {
			// Already has xsi declaration
			return originalXml;
		}
		int iEndOfElementName = iOpeningBracket;
		while (iEndOfElementName < iClosingBracket) {
			char ch = originalXml.charAt(iEndOfElementName);
			if (ch == ' ' || ch == '>') {
				break;
			}
			iEndOfElementName++;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(originalXml.substring(0, iEndOfElementName));
		sb.append(" xmlns:xsi='");
		sb.append(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
		sb.append("'");
		sb.append(originalXml.substring(iEndOfElementName));
		return sb.toString();
	}
	
}
