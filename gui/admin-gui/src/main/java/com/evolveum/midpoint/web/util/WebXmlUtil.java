/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of brutal and ugly hacks, to be removed when prism implementation will be reviewed in 3.5.
 *
 * @author mederly
 */
public class WebXmlUtil {

	public static List<String> declarations = new ArrayList<>();
	static {
		addDeclaration(null, SchemaConstantsGenerated.NS_COMMON);
		addDeclaration("c", SchemaConstantsGenerated.NS_COMMON);
		addDeclaration("t", SchemaConstantsGenerated.NS_TYPES);
		addDeclaration("q", SchemaConstantsGenerated.NS_QUERY);
		addDeclaration("ri", MidPointConstants.NS_RI);
		addDeclaration("icfs", SchemaConstantsGenerated.NS_ICF_SCHEMA);
		addDeclaration("icfc", SchemaConstantsGenerated.NS_ICF_CONFIGURATION);
		addDeclaration("mr", PrismConstants.NS_MATCHING_RULE);
		addDeclaration("mext", SchemaConstants.NS_MODEL_EXTENSION);
		addDeclaration("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
	}

	private static void addDeclaration(String prefix, String uri) {
		if (prefix == null) {
			declarations.add("xmlns=\"" + uri + "\"");
		} else {
			declarations.add("xmlns:" + prefix + "=\"" + uri + "\"");
		}
	}

	public static String stripNamespaceDeclarations(String xml) {
		if (xml == null) {
			return null;
		}
		for (String declaration : declarations) {
			for (;;) {
				int i = xml.indexOf(declaration);
				if (i < 0) {
					break;
				}
				int j = i + declaration.length();
				while (j < xml.length() && Character.isWhitespace(xml.charAt(j))) {
					j++;
				}
				String before = xml.substring(0, i);
				String after = j < xml.length() ? xml.substring(j, xml.length()) : "";
				xml = before + after;
			}
		}
		int i = xml.indexOf('>');
		if (i > 0) {
			if (Character.isWhitespace(xml.charAt(i-1))) {
				xml = xml.substring(0, i-1) + xml.substring(i, xml.length());
			}
		}
		return xml;
	}

	// body is not blank
	public static String wrapInElement(String name, String body, boolean alsoDefaultNamespace) {
		StringBuilder sb = new StringBuilder();
		sb.append("<").append(name);
		for (String declaration : declarations) {
			if (alsoDefaultNamespace || !declaration.startsWith("xmlns=")) {
				sb.append("\n    ").append(declaration);
			}
		}
		sb.append(">\n");
		sb.append(body);
		sb.append("\n</").append(name).append(">\n");
		return sb.toString();
	}
}
