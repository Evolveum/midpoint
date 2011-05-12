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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.component.form;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

public enum AttributeType {

	INT(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "int")),

	INTEGER(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "integer")),

	INTEGER_NEGATIVE(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "negativeInteger")),

	INTEGER_NON_NEGATIVE(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "nonNegativeInteger")),

	INTEGER_NON_POSITIVE(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "nonPositiveInteger")),

	INTEGER_POSITIVE(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "positiveInteger")),

	BOOLEAN(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "boolean")),

	LONG(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "long")),

	SHORT(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "short")),

	ANY_URI(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "anyURI")),

	STRING(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string")),

	DATE(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "date")),

	ELEMENT(new QName("http://midpoint.evolveum.com", "element")),

	PASSWORD(new QName("http://midpoint.evolveum.com", "password"));

	private QName qname;

	private AttributeType(QName qname) {
		this.qname = qname;
	}

	public QName getQname() {
		return qname;
	}

	public static AttributeType getType(QName qname) {
		AttributeType[] values = values();
		for (AttributeType type : values) {
			if (type.qname.equals(qname)) {
				return type;
			}
		}

		return STRING;
	}
}
