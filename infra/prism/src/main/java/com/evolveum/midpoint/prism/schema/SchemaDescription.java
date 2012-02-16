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
package com.evolveum.midpoint.prism.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;

public class SchemaDescription implements Dumpable {
	private String path;
	private String usualPrefix;
	private String namespace;
	private String sourceDescription;
	private InputStreamable streamable; 
	private Node node;
	private boolean isPrismSchema = false;
	private PrismSchema schema;
	private Package compileTimeClassesPackage;

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

	public boolean isPrismSchema() {
		return isPrismSchema;
	}
	
	public void setPrismSchema(boolean isMidPointSchema) {
		this.isPrismSchema = isMidPointSchema;
	}
	
	public PrismSchema getSchema() {
		return schema;
	}

	public void setSchema(PrismSchema schema) {
		this.schema = schema;
	}

	public Package getCompileTimeClassesPackage() {
		return compileTimeClassesPackage;
	}

	public void setCompileTimeClassesPackage(Package compileTimeClassesPackage) {
		this.compileTimeClassesPackage = compileTimeClassesPackage;
	}

	public static SchemaDescription parseResource(final String resourcePath) throws SchemaException {
		SchemaDescription desc = new SchemaDescription("system resource "+resourcePath);
		desc.path = resourcePath;
		desc.streamable = new InputStreamable() {
			@Override
			public InputStream openInputStream() {
				InputStream inputStream = SchemaRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
				if (inputStream == null) {
					throw new IllegalStateException("Cannot fetch system resource for schema " + resourcePath);
				}
				return inputStream;
			}
		};
		desc.parseFromInputStream();
		return desc;
	}
		
	public static SchemaDescription parseFile(final File file) throws FileNotFoundException, SchemaException {
		SchemaDescription desc = new SchemaDescription("file "+file.getPath());
		desc.path = file.getPath();
		desc.streamable = new InputStreamable() {
			@Override
			public InputStream openInputStream() {
				InputStream inputStream;
				try {
					inputStream = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					throw new IllegalStateException("Cannot fetch file for schema " + file,e);
				}
				return inputStream;
			}
		};
		desc.parseFromInputStream();
		return desc;
	}
	
	private void parseFromInputStream() throws SchemaException {
		InputStream inputStream = streamable.openInputStream();
		try {
			node = DOMUtil.parse(inputStream);
		} catch (IOException e) {
			throw new SchemaException("Cannot parse schema from " + sourceDescription, e);
		}
		fetchBasicInfoFromSchema();
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
	
	public boolean canInputStream() {
		return (streamable != null);
	}
	
	public InputStream openInputStream() {
		if (!canInputStream()) {
			throw new IllegalStateException("Schema "+sourceDescription+" cannot provide input stream");
		}
		return streamable.openInputStream();
	}

	public Source getSource() {
		Source source = null;
		if (canInputStream()) {
			InputStream inputStream = openInputStream();
			// Return stream source as a first option. It is less effcient,
			// but it provides information about line numbers
			source = new StreamSource(inputStream);
		} else {
			source = new DOMSource(node);
		}
		source.setSystemId(path);
		return source;
	}
	
	public Element getDomElement() {
		if (node instanceof Element) {
			return (Element)node;
		}
		return DOMUtil.getFirstChildElement(node);
	}
	
	private interface InputStreamable {
		InputStream openInputStream();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.Dumpable#dump()
	 */
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append(path);
		if (schema != null) {
			sb.append(" ");
			sb.append(schema.toString());
		}
		return sb.toString();
	}

}