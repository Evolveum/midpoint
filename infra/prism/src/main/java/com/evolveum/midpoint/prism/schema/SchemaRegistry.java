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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.validation.SchemaFactory;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Registry and resolver of schema files and resources.
 * 
 * 
 * @author Radovan Semancik
 *
 */
public class SchemaRegistry implements LSResourceResolver, EntityResolver, Dumpable {
	
	private static final QName DEFAULT_XSD_TYPE = DOMUtil.XSD_STRING;
	
	private javax.xml.validation.SchemaFactory schemaFactory;
	private javax.xml.validation.Schema javaxSchema;
	private EntityResolver builtinSchemaResolver;	
	private List<SchemaDescription> schemaDescriptions;
	private Map<String,SchemaDescription> parsedSchemas;
	private Map<QName,ComplexTypeDefinition> extensionSchemas;
	private PrismSchema objectSchema = null;
	private boolean initialized = false;
	private String objectSchemaNamespace;
	private DynamicNamespacePrefixMapper namespacePrefixMapper;
    @Autowired(required = true)
	private PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(SchemaRegistry.class);
	
	public SchemaRegistry() {
		super();
		this.schemaDescriptions = new ArrayList<SchemaDescription>();
		this.parsedSchemas = new HashMap<String, SchemaDescription>();
		this.extensionSchemas = new HashMap<QName, ComplexTypeDefinition>();
	}
	
	public String getObjectSchemaNamespace() {
		return objectSchemaNamespace;
	}

	public void setObjectSchemaNamespace(String objectSchemaNamespace) {
		this.objectSchemaNamespace = objectSchemaNamespace;
	}
	
	public DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
		return namespacePrefixMapper;
	}

	public void setNamespacePrefixMapper(DynamicNamespacePrefixMapper namespacePrefixMapper) {
		this.namespacePrefixMapper = namespacePrefixMapper;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	public EntityResolver getBuiltinSchemaResolver() {
		return builtinSchemaResolver;
	}

	public void setBuiltinSchemaResolver(EntityResolver builtinSchemaResolver) {
		this.builtinSchemaResolver = builtinSchemaResolver;
	}

	/**
	 * Must be called before call to initialize()
	 */
	public void registerSchemaResource(String resourcePath, String usualPrefix) throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseResource(resourcePath);
		desc.setUsualPrefix(usualPrefix);
		registerSchemaDescription(desc);
	}
	
	/**
	 * Must be called before call to initialize()
	 */
	public void registerPrismSchemaResource(String resourcePath, String usualPrefix) throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseResource(resourcePath);
		desc.setUsualPrefix(usualPrefix);
		desc.setPrismSchema(true);
		registerSchemaDescription(desc);
	}
	
	/**
	 * Must be called before call to initialize()
	 */
	public void registerPrismSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage) throws SchemaException {
		registerPrismSchemaResource(resourcePath, usualPrefix, compileTimeClassesPackage, false);
	}

	/**
	 * Must be called before call to initialize()
	 */
	public void registerPrismDefaultSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage) throws SchemaException {
		registerPrismSchemaResource(resourcePath, usualPrefix, compileTimeClassesPackage, true);
	}

	/**
	 * Must be called before call to initialize()
	 */
	public void registerPrismSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage, 
			boolean defaultSchema) throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseResource(resourcePath);
		desc.setUsualPrefix(usualPrefix);
		desc.setPrismSchema(true);
		desc.setDefault(defaultSchema);
		desc.setCompileTimeClassesPackage(compileTimeClassesPackage);
		registerSchemaDescription(desc);
	}

	/**
	 * Must be called before call to initialize()
	 * @param node
	 */
	public void registerSchema(Node node, String sourceDescription) throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseNode(node, sourceDescription);
		registerSchemaDescription(desc);
	}

	/**
	 * Must be called before call to initialize()
	 * @param node
	 */
	public void registerSchema(Node node, String sourceDescription, String usualPrefix) throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseNode(node, sourceDescription);
		desc.setUsualPrefix(usualPrefix);
		registerSchemaDescription(desc);
	}
	
	public void registerPrismSchemaFile(File file) throws FileNotFoundException, SchemaException {
		loadPrismSchemaFileDescription(file);
	}

	public SchemaDescription loadPrismSchemaFileDescription(File file) throws FileNotFoundException, SchemaException {
		SchemaDescription desc = SchemaDescription.parseFile(file);
		desc.setPrismSchema(true);
		registerSchemaDescription(desc);
		return desc;
	}

	private void registerSchemaDescription(SchemaDescription desc) {
		if (desc.getUsualPrefix() != null) {
			namespacePrefixMapper.registerPrefix(desc.getNamespace(), desc.getUsualPrefix(), desc.isDefault());
		}
		parsedSchemas.put(desc.getNamespace(), desc);
		schemaDescriptions.add(desc);
	}
	
	public void registerPrismSchemasFromDirectory(File directory) throws FileNotFoundException, SchemaException {
		List<File> files = Arrays.asList(directory.listFiles());
		// Sort the filenames so we have deterministic order of loading
		// This is useful in tests but may come handy also during customization
		Collections.sort(files);
		for (File file: files) {
			if (file.getName().startsWith(".")) {
				// skip dotfiles. this will skip SVN data and similar things
				continue;
			}
			if (file.isDirectory()) {
				registerPrismSchemasFromDirectory(file);
			}
			if (file.isFile()) {
				registerPrismSchemaFile(file);
			}
		}
	}
	
	/**
	 * This can be used to read additional schemas even after the registry was initialized.
	 */
	public void loadPrismSchemasFromDirectory(File directory) throws FileNotFoundException, SchemaException {
		List<File> files = Arrays.asList(directory.listFiles());
		// Sort the filenames so we have deterministic order of loading
		// This is useful in tests but may come handy also during customization
		Collections.sort(files);
		for (File file: files) {
			if (file.getName().startsWith(".")) {
				// skip dotfiles. this will skip SVN data and similar things
				continue;
			}
			if (file.isDirectory()) {
				loadPrismSchemasFromDirectory(file);
			}
			if (file.isFile()) {
				loadPrismSchemaFile(file);
			}
		}
	}
	
	public void loadPrismSchemaFile(File file) throws FileNotFoundException, SchemaException {
		SchemaDescription desc = loadPrismSchemaFileDescription(file);
		parsePrismSchema(desc);
	}

    public void loadPrismSchemaResource(String resourcePath) throws SchemaException {
        SchemaDescription desc = SchemaDescription.parseResource(resourcePath);
        desc.setPrismSchema(true);
        registerSchemaDescription(desc);
        parsePrismSchema(desc);
    }

    /**
	 * This can be used to read additional schemas even after the registry was initialized.
	 */	
	public void initialize() throws SAXException, IOException, SchemaException {
		if (prismContext == null) {
			throw new IllegalStateException("Prism context not set");
		}
		if (namespacePrefixMapper == null) {
			throw new IllegalStateException("Namespace prefix mapper not set");
		}
		try {
			initResolver();
			parsePrismSchemas();
			parseJavaxSchema();
			compileCompileTimeClassList();
			initialized = true;
			
		} catch (SAXException ex) {
			if (ex instanceof SAXParseException) {
				SAXParseException sex = (SAXParseException)ex;
				throw new SchemaException("Error parsing schema "+sex.getSystemId()+" line "+sex.getLineNumber()+": "+sex.getMessage());
			}
			throw ex;
		}
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

	private void parsePrismSchemas() throws SchemaException {
		for (SchemaDescription schemaDescription : schemaDescriptions) {
			if (schemaDescription.isPrismSchema()) {
				parsePrismSchema(schemaDescription);
			}
		}
	}
	
	private void parsePrismSchema(SchemaDescription schemaDescription) throws SchemaException {
		String namespace = schemaDescription.getNamespace();
		
		Element domElement = schemaDescription.getDomElement();
		boolean isRuntime = schemaDescription.getCompileTimeClassesPackage() == null;
		PrismSchema schema = PrismSchema.parse(domElement, this, isRuntime, schemaDescription.getSourceDescription(), getPrismContext());
		if (namespace == null) {
			namespace = schema.getNamespace();
		}
		LOGGER.trace("Parsed schema {}, namespace: {}",schemaDescription.getSourceDescription(),namespace);
		schemaDescription.setSchema(schema);
		detectExtensionSchema(schema);
	}

	private void detectExtensionSchema(PrismSchema schema) throws SchemaException {
		for (ComplexTypeDefinition def: schema.getDefinitions(ComplexTypeDefinition.class)) {
			QName extType = def.getExtensionForType();
			if (extType != null) {
				if (extensionSchemas.containsKey(extType)) {
					throw new SchemaException("Duplicate definition of extension for type "+extType+": "+def+" and "+extensionSchemas.get(extType));
				}
				extensionSchemas.put(extType, def);
			}
		}
	}
	
	private void compileCompileTimeClassList() {
		for (SchemaDescription schemaDescription : schemaDescriptions) {	
			Package pkg = schemaDescription.getCompileTimeClassesPackage();
			if (pkg != null) {
				Map<QName, Class<?>> map = createXsdTypeMap(pkg);
				schemaDescription.setXsdTypeTocompileTimeClassMap(map);
			}
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
	
	public PrismSchema getSchema(String namespace) {
		return parsedSchemas.get(namespace).getSchema();
	}
	
	/**
	 * Returns a schema that contains all the object definitions augmented with
	 * extension definitions as appropriate. This is the method intended for common
	 * usage in the code.
	 * 
	 * The returned schema is considered to be immutable. Any attempt to change it
	 * may lead to unexpected results. 
	 */
	public PrismSchema getObjectSchema() {
		if (!initialized) {
			throw new IllegalStateException("Attempt to get common schema from uninitialized Schema Registry");
		}
		if (objectSchema == null) {
			initializeObjectSchema();
		}
		return objectSchema;
	}
	
	private void initializeObjectSchema() {
		if (objectSchemaNamespace == null) {
			throw new IllegalArgumentException("Object schema namespace is not set");
		}
		PrismSchema commonSchema = parsedSchemas.get(objectSchemaNamespace).getSchema();
		// FIXME
		objectSchema = new PrismSchema(objectSchemaNamespace, prismContext);
		for (Definition def: commonSchema.getDefinitions()) {
			if (def instanceof PrismObjectDefinition<?>) {
				QName typeName = def.getTypeName();
				if (extensionSchemas.containsKey(typeName)) {
					LOGGER.trace("Applying extension type for {}: {}", typeName, extensionSchemas.get(typeName));
					PrismObjectDefinition<?> objDef = (PrismObjectDefinition<?>)def;
					PrismObjectDefinition<?> enhDef = objDef.clone();
					enhDef.setExtensionDefinition(extensionSchemas.get(typeName));
					def = enhDef;
					LOGGER.trace("Resuting object type def:\n{}", enhDef.dump());
				}
			}
			objectSchema.getDefinitions().add(def);
		}
	}
	
	public Collection<Package> getCompileTimePackages() {
		Collection<Package> compileTimePackages = new ArrayList<Package>(schemaDescriptions.size());
		for (SchemaDescription desc : schemaDescriptions) {
			if (desc.getCompileTimeClassesPackage() != null) {
				compileTimePackages.add(desc.getCompileTimeClassesPackage());
			}
		}
		return compileTimePackages;
	}
	
	private Map<QName, Class<?>> createXsdTypeMap(Package pkg) {
		Map<QName, Class<?>> map = new HashMap<QName, Class<?>>();
		for (Class clazz: ClassPathUtil.listClasses(pkg)) {
			QName typeName = JAXBUtil.getTypeQName(clazz);
			if (typeName != null) {
				map.put(typeName, clazz);
			}
		}
		return map;
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
	 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		// Prefer built-in resolution over the pre-parsed one. This is less efficient, but if the order is swapped
		// the parsers complaints about re-definition of the elements
//		InputSource inputSource = resolveResourceUsingBuiltinResolver(null, null, publicId, systemId, null);
		InputSource inputSource = resolveResourceFromRegisteredSchemas(null, publicId, publicId, systemId, null);
		if (inputSource == null) {
			inputSource = resolveResourceUsingBuiltinResolver(null, null, publicId, systemId, null);
//			inputSource = resolveResourceFromRegisteredSchemas(null, publicId, publicId, systemId, null);
		}
		if (inputSource == null) {
			LOGGER.error("Unable to resolve resource with publicID: {}, systemID: {}",new Object[]{publicId, systemId});
			return null;
		}
		LOGGER.trace("Resolved resource with publicID: {}, systemID: {} : {}",new Object[]{publicId, systemId, inputSource});
		return inputSource;
	}

	
	/* (non-Javadoc)
	 * @see org.w3c.dom.ls.LSResourceResolver#resolveResource(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId,
			String baseURI) {
		// Prefer built-in resolution over the pre-parsed one. This is less efficient, but if the order is swapped
		// the parsers complaints about re-definition of the elements
//		InputSource inputSource = resolveResourceUsingBuiltinResolver(type,namespaceURI,publicId,systemId,baseURI);
		 InputSource inputSource = resolveResourceFromRegisteredSchemas(type,namespaceURI,publicId,systemId,baseURI);
		if (inputSource == null) {
			inputSource = resolveResourceUsingBuiltinResolver(type,namespaceURI,publicId,systemId,baseURI);
//			inputSource = resolveResourceFromRegisteredSchemas(type,namespaceURI,publicId,systemId,baseURI);
		}
		if (inputSource == null) {
			LOGGER.error("Unable to resolve resource of type {}, namespaceURI: {}, publicID: {}, systemID: {}, baseURI: {}",new Object[]{type, namespaceURI, publicId, systemId, baseURI});
			return null;
		}
		LOGGER.trace("Resolved resource of type {}, namespaceURI: {}, publicID: {}, systemID: {}, baseURI: {} : {}",new Object[]{type, namespaceURI, publicId, systemId, baseURI, inputSource});
		return new Input(publicId, systemId, inputSource.getByteStream());
	}
		
	private InputSource resolveResourceFromRegisteredSchemas(String type, String namespaceURI,
			String publicId, String systemId, String baseURI) {
		InputSource source = resolveResourceFromRegisteredSchemasByNamespace(namespaceURI);
		if (source == null) {
			source = resolveResourceFromRegisteredSchemasByNamespace(publicId);
		}
		if (source == null) {
			source = resolveResourceFromRegisteredSchemasByNamespace(systemId);
		}
		return source;
	}
		
	private InputSource resolveResourceFromRegisteredSchemasByNamespace(String namespaceURI) {
		if (namespaceURI != null) {
			if (parsedSchemas.containsKey(namespaceURI)) {
				SchemaDescription schemaDescription = parsedSchemas.get(namespaceURI);
				if (schemaDescription.canInputStream()) {
					InputStream inputStream = schemaDescription.openInputStream();
					InputSource source = new InputSource();
					source.setByteStream(inputStream);
					//source.setSystemId(schemaDescription.getPath());
					// Make sure that both publicId and systemId are always set to schema namespace
					// this helps to avoid double processing of the schemas
					source.setSystemId(namespaceURI);
					source.setPublicId(namespaceURI);
					return source;
				} else {
					throw new IllegalStateException("Requested resolution of schema "+schemaDescription.getSourceDescription()+" that does not support input stream");
				}
			}
		}
		return null;
	}

	public InputSource resolveResourceUsingBuiltinResolver(String type, String namespaceURI, String publicId, String systemId,
				String baseURI) {
		InputSource inputSource = null;
		try {
			if (namespaceURI != null) {
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
		return inputSource;
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.Dumpable#dump()
	 */
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder("SchemaRegistry:");
		
		sb.append("  Parsed Schemas:\n");
		for (String namespace: parsedSchemas.keySet()) {
			sb.append("    ");
			sb.append(namespace);
			sb.append(": ");
			sb.append(parsedSchemas.get(namespace).dump());
			sb.append("\n");
		}
		return sb.toString();
	}

	public Class<?> determineCompileTimeClass(QName elementName, ComplexTypeDefinition complexTypeDefinition) {
		return determineCompileTimeClass(complexTypeDefinition.getTypeName());
	}
	
	public Class<?> determineCompileTimeClass(ComplexTypeDefinition complexTypeDefinition) {
		return determineCompileTimeClass(complexTypeDefinition.getTypeName());
	}
		
	public Class<?> determineCompileTimeClass(QName typeName) {
		if (typeName.getNamespaceURI() == null) {
			throw new IllegalArgumentException("XSD type "+typeName+" has no namespace, cannot determine schema");
		}
		SchemaDescription desc = findSchemaDescriptionByNamespace(typeName.getNamespaceURI());
		if (desc == null) {
			return null;
		}
		Package pkg = desc.getCompileTimeClassesPackage();
		if (pkg == null) {
			return null;
		}
		Class<?> compileTimeClass = JAXBUtil.findClassForType(typeName, pkg);
		return compileTimeClass;
	}

	public PrismSchema findSchemaByCompileTimeClass(Class<? extends Objectable> compileTimeClass) {
		Package compileTimePackage = compileTimeClass.getPackage();
		for (SchemaDescription desc: schemaDescriptions) {
			if (compileTimePackage.equals(desc.getCompileTimeClassesPackage())) {
				PrismSchema schema = desc.getSchema();
				if (schema.getNamespace().equals(objectSchemaNamespace)) {
					return getObjectSchema();
				}
				return schema;
			}
		}
		return null;
	}
	
	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByCompileTimeClass(Class<T> compileTimeClass) {
		PrismSchema schema = findSchemaByCompileTimeClass(compileTimeClass);
		if (schema == null) {
			return null;
		}
		return schema.findObjectDefinitionByCompileTimeClass(compileTimeClass);
	}
	
	public void applyDefinition(PrismObject<? extends Objectable> prismObject, Class<? extends Objectable> type) throws SchemaException {
		applyDefinition(prismObject, type, true);
	}
	
	/**
	 * This method will try to locate the appropriate object definition and apply it.
	 */
	public void applyDefinition(PrismObject<? extends Objectable> prismObject, Class<? extends Objectable> type, boolean force) throws SchemaException {
		PrismObjectDefinition<? extends Objectable> objectDefinition = determineDefinitionFromClass(type);
		prismObject.applyDefinition(objectDefinition, force);
	}
	
	/**
	 * This method will try to locate the appropriate object definition and apply it.
	 */
	public <T extends Objectable> void applyDefinition(ObjectDelta<T> objectDelta, Class<T> type, boolean force) throws SchemaException {
		PrismObjectDefinition<T> objectDefinition = determineDefinitionFromClass(type);
		objectDelta.applyDefinition(objectDefinition, force);
	}
	
	public <C extends Containerable, O extends Objectable> void applyDefinition(PrismContainerValue<C> prismContainerValue, Class<O> type, 
			ItemPath path, boolean force) throws SchemaException {
		PrismObjectDefinition<O> objectDefinition = determineDefinitionFromClass(type);
		PrismContainerDefinition<C> containerDefinition = objectDefinition.findContainerDefinition(path);
		prismContainerValue.applyDefinition(containerDefinition, force);
	}
		
	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByType(QName typeName) {
		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findObjectDefinitionByType(typeName);
	}
	
	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByElementName(QName elementName) {
		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findObjectDefinitionByElementName(elementName);
	}
	
	public PrismContainerDefinition findContainerDefinitionByType(QName typeName) {
		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findContainerDefinitionByType(typeName);
	}
	
	public PrismContainerDefinition findContainerDefinitionByElementName(QName elementName) {
		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findContainerDefinitionByElementName(elementName);
	}

    public ItemDefinition findItemDefinitionByElementName(QName elementName) {
        PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
        if (schema == null) {
            return null;
        }
        return schema.findItemDefinition(elementName, ItemDefinition.class);
    }

	public PrismPropertyDefinition findPropertyDefinitionByElementName(QName elementName) {
		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findPropertyDefinitionByElementName(elementName);
	}
	
	public ComplexTypeDefinition findComplexTypeDefinition(QName typeName) {
		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findComplexTypeDefinition(typeName);
	}


	public PrismSchema findSchemaByNamespace(String namespaceURI) {
		// Prefer object schema
		if (namespaceURI.equals(objectSchemaNamespace)) {
			return getObjectSchema();
		}
		SchemaDescription desc = findSchemaDescriptionByNamespace(namespaceURI);
		if (desc == null) {
			return null;
		}
		return desc.getSchema();
	}
		
	public SchemaDescription findSchemaDescriptionByNamespace(String namespaceURI) {
		for (SchemaDescription desc: schemaDescriptions) {
			if (namespaceURI.equals(desc.getNamespace())) {
				return desc;
			}
		}
		return null;
	}
	
	public PrismObjectDefinition determineDefinitionFromClass(Class type) {
		PrismObjectDefinition def = findObjectDefinitionByCompileTimeClass(type);
		if (def != null) {
			return def;
		}
		Class<?> superclass = type.getSuperclass();
		if (superclass == Object.class) {
			return null;
		}
		return determineDefinitionFromClass(superclass);
	}

	/**
	 * Returns true if specified element has a definition that matches specified type
	 * in the known schemas.
	 */
	public boolean hasImplicitTypeDefinition(QName elementName, QName typeName) {
		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
		ItemDefinition itemDefinition = schema.findItemDefinition(elementName, ItemDefinition.class);
		if (itemDefinition == null) {
			return false;
		}
		return typeName.equals(itemDefinition.getTypeName());
	}

	public static ItemDefinition createDefaultItemDefinition(QName itemName, PrismContext prismContext) {
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(itemName, itemName, DEFAULT_XSD_TYPE, prismContext);
		// Set it to multi-value to be on the safe side
		propDef.setMaxOccurs(-1);
		propDef.setDynamic(true);
		return propDef;
	}

	/**
	 * Looks for a top-level definition for the specified element name (in all schemas). 
	 */
	public ItemDefinition resolveGlobalItemDefinition(QName elementQName) throws SchemaException {
		String elementNamespace = elementQName.getNamespaceURI();
		if (elementNamespace == null) {
			return null;
		}
		PrismSchema schema = findSchemaByNamespace(elementNamespace);
		if (schema == null) {
			return null;
		}
		ItemDefinition itemDefinition = schema.findItemDefinition(elementQName, ItemDefinition.class);
		if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+elementQName+" (schema for the namespace is present)");
		}
		return itemDefinition;
	}
	
	public <T extends Objectable> PrismObject<T> instantiate(Class<T> compileTimeClass) throws SchemaException {
		PrismObjectDefinition<T> objDef = findObjectDefinitionByCompileTimeClass(compileTimeClass);
		if (objDef == null) {
			throw new SchemaException("No definition for compile time class "+compileTimeClass);
		}
		return objDef.instantiate();
	}


}
