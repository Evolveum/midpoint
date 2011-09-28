/*
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
 *
 * Portions Copyrighted 2011 Igor Farinic
 */

package com.evolveum.midpoint.schema.namespace;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.apache.cxf.common.util.StringUtils;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 * 
 * @see MID-349
 * 
 * @author Igor Farinic
 * 
 */
public class MidPointNamespacePrefixMapper {

	private static final Map<String, String> namespacePrefixMap = new HashMap<String, String>();

	/**
	 * 
	 * @param namespace
	 * @return preferred prefix for the namespace, if no prefix is assigned yet,
	 *         then it will assign a prefix and return it.
	 */
	public static synchronized String getPreferredPrefix(String namespace) {
		return getPreferredPrefix(namespace, null);
	}

	/**
	 * @param namespace
	 * @param hintPrefix
	 * @return preferred prefix for the namespace, if no prefix is assigned yet,
	 *         then it assign hint prefix (if it is not assigned yet) or assign
	 *         a new prefix and return it (if hint prefix is already assigned to
	 *         other namespace).
	 */
	public static synchronized String getPreferredPrefix(String namespace, String hintPrefix) {
		String prefix = namespacePrefixMap.get(namespace);

		if (StringUtils.isEmpty(prefix)) {
			if (StringUtils.isEmpty(hintPrefix)) {
				// FIXME: improve new prefix assignment
				prefix = "gen" + (new Random()).nextInt(999);
			} else {
				if (namespacePrefixMap.containsValue(hintPrefix)) {
					// FIXME: improve new prefix assignment
					prefix = "gen" + (new Random()).nextInt(999);
				} else {
					prefix = hintPrefix;
				}
			}
			namespacePrefixMap.put(namespace, prefix);
		}

		return prefix;

	}

	public static void initialize() {
		namespacePrefixMap.clear();
		namespacePrefixMap.put(SchemaConstants.NS_C, SchemaConstants.NS_C_PREFIX);
		namespacePrefixMap.put(SchemaConstants.NS_ANNOTATION, "a");
		namespacePrefixMap.put(SchemaConstants.NS_ICF_SCHEMA, "icfs");
		namespacePrefixMap.put(SchemaConstants.NS_ICF_CONFIGURATION, "icfc");
		namespacePrefixMap.put(SchemaConstants.NS_CAPABILITIES, "cap");
		namespacePrefixMap.put(SchemaConstants.NS_RESOURCE, "r");
		namespacePrefixMap.put(SchemaConstants.NS_FILTER, "f");
		namespacePrefixMap.put(SchemaConstants.NS_PROVISIONING_LIVE_SYNC, "ls");
		namespacePrefixMap.put(SchemaConstants.NS_SITUATION, "sit");
		namespacePrefixMap.put(
				"http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd",
				"ids");
		namespacePrefixMap.put(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
		namespacePrefixMap.put(W3C_XML_SCHEMA_NS_URI, "xsd");
		namespacePrefixMap.put("http://www.w3.org/2001/04/xmlenc#", "enc");
		namespacePrefixMap.put("http://www.w3.org/2000/09/xmldsig#", "ds");
	}
	
	static {
		initialize();
	}
}