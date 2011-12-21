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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.namespace.MidPointNamespacePrefixMapper;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.xml.txw2.Document;

/**
 * Registry and resolver of schema files and resources.
 * 
 * 
 * @author Radovan Semancik
 *
 */
public class SchemaRegistry implements LSResourceResolver {
	
	private javax.xml.validation.SchemaFactory schemaFactory;
	private javax.xml.validation.Schema javaxSchema;
	private EntityResolver builtinSchemaResolver;	
	private List<SchemaDescription> schemaDescriptions;
	private Map<String,Schema> parsedSchemas;
	private boolean initialized = false;
	
	private static final Trace LOGGER = TraceManager.getTrace(SchemaRegistry.class);
	
	public SchemaRegistry() {
		super();
		this.schemaDescriptions = new ArrayList<SchemaDescription>();
		this.parsedSchemas = new HashMap<String, Schema>();
		registerBuiltinSchemas();
	}
	
	private void registerBuiltinSchemas() {
		String prefix;
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_C);
		registerMidPointSchemaResource("xml/ns/public/common/common-1.xsd",prefix,SchemaConstants.NS_C);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_ANNOTATION);
		registerMidPointSchemaResource("xml/ns/public/common/annotation-1.xsd",prefix,SchemaConstants.NS_ANNOTATION);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_RESOURCE);
		registerMidPointSchemaResource("xml/ns/public/resource/resource-schema-1.xsd",prefix,SchemaConstants.NS_RESOURCE);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_CAPABILITIES);
		registerMidPointSchemaResource("xml/ns/public/resource/capabilities-1.xsd",prefix,SchemaConstants.NS_CAPABILITIES);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_ICF_CONFIGURATION);
		registerMidPointSchemaResource("xml/ns/public/connector/icf-1/connector-schema-1.xsd",prefix,SchemaConstants.NS_ICF_CONFIGURATION);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(SchemaConstants.NS_ICF_SCHEMA);
		registerMidPointSchemaResource("xml/ns/public/connector/icf-1/resource-schema-1.xsd",prefix,SchemaConstants.NS_ICF_SCHEMA);
		prefix = MidPointNamespacePrefixMapper.getPreferredPrefix(W3C_XML_SCHEMA_NS_URI);
		registerSchemaResource("xml/ns/standard/XMLSchema.xsd",prefix,W3C_XML_SCHEMA_NS_URI);
	}
	
	/**
	 * Must be called before call to initialize()
	 */
	public void registerSchemaResource(String resourcePath, String usualPrefix) {
		schemaDescriptions.add(new SchemaDescription(resourcePath, usualPrefix, "system resource "+resourcePath));
	}

	/**
	 * Must be called before call to initialize()
	 */
	public void registerSchemaResource(String resourcePath, String usualPrefix, String namespace) {
		schemaDescriptions.add(new SchemaDescription(resourcePath, usualPrefix, namespace, "system resource "+resourcePath));
	}
	
	/**
	 * Must be called before call to initialize()
	 */
	public void registerMidPointSchemaResource(String resourcePath, String usualPrefix, String namespace) {
		schemaDescriptions.add(new SchemaDescription(resourcePath, usualPrefix, namespace, "system resource "+resourcePath, true));
	}

	/**
	 * Must be called before call to initialize()
	 * @param node
	 */
	public void registerSchema(Node node, String sourceDescription) {
		schemaDescriptions.add(new SchemaDescription(node, sourceDescription));
	}

	/**
	 * Must be called before call to initialize()
	 * @param node
	 */
	public void registerSchema(Node node, String sourceDescription, String usualPrefix) {
		schemaDescriptions.add(new SchemaDescription(node, usualPrefix, sourceDescription));
	}
	
	public void registerSchemaFile(File file) {
		schemaDescriptions.add(new SchemaDescription(file, "file "+file.getPath()));
	}
	
	public void registerSchemasFromDirectory(File directory) {
		for (File file: directory.listFiles()) {
			if (file.isDirectory()) {
				registerSchemasFromDirectory(file);
			}
			if (file.isFile()) {
				registerSchemaFile(file);
			}
		}
	}
	
	public void initialize() throws SAXException, IOException, SchemaException {
		initResolver();
		parseJavaxSchema();
		parseMidPointSchema();
		initialized = true;
	}
	
	private void parseJavaxSchema() throws SAXException, IOException {
		schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source[] sources = new Source[schemaDescriptions.size()];
		int i = 0;
		for (SchemaDescription schemaDescription : schemaDescriptions) {
			Source source = schemaDescription.getSource();
			sources[i] = source;
			i++;
		}
		schemaFactory.setResourceResolver(this);
		javaxSchema = schemaFactory.newSchema(sources);
	}

	private void parseMidPointSchema() throws SchemaException {
		for (SchemaDescription schemaDescription : schemaDescriptions) {
			if (!schemaDescription.isMidPointSchema()) {
				continue;
			}
			Element domElement = schemaDescription.getDomElement();
			Schema schema = Schema.parse(domElement);
			String namespace = schemaDescription.getNamespace();
			if (namespace == null) {
				namespace = schema.getNamespace();
			}
			parsedSchemas.put(namespace, schema);
		}
	}
	
	private void initResolver() throws IOException {
		CatalogManager catalogManager = new CatalogManager();
		catalogManager.setUseStaticCatalog(true);
		catalogManager.setIgnoreMissingProperties(true);
		catalogManager.setVerbosity(1);
		catalogManager.setPreferPublic(true);
		CatalogResolver catalogResolver = new CatalogResolver(catalogManager);
		Catalog catalog = catalogResolver.getCatalog();

		Enumeration<URL> catalogs = Thread.currentThread().getContextClassLoader()
				.getResources("META-INF/catalog.xml");
		while (catalogs.hasMoreElements()) {
			URL catalogURL = catalogs.nextElement();
			catalog.parseCatalog(catalogURL);
		}
		
		builtinSchemaResolver=catalogResolver;
	}

	public javax.xml.validation.Schema getJavaxSchema() {
		return javaxSchema;
	}
	
	public Schema getSchema(String namespace) {
		return parsedSchemas.get(namespace);
	}
	
	// Convenience and safety
	public Schema getCommonSchema() {
		if (!initialized) {
			throw new IllegalStateException("Attempt to get common schema from uninitialized Schema Registry");
		}
		return parsedSchemas.get(SchemaConstants.NS_C);
	}
	
	public QName setQNamePrefix(QName qname) {
		String namespace = qname.getNamespaceURI();
		SchemaDescription desc = lookupSchemaDescription(namespace);
		if (desc==null || desc.getUsualPrefix() == null) {
			return qname;
		}
		return new QName(qname.getNamespaceURI(),qname.getLocalPart(),desc.getUsualPrefix());
	}
	
	private SchemaDescription lookupSchemaDescription(String namespace) {
		for (SchemaDescription desc : schemaDescriptions) {
			if (namespace.equals(desc.getNamespace())) {
				return desc;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.w3c.dom.ls.LSResourceResolver#resolveResource(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
			String baseURI) {
		InputSource inputSource = null;
		try {
			if (namespaceURI!=null) {
				// The systemId will be populated by schema location, not namespace URI.
				// As we use catalog resolver as the default one, we need to pass it the namespaceURI in place of systemId
				inputSource = builtinSchemaResolver.resolveEntity(publicId, namespaceURI);
			} else {
				inputSource = builtinSchemaResolver.resolveEntity(publicId, systemId);
			}
		} catch (SAXException e) {
			LOGGER.error("XML parser error resolving reference of type {}, namespaceURI: {}, publicID: {}, systemID: {}, baseURI: {}: {}",new Object[]{type, namespaceURI, publicId, systemId, baseURI, e.getMessage(), e});
			// TODO: better error handling
			return null;
		} catch (IOException e) {
			LOGGER.error("IO error resolving reference of type {}, namespaceURI: {}, publicID: {}, systemID: {}, baseURI: {}: {}",new Object[]{type, namespaceURI, publicId, systemId, baseURI, e.getMessage(), e});
			// TODO: better error handling
			return null;
		}		
		if (inputSource==null) {
			LOGGER.error("Unable to resolve reference of type {}, namespaceURI: {}, publicID: {}, systemID: {}, baseURI: {}",new Object[]{type, namespaceURI, publicId, systemId, baseURI});
			return null;
		}
		LOGGER.trace("Resolved reference of type {}, namespaceURI: {}, publicID: {}, systemID: {}, baseURI: {} -> publicID: {}, systemID: {}",new Object[]{type, namespaceURI, publicId, systemId, baseURI, inputSource.getPublicId(), inputSource.getSystemId()});
		return new Input(publicId, systemId, inputSource.getByteStream());
	}
	
	public class SchemaDescription {
		private String resourcePath;
		private String usualPrefix;
		private String namespace;
		private String sourceDescription;
		private Node node;
		private File file;
		private boolean isMidPointSchema = false;

		public SchemaDescription(String resourcePath, String usualPrefix, String namespace, String sourceDescription) {
			super();
			this.resourcePath = resourcePath;
			this.namespace = namespace;
			this.usualPrefix = usualPrefix;
		}

		public SchemaDescription(String resourcePath, String usualPrefix, String namespace, String sourceDescription, boolean isMidPointSchema) {
			super();
			this.resourcePath = resourcePath;
			this.namespace = namespace;
			this.usualPrefix = usualPrefix;
			this.sourceDescription = sourceDescription;
			this.isMidPointSchema = isMidPointSchema;
		}
		
		public SchemaDescription(String resourcePath, String usualPrefix, String sourceDescription) {
			super();
			this.resourcePath = resourcePath;
			this.usualPrefix = usualPrefix;
			this.sourceDescription = sourceDescription;
		}

		public SchemaDescription(Node node, String usualPrefix, String sourceDescription) {
			super();
			this.node = node;
			this.usualPrefix = usualPrefix;
			this.sourceDescription = sourceDescription;
		}
		
		public SchemaDescription(Node node, String sourceDescription) {
			super();
			this.node = node;
			this.sourceDescription = sourceDescription;
		}
		
		public SchemaDescription(File file, String sourceDescription) {
			super();
			this.file = file;
			this.sourceDescription = sourceDescription;
		}

		public String getResourcePath() {
			return resourcePath;
		}

		public void setResourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
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
		
		public boolean isMidPointSchema() {
			return isMidPointSchema;
		}
		
		public boolean canInputStream() {
			return (resourcePath != null || file != null);
		}
		
		public InputStream getInputStream() {
			if (resourcePath != null) {			
				InputStream inputStream = SchemaRegistry.class.getClassLoader().getResourceAsStream(resourcePath);
				if (inputStream == null) {
					throw new IllegalStateException("Cannot fetch system resource for schema " + resourcePath);
				}
				return inputStream;
			}
			if (file != null) {
				InputStream inputStream;
				try {
					inputStream = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					throw new IllegalStateException("Cannot open schema file "+file,e);
				}
				return inputStream;
			}
			throw new IllegalStateException("Cannot produce input stream");
		}

		public Source getSource() {
			if (node != null) {
				return new DOMSource(node);
			}
			if (canInputStream()) {			
				InputStream inputStream = getInputStream();
				return new StreamSource(inputStream);
			}
			throw new IllegalStateException("Cannot determine source for schema " + namespace);
		}
		
		public Element getDomElement() {
			if (node == null) {
				if (canInputStream()) {			
					InputStream inputStream = getInputStream();
					try {
						node = DOMUtil.parse(inputStream);
					} catch (IOException e) {
						throw new IllegalStateException("Cannot fetch and parse " + sourceDescription 
								+ " for namespace " + namespace + ": "+e.getMessage(), e);
					}
				} else {
					throw new IllegalStateException("Cannot parse schema from " + sourceDescription + ": no DOM node and no resource");
				}
			}
			if (node instanceof Element) {
				return (Element)node;
			}
			return DOMUtil.getFirstChildElement(node);
		}
	}
	
	class Input implements LSInput {

		private String publicId;
		private String systemId;
		private BufferedInputStream inputStream;

		public String getPublicId() {
		    return publicId;
		}

		public void setPublicId(String publicId) {
		    this.publicId = publicId;
		}

		public String getBaseURI() {
		    return null;
		}

		public InputStream getByteStream() {
		    return null;
		}

		public boolean getCertifiedText() {
		    return false;
		}

		public Reader getCharacterStream() {
		    return null;
		}

		public String getEncoding() {
		    return null;
		}

		public String getStringData() {
		    synchronized (inputStream) {
		        try {
		            byte[] input = new byte[inputStream.available()];
		            inputStream.read(input);
		            String contents = new String(input);
		            return contents;
		        } catch (IOException e) {
		        	LOGGER.error("IO error creating LSInput for publicID: {}, systemID: {}: {}",new Object[]{publicId, systemId, e.getMessage(), e});
		        	// TODO: better error handling
		            return null;
		        }
		    }
		}

		public void setBaseURI(String baseURI) {
		}

		public void setByteStream(InputStream byteStream) {
		}

		public void setCertifiedText(boolean certifiedText) {
		}

		public void setCharacterStream(Reader characterStream) {
		}

		public void setEncoding(String encoding) {
		}

		public void setStringData(String stringData) {
		}

		public String getSystemId() {
		    return systemId;
		}

		public void setSystemId(String systemId) {
		    this.systemId = systemId;
		}

		public BufferedInputStream getInputStream() {
		    return inputStream;
		}

		public void setInputStream(BufferedInputStream inputStream) {
		    this.inputStream = inputStream;
		}

		public Input(String publicId, String sysId, InputStream input) {
		    this.publicId = publicId;
		    this.systemId = sysId;
		    this.inputStream = new BufferedInputStream(input);
		}
	}
}
