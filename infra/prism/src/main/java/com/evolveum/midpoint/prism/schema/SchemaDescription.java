/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.cxf.wsdl.WSDLConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class SchemaDescription implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaDescription.class);

	private String path;
	private String usualPrefix;
	private String namespace;
	private String sourceDescription;
	private InputStreamable streamable; 
	private Node node;
	private boolean isPrismSchema = false;
	private boolean isDefault = false;
    private boolean isDeclaredByDefault = false;
	private PrismSchema schema;
	private Package compileTimeClassesPackage;
	private Map<QName, Class<?>> xsdTypeTocompileTimeClassMap;

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
	
	public boolean isDefault() {
		return isDefault;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

    public boolean isDeclaredByDefault() {
        return isDeclaredByDefault;
    }

    public void setDeclaredByDefault(boolean isDeclaredByDefault) {
        this.isDeclaredByDefault = isDeclaredByDefault;
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
	
	public Map<QName, Class<?>> getXsdTypeTocompileTimeClassMap() {
		return xsdTypeTocompileTimeClassMap;
	}

	public void setXsdTypeTocompileTimeClassMap(Map<QName, Class<?>> xsdTypeTocompileTimeClassMap) {
		this.xsdTypeTocompileTimeClassMap = xsdTypeTocompileTimeClassMap;
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

    public static List<SchemaDescription> parseWsdlResource(final String resourcePath) throws SchemaException {
        List<SchemaDescription> schemaDescriptions = new ArrayList<>();

        InputStream inputStream = SchemaRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IllegalStateException("Cannot fetch system resource for schema " + resourcePath);
        }
        Node node;
        try {
            node = DOMUtil.parse(inputStream);
        } catch (IOException e) {
            throw new SchemaException("Cannot parse schema from system resource " + resourcePath, e);
        }
        Element rootElement = node instanceof Element ? (Element)node : DOMUtil.getFirstChildElement(node);
        QName rootElementQName = DOMUtil.getQName(rootElement);
        if (WSDLConstants.QNAME_DEFINITIONS.equals(rootElementQName)) {
            Element types = DOMUtil.getChildElement(rootElement, WSDLConstants.QNAME_TYPES);
            if (types == null) {
                LOGGER.warn("No <types> section in WSDL document in system resource " + resourcePath);
                return schemaDescriptions;
            }
            List<Element> schemaElements = DOMUtil.getChildElements(types, DOMUtil.XSD_SCHEMA_ELEMENT);
            if (schemaElements.isEmpty()) {
                LOGGER.warn("No schemas in <types> section in WSDL document in system resource " + resourcePath);
                return schemaDescriptions;
            }
            int number = 1;
            for (Element schemaElement : schemaElements) {
                SchemaDescription desc = new SchemaDescription("schema #" + (number++) + " in system resource " + resourcePath);
                desc.node = schemaElement;
                desc.fetchBasicInfoFromSchema();
                schemaDescriptions.add(desc);
                LOGGER.trace("Schema registered from {}", desc.getSourceDescription());
            }
            return schemaDescriptions;
        } else {
            throw new SchemaException("WSDL system resource "+resourcePath+" does not start with wsdl:definitions element");
        }
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
			String targetNamespace = DOMUtil.getAttribute(rootElement,DOMUtil.XSD_ATTR_TARGET_NAMESPACE);
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
	
	@FunctionalInterface
    private interface InputStreamable {
		InputStream openInputStream();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(path);
		if (schema != null) {
			sb.append(" ");
			sb.append(schema.toString());
		}
		return sb.toString();
	}

}
