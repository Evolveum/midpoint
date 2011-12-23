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
package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.util.DOMUtil;

public class SchemaDescription {
	private String path;
	private String usualPrefix;
	private String namespace;
	private String sourceDescription;
	private Node node;
	private boolean isMidPointSchema = false;

	private SchemaDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}

	public String getPath() {
		return path;
	}

	public void setResourcePath(String path) {
		this.path = path;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getUsualPrefix() {
		return usualPrefix;
	}

	public void setUsualPrefix(String usualPrefix) {
		this.usualPrefix = usualPrefix;
	}
	
	public String getSourceDescription() {
		return sourceDescription;
	}

	public void setSourceDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isMidPointSchema() {
		return isMidPointSchema;
	}
	
	public void setMidPointSchema(boolean isMidPointSchema) {
		this.isMidPointSchema = isMidPointSchema;
	}
	
	public static SchemaDescription parseResource(String resourcePath) throws SchemaException {
		SchemaDescription desc = new SchemaDescription("system resource "+resourcePath);
		desc.path = resourcePath;
		InputStream inputStream = SchemaRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
		if (inputStream == null) {
			throw new IllegalStateException("Cannot fetch system resource for schema " + resourcePath);
		}
		try {
			desc.node = DOMUtil.parse(inputStream);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot parse system resource for schema " + resourcePath, e);
		}
		desc.fetchBasicInfoFromSchema();
		return desc;
	}

	public static SchemaDescription parseFile(File file) throws FileNotFoundException, SchemaException {
		SchemaDescription desc = new SchemaDescription("file "+file.getPath());
		desc.path = file.getPath();
		InputStream inputStream = new FileInputStream(file);
		try {
			desc.node = DOMUtil.parse(inputStream);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot parse file for schema " + file, e);
		}
		desc.fetchBasicInfoFromSchema();
		return desc;
	}

	public static SchemaDescription parseNode(Node node, String sourceDescription) throws SchemaException {
		SchemaDescription desc = new SchemaDescription(sourceDescription);
		desc.node = node;
		desc.fetchBasicInfoFromSchema();
		return desc;
	}
	
	private void fetchBasicInfoFromSchema() throws SchemaException {
		Element rootElement = getDomElement();
		if (DOMUtil.XSD_SCHEMA_ELEMENT.equals(DOMUtil.getQName(rootElement))) {
			String targetNamespace = rootElement.getAttributeNS(DOMUtil.XSD_ATTR_TARGET_NAMESPACE.getNamespaceURI(), 
					DOMUtil.XSD_ATTR_TARGET_NAMESPACE.getLocalPart());
			if (StringUtils.isEmpty(targetNamespace)) {
				// also try without the namespace
				targetNamespace = rootElement.getAttribute(DOMUtil.XSD_ATTR_TARGET_NAMESPACE.getLocalPart());
			}
			if (targetNamespace != null) {
				this.namespace = targetNamespace;
			} else {
				throw new SchemaException("Schema "+sourceDescription+" does not have targetNamespace attribute");
			}
		} else {
			throw new SchemaException("Schema "+sourceDescription+" does not start with xsd:schema element");
		}
	}

	public Source getSource() {
		DOMSource source = new DOMSource(node);
		source.setSystemId(path);
		return source;
	}
	
	public Element getDomElement() {
		if (node instanceof Element) {
			return (Element)node;
		}
		return DOMUtil.getFirstChildElement(node);
	}

}