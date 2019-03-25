/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.prism;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static com.evolveum.midpoint.util.DOMUtil.NS_W3C_XML_SCHEMA_PREFIX;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public enum PrimitiveType {
	
	STRING("string"),
	DECIMAL("decimal"),
	INTEGER("integer"),
	INT("int"),
	LONG("long"),
	SHORT("short"),
	FLOAT("float"),
	DOUBLE("double"),
	BOOLEAN("boolean"),
	BASE64BINARY("base64binary"),
	DATETIME("dateTime"),
	DURATION("duration"),
	BYTE("byte"),
	QNAME("qname"),
	ANYURI("anyURI");
	
	private final String localName;
	private final QName qname;
	
	private PrimitiveType(String localName) {
		this.localName = localName;
		this.qname = new QName(W3C_XML_SCHEMA_NS_URI, localName, NS_W3C_XML_SCHEMA_PREFIX);
	}

	public QName getQname() {
		return qname;
	}

	public static final QName XSD_STRING = STRING.getQname();
	public static final QName XSD_DECIMAL = DECIMAL.getQname();
	public static final QName XSD_INTEGER = INTEGER.getQname();
	public static final QName XSD_INT = INT.getQname();
	public static final QName XSD_LONG = LONG.getQname();
	public static final QName XSD_SHORT = SHORT.getQname();
	public static final QName XSD_FLOAT = FLOAT.getQname();
	public static final QName XSD_DOUBLE = DOUBLE.getQname();
	public static final QName XSD_BOOLEAN = BOOLEAN.getQname();
	public static final QName XSD_BASE64BINARY = BASE64BINARY.getQname();
	public static final QName XSD_DATETIME = DATETIME.getQname();
	public static final QName XSD_DURATION = DURATION.getQname();
	public static final QName XSD_BYTE = BYTE.getQname();
	public static final QName XSD_QNAME = QNAME.getQname();
	public static final QName XSD_ANYURI = ANYURI.getQname();

}
