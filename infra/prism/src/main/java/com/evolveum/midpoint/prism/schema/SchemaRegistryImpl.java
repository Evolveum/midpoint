/*
 * Copyright (c) 2010-2015 Evolveum
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.validation.SchemaFactory;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
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
public class SchemaRegistryImpl implements DebugDumpable, SchemaRegistry {

	private static final QName DEFAULT_XSD_TYPE = DOMUtil.XSD_STRING;

    private static final String DEFAULT_RUNTIME_CATALOG_RESOURCE = "META-INF/catalog-runtime.xml";

	private File[] catalogFiles;														// overrides catalog resource name
    private String catalogResourceName = DEFAULT_RUNTIME_CATALOG_RESOURCE;

	private javax.xml.validation.SchemaFactory schemaFactory;
	private javax.xml.validation.Schema javaxSchema;
	private EntityResolver builtinSchemaResolver;
	final private List<SchemaDescription> schemaDescriptions = new ArrayList<>();
	// namespace -> schemas; in case of extension schemas there could be more of them with the same namespace!
	final private MultiValuedMap<String,SchemaDescription> parsedSchemas = new ArrayListValuedHashMap<>();
	// base type name -> CTD with (merged) extension definition
	final private Map<QName,ComplexTypeDefinition> extensionSchemas = new HashMap<>();
	private boolean initialized = false;
	private DynamicNamespacePrefixMapper namespacePrefixMapper;
	private String defaultNamespace;

	private XmlEntityResolver entityResolver = new XmlEntityResolverImpl(this);

	@Autowired		// TODO does this work?
	private PrismContext prismContext;

	private static final Trace LOGGER = TraceManager.getTrace(SchemaRegistryImpl.class);

	@Override
	public DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
		return namespacePrefixMapper;
	}

	public void setNamespacePrefixMapper(DynamicNamespacePrefixMapper namespacePrefixMapper) {
		this.namespacePrefixMapper = namespacePrefixMapper;
	}

	@Override
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public XmlEntityResolver getEntityResolver() {
		return entityResolver;
	}

	public MultiValuedMap<String, SchemaDescription> getParsedSchemas() {
		return parsedSchemas;
	}

	public EntityResolver getBuiltinSchemaResolver() {
		return builtinSchemaResolver;
	}

	public File[] getCatalogFiles() {
		return catalogFiles;
	}

	public void setCatalogFiles(File[] catalogFiles) {
		this.catalogFiles = catalogFiles;
	}

	public String getCatalogResourceName() {
		return catalogResourceName;
	}

	public void setCatalogResourceName(String catalogResourceName) {
		this.catalogResourceName = catalogResourceName;
	}

	@Override
	public String getDefaultNamespace() {
		return defaultNamespace;
	}

	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}

	//region Registering resources and initialization
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

    public void registerPrismSchemasFromWsdlResource(String resourcePath, List<Package> compileTimeClassesPackages) throws SchemaException {
        List<SchemaDescription> descriptions = SchemaDescription.parseWsdlResource(resourcePath);
        Iterator<Package> pkgIterator = null;
        if (compileTimeClassesPackages != null) {
            if (descriptions.size() != compileTimeClassesPackages.size()) {
                throw new SchemaException("Mismatch between the size of compileTimeClassesPackages ("+compileTimeClassesPackages.size()
                        +" and schemas in "+resourcePath+" ("+descriptions.size()+")");
            }
            pkgIterator = compileTimeClassesPackages.iterator();
        }
        for (SchemaDescription desc : descriptions) {
            desc.setPrismSchema(true);
            if (pkgIterator != null) {
                desc.setCompileTimeClassesPackage(pkgIterator.next());
            }
            registerSchemaDescription(desc);
        }
    }

    /**
	 * Must be called before call to initialize()
	 */
	public void registerPrismSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage) throws SchemaException {
		registerPrismSchemaResource(resourcePath, usualPrefix, compileTimeClassesPackage, false, false);
	}

    /**
     * Must be called before call to initialize()
     */
    public void registerPrismSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage, boolean prefixDeclaredByDefault) throws SchemaException {
        registerPrismSchemaResource(resourcePath, usualPrefix, compileTimeClassesPackage, false, prefixDeclaredByDefault);
    }

    /**
	 * Must be called before call to initialize()
	 */
	public void registerPrismDefaultSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage) throws SchemaException {
		registerPrismSchemaResource(resourcePath, usualPrefix, compileTimeClassesPackage, true, true);
	}

	/**
	 * Must be called before call to initialize()
     *
     * @param prefixDeclaredByDefault Whether this prefix will be declared in top element in all XML serializations (MID-2198)
	 */
	public void registerPrismSchemaResource(String resourcePath, String usualPrefix, Package compileTimeClassesPackage,
			boolean defaultSchema, boolean prefixDeclaredByDefault) throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseResource(resourcePath);
		desc.setUsualPrefix(usualPrefix);
		desc.setPrismSchema(true);
		desc.setDefault(defaultSchema);
        desc.setDeclaredByDefault(prefixDeclaredByDefault);
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

	public void registerPrismSchema(InputStream input, String sourceDescription) throws SchemaException {
		loadPrismSchemaDescription(input, sourceDescription);
	}

	public SchemaDescription loadPrismSchemaFileDescription(File file) throws FileNotFoundException, SchemaException {
		 if (!(file.getName().matches(".*\\.xsd$"))){
         	LOGGER.trace("Skipping registering {}, because it is not schema definition.", file.getAbsolutePath());
         	return null;
         }
		SchemaDescription desc = SchemaDescription.parseFile(file);
		desc.setPrismSchema(true);
		registerSchemaDescription(desc);
		return desc;
	}

	public SchemaDescription loadPrismSchemaDescription(InputStream input, String sourceDescription)
			throws SchemaException {
		SchemaDescription desc = SchemaDescription.parseInputStream(input, sourceDescription);
		desc.setPrismSchema(true);
		registerSchemaDescription(desc);
		return desc;
	}

	private void registerSchemaDescription(SchemaDescription desc) {
		if (desc.getUsualPrefix() != null) {
			namespacePrefixMapper.registerPrefix(desc.getNamespace(), desc.getUsualPrefix(), desc.isDefault());
            if (desc.isDeclaredByDefault()) {
                namespacePrefixMapper.addDeclaredByDefault(desc.getUsualPrefix());
            }
		}
		parsedSchemas.put(desc.getNamespace(), desc);
		schemaDescriptions.add(desc);
	}

	public void registerPrismSchemasFromDirectory(File directory) throws FileNotFoundException, SchemaException {
        File[] fileArray = directory.listFiles();
        if (fileArray != null) {
            List<File> files = Arrays.asList(fileArray);
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
		parsePrismSchema(desc, false);
	}

    public void loadPrismSchemaResource(String resourcePath) throws SchemaException {
        SchemaDescription desc = SchemaDescription.parseResource(resourcePath);
        desc.setPrismSchema(true);
        registerSchemaDescription(desc);
        parsePrismSchema(desc, false);
    }

    /**
	 * This can be used to read additional schemas even after the registry was initialized.
	 */
	@Override
	public void initialize() throws SAXException, IOException, SchemaException {
		if (prismContext == null) {
			throw new IllegalStateException("Prism context not set");
		}
		if (namespacePrefixMapper == null) {
			throw new IllegalStateException("Namespace prefix mapper not set");
		}
		try {
			LOGGER.info("initialize() starting");
			long start = System.currentTimeMillis();

			initResolver();
			long resolverDone = System.currentTimeMillis();
			LOGGER.info("initResolver() done in {} ms", resolverDone - start);

			parsePrismSchemas();
			long prismSchemasDone = System.currentTimeMillis();
			LOGGER.info("parsePrismSchemas() done in {} ms", prismSchemasDone - resolverDone);

			parseJavaxSchema();
			long javaxSchemasDone = System.currentTimeMillis();
			LOGGER.info("parseJavaxSchema() done in {} ms", javaxSchemasDone - prismSchemasDone);

			compileCompileTimeClassList();
			long classesDone = System.currentTimeMillis();
			LOGGER.info("compileCompileTimeClassList() done in {} ms", classesDone - javaxSchemasDone);

			initialized = true;
		} catch (SAXException ex) {
			if (ex instanceof SAXParseException) {
				SAXParseException sex = (SAXParseException)ex;
				throw new SchemaException("Error parsing schema "+sex.getSystemId()+" line "+sex.getLineNumber()+": "+sex.getMessage(), sex);
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
		schemaFactory.setResourceResolver(entityResolver);
		javaxSchema = schemaFactory.newSchema(sources);
	}

	private void parsePrismSchemas() throws SchemaException {
		parsePrismSchemas(schemaDescriptions, true);
		applySchemaExtensions();
		for (SchemaDescription schemaDescription : schemaDescriptions) {
			if (schemaDescription.getSchema() != null) {
				resolveMissingTypeDefinitionsInGlobalItemDefinitions((PrismSchemaImpl) schemaDescription.getSchema());
			}
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("====================================== Dumping prism schemas ======================================\n");
			for (SchemaDescription schemaDescription : schemaDescriptions) {
				LOGGER.trace("************************************************************* {} (in {})",
						schemaDescription.getNamespace(), schemaDescription.getPath());
				if (schemaDescription.getSchema() != null) {
					LOGGER.trace("{}", schemaDescription.getSchema().debugDump());
				}
			}
		}
	}

	// global item definitions may refer to types that are not yet available
	private void resolveMissingTypeDefinitionsInGlobalItemDefinitions(PrismSchemaImpl schema) throws SchemaException {
		for (Iterator<DefinitionSupplier> iterator = schema.getDelayedItemDefinitions().iterator(); iterator.hasNext(); ) {
			DefinitionSupplier definitionSupplier = iterator.next();
			Definition definition = definitionSupplier.get();
			if (definition != null) {
				schema.add(definition);
			}
			iterator.remove();
		}
	}

	// only in exceptional situations
	// may not work for schemas with circular references
	private void parsePrismSchema(SchemaDescription schemaDescription, boolean allowDelayedItemDefinitions) throws SchemaException {
		String namespace = schemaDescription.getNamespace();

		Element domElement = schemaDescription.getDomElement();
		boolean isRuntime = schemaDescription.getCompileTimeClassesPackage() == null;
		long started = System.currentTimeMillis();
		LOGGER.trace("Parsing schema {}, namespace: {}, isRuntime: {}",
				schemaDescription.getSourceDescription(), namespace, isRuntime);
		PrismSchema schema = PrismSchemaImpl.parse(domElement, entityResolver, isRuntime,
				schemaDescription.getSourceDescription(), allowDelayedItemDefinitions, getPrismContext());
		if (StringUtils.isEmpty(namespace)) {
			namespace = schema.getNamespace();
		}
		LOGGER.trace("Parsed schema {}, namespace: {}, isRuntime: {} in {} ms",
				schemaDescription.getSourceDescription(), namespace, isRuntime, System.currentTimeMillis()-started);
		schemaDescription.setSchema(schema);
		detectExtensionSchema(schema);
	}

	// see https://stackoverflow.com/questions/14837293/xsd-circular-import
	private void parsePrismSchemas(List<SchemaDescription> schemaDescriptions, boolean allowDelayedItemDefinitions) throws SchemaException {
		List<SchemaDescription> prismSchemaDescriptions = schemaDescriptions.stream()
				.filter(sd -> sd.isPrismSchema())
				.collect(Collectors.toList());
		Element schemaElement = DOMUtil.createElement(DOMUtil.XSD_SCHEMA_ELEMENT);
		schemaElement.setAttribute("targetNamespace", "http://dummy/");
		schemaElement.setAttribute("elementFormDefault", "qualified");

		// These fragmented namespaces should not be included in wrapper XSD because they are defined in multiple XSD files.
		// We have to process them one by one.
		MultiValuedMap<String, SchemaDescription> schemasByNamespace = new ArrayListValuedHashMap<>();
		prismSchemaDescriptions.forEach(sd -> schemasByNamespace.put(sd.getNamespace(), sd));
		List<String> fragmentedNamespaces = schemasByNamespace.keySet().stream()
				.filter(ns -> schemasByNamespace.get(ns).size() > 1)
				.collect(Collectors.toList());
		LOGGER.trace("Fragmented namespaces: {}", fragmentedNamespaces);

		List<SchemaDescription> wrappedDescriptions = new ArrayList<>();
		for (SchemaDescription description : prismSchemaDescriptions) {
			String namespace = description.getNamespace();
			if (!fragmentedNamespaces.contains(namespace)) {
				Element importElement = DOMUtil.createSubElement(schemaElement, DOMUtil.XSD_IMPORT_ELEMENT);
				importElement.setAttribute(DOMUtil.XSD_ATTR_NAMESPACE.getLocalPart(), namespace);
				description.setSchema(new PrismSchemaImpl(prismContext));
				wrappedDescriptions.add(description);
			}
		}
		if (LOGGER.isTraceEnabled()) {
			String xml = DOMUtil.serializeDOMToString(schemaElement);
			LOGGER.trace("Wrapper XSD:\n{}", xml);
		}

		long started = System.currentTimeMillis();
		LOGGER.trace("Parsing {} schemas wrapped in single XSD", wrappedDescriptions.size());
		PrismSchemaImpl.parseSchemas(schemaElement, entityResolver,
				wrappedDescriptions, allowDelayedItemDefinitions, getPrismContext());
		LOGGER.trace("Parsed {} schemas in {} ms",
				wrappedDescriptions.size(), System.currentTimeMillis()-started);

		for (SchemaDescription description : wrappedDescriptions) {
			detectExtensionSchema(description.getSchema());
		}

		for (String namespace : fragmentedNamespaces) {
			Collection<SchemaDescription> fragments = schemasByNamespace.get(namespace);
			LOGGER.trace("Parsing {} schemas for fragmented namespace {}", fragments.size(), namespace);
			for (SchemaDescription schemaDescription : fragments) {
				parsePrismSchema(schemaDescription, allowDelayedItemDefinitions);
			}
		}
	}

	private void detectExtensionSchema(PrismSchema schema) throws SchemaException {
		for (ComplexTypeDefinition def: schema.getDefinitions(ComplexTypeDefinition.class)) {
			QName extType = def.getExtensionForType();
			if (extType != null) {
				LOGGER.trace("Processing {} as an extension for {}", def, extType);
				if (extensionSchemas.containsKey(extType)) {
					ComplexTypeDefinition existingExtension = extensionSchemas.get(extType);
					existingExtension.merge(def);
				} else {
					extensionSchemas.put(extType, def.clone());
				}
			}
		}
	}

	private void applySchemaExtensions() throws SchemaException {
		for (Entry<QName,ComplexTypeDefinition> entry: extensionSchemas.entrySet()) {
			QName typeQName = entry.getKey();
			ComplexTypeDefinition extensionCtd = entry.getValue();
			ComplexTypeDefinition primaryCtd = findComplexTypeDefinition(typeQName);
			PrismContainerDefinition extensionContainer = primaryCtd.findContainerDefinition(
					new QName(primaryCtd.getTypeName().getNamespaceURI(), PrismConstants.EXTENSION_LOCAL_NAME));
			if (extensionContainer == null) {
				throw new SchemaException("Attempt to extend type "+typeQName+" with "+extensionCtd.getTypeClass()+" but the original type does not have extension container");
			}
			((PrismContainerDefinitionImpl) extensionContainer).setComplexTypeDefinition(extensionCtd.clone());
			((PrismContainerDefinitionImpl) extensionContainer).setTypeName(extensionCtd.getTypeName());
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

		if (catalogFiles != null && catalogFiles.length > 0) {
			for (File catalogFile : catalogFiles) {
				LOGGER.trace("Using catalog file {}", catalogFile);
				catalog.parseCatalog(catalogFile.getPath());
			}
		} else if (catalogResourceName != null) {
			LOGGER.trace("Using catalog from resource: {}", catalogResourceName);
			Enumeration<URL> catalogs = Thread.currentThread().getContextClassLoader().getResources(catalogResourceName);
			while (catalogs.hasMoreElements()) {
				URL catalogResourceUrl = catalogs.nextElement();
				LOGGER.trace("Parsing catalog from URL: {}", catalogResourceUrl);
				catalog.parseCatalog(catalogResourceUrl);
			}
		} else {
			throw new IllegalStateException("Catalog is not defined");
		}

		builtinSchemaResolver = catalogResolver;
	}
	//endregion

	//region Schemas and type maps (TODO)
	@Override
	public javax.xml.validation.Schema getJavaxSchema() {
		return javaxSchema;
	}

	@Override
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
	//endregion




	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SchemaRegistry:");
		sb.append("  Parsed Schemas:");
		for (String namespace: parsedSchemas.keySet()) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append(namespace);
			sb.append(": ");
			sb.append(parsedSchemas.get(namespace));
		}
		return sb.toString();
	}

	//region applyDefinition(..) methods
	@Override
	public <C extends Containerable> void applyDefinition(PrismContainer<C> container, Class<C> type) throws SchemaException {
		applyDefinition(container, type, true);
	}

	@Override
	public <C extends Containerable> void applyDefinition(PrismContainer<C> container, Class<C> compileTimeClass, boolean force) throws SchemaException {
		PrismContainerDefinition<C> definition = determineDefinitionFromClass(compileTimeClass);
		container.applyDefinition(definition, force);
	}

	@Override
	public <O extends Objectable> void applyDefinition(ObjectDelta<O> objectDelta, Class<O> compileTimeClass, boolean force) throws SchemaException {
		PrismObjectDefinition<O> objectDefinition = determineDefinitionFromClass(compileTimeClass);
		objectDelta.applyDefinition(objectDefinition, force);
	}

	@Override
	public <C extends Containerable, O extends Objectable> void applyDefinition(PrismContainerValue<C> prismContainerValue,
			Class<O> compileTimeClass, ItemPath path, boolean force) throws SchemaException {
		PrismObjectDefinition<O> objectDefinition = determineDefinitionFromClass(compileTimeClass);
		PrismContainerDefinition<C> containerDefinition = objectDefinition.findContainerDefinition(path);
		prismContainerValue.applyDefinition(containerDefinition, force);
	}

	@Override
	public <C extends Containerable> void applyDefinition(PrismContainerValue<C> prismContainerValue, QName typeName,
			ItemPath path, boolean force) throws SchemaException {
		PrismObjectDefinition objectDefinition = findObjectDefinitionByType(typeName);
		if (objectDefinition != null) {
			PrismContainerDefinition<C> containerDefinition = objectDefinition.findContainerDefinition(path);
			prismContainerValue.applyDefinition(containerDefinition, force);
			return;
		}
		PrismContainerDefinition typeDefinition = findContainerDefinitionByType(typeName);
		if (typeDefinition != null) {
			PrismContainerDefinition<C> containerDefinition = typeDefinition.findContainerDefinition(path);
			prismContainerValue.applyDefinition(containerDefinition, force);
			return;
		}
		ComplexTypeDefinition complexTypeDefinition = findComplexTypeDefinition(typeName);
		if (complexTypeDefinition != null) {
			PrismContainerDefinition<C> containerDefinition = complexTypeDefinition.findContainerDefinition(path);
			prismContainerValue.applyDefinition(containerDefinition, force);
			return;
		}
		throw new SchemaException("No definition for container "+path+" in type "+typeName);
	}
	//endregion






	private boolean namespaceMatches(String namespace, @Nullable List<String> ignoredNamespaces) {
		if (ignoredNamespaces == null) {
			return false;
		}
		for (String ignored : ignoredNamespaces) {
			if (namespace.startsWith(ignored)) {
				return true;
			}
		}
		return false;
	}






	//region Finding items (standard cases - core methods)

//	@Override
//	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByType(@NotNull QName typeName) {
//		if (StringUtils.isEmpty(typeName.getNamespaceURI())) {
//			// a quick hack (todo do it seriously)
//			ComplexTypeDefinition ctd = resolveGlobalTypeDefinitionWithoutNamespace(typeName.getLocalPart());
//			if (ctd == null) {
//				return null;
//			}
//			typeName = ctd.getTypeName();
//		}
//		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
//		if (schema == null){
//			//TODO: check for confilicted objects
//			//			Iterator<PrismSchema> schemaIterator = getSchemas().iterator();
//			//			while (schemaIterator.hasNext()){
//			//				schema = schemaIterator.next();
//			//				if (schema == null){
//			//					continue;
//			//				}
//			//				PrismObjectDefinition<T> def = schema.findObjectDefinitionByTypeAssumeNs(typeName);
//			//				if (def != null){
//			//					return def;
//			//				}
//			//
//			//			}
//			return null;
//		}
//		return (PrismObjectDefinition) schema.findObjectDefinitionByType(typeName);
//	}
//
//	@Override
//	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByElementName(@NotNull QName elementName) {
//		if (StringUtils.isEmpty(elementName.getNamespaceURI())) {
//			return resolveGlobalItemDefinitionWithoutNamespace(elementName.getLocalPart(), PrismObjectDefinition.class);
//		}
//		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
//		if (schema == null) {
//			return null;
//		}
//		return (PrismObjectDefinition) schema.findObjectDefinitionByElementName(elementName);
//	}
//
//	@Override
//	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByType(QName typeName) {
//		if (StringUtils.isEmpty(typeName.getNamespaceURI())) {
//			// Maybe not optimal but sufficient way: we resolve complex type definition, and from it we get qualified type name.
//			// This is then used to find container definition in the traditional way.
//			ComplexTypeDefinition complexTypeDefinition = resolveGlobalTypeDefinitionWithoutNamespace(typeName.getLocalPart());
//			if (complexTypeDefinition == null) {
//				return null;
//			}
//			typeName = complexTypeDefinition.getTypeName();
//		}
//		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
//		if (schema == null) {
//			return null;
//		}
//		return schema.findContainerDefinitionByType(typeName);
//	}
//
//	@Override
//	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByElementName(QName elementName) {
//		if (StringUtils.isEmpty(elementName.getNamespaceURI())) {
//			return resolveGlobalItemDefinitionWithoutNamespace(elementName.getLocalPart(), PrismContainerDefinition.class);
//		}
//		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
//		if (schema == null) {
//			return null;
//		}
//		return schema.findContainerDefinitionByElementName(elementName);
//	}


	//
	@NotNull
	@Override
	public <ID extends ItemDefinition> List<ID> findItemDefinitionsByCompileTimeClass(
			@NotNull Class<?> compileTimeClass, @NotNull Class<ID> definitionClass) {
		PrismSchema schema = findSchemaByCompileTimeClass(compileTimeClass);
		if (schema == null) {
			return Collections.emptyList();
		}
		@SuppressWarnings("unchecked")
		List<ID> list = schema.findItemDefinitionsByCompileTimeClass(compileTimeClass, definitionClass);
		return list;
	}

	@Nullable
	@Override
	public <ID extends ItemDefinition> ID findItemDefinitionByType(@NotNull QName typeName, @NotNull Class<ID> definitionClass) {
		if (QNameUtil.noNamespace(typeName)) {
			TypeDefinition td = resolveGlobalTypeDefinitionWithoutNamespace(typeName.getLocalPart(), TypeDefinition.class);
			if (td == null) {
				return null;
			}
			typeName = td.getTypeName();
		}
		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findItemDefinitionByType(typeName, definitionClass);
	}

	@NotNull
	@Override
	public <ID extends ItemDefinition> List<ID> findItemDefinitionsByElementName(@NotNull QName elementName, @NotNull Class<ID> definitionClass) {
		if (QNameUtil.noNamespace(elementName)) {
			return resolveGlobalItemDefinitionsWithoutNamespace(elementName.getLocalPart(), definitionClass);
		} else {
			PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
			if (schema == null) {
				return new ArrayList<>();
			}
			return schema.findItemDefinitionsByElementName(elementName, definitionClass);
		}
	}

	@Nullable
	@Override
	public <TD extends TypeDefinition> TD findTypeDefinitionByCompileTimeClass(@NotNull Class<?> compileTimeClass, @NotNull Class<TD> definitionClass) {
		PrismSchema schema = findSchemaByCompileTimeClass(compileTimeClass);
		if (schema == null) {
			return null;
		}
		return schema.findTypeDefinitionByCompileTimeClass(compileTimeClass, definitionClass);
	}

	@Nullable
	@Override
	public <TD extends TypeDefinition> TD findTypeDefinitionByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass) {
		if (QNameUtil.noNamespace(typeName)) {
			return resolveGlobalTypeDefinitionWithoutNamespace(typeName.getLocalPart(), definitionClass);
		}
		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findTypeDefinitionByType(typeName, definitionClass);
	}

	@NotNull
	@Override
	public <TD extends TypeDefinition> Collection<? extends TD> findTypeDefinitionsByType(@NotNull QName typeName,
			@NotNull Class<TD> definitionClass) {
		if (QNameUtil.noNamespace(typeName)) {
			return resolveGlobalTypeDefinitionsWithoutNamespace(typeName.getLocalPart(), definitionClass);
		}
		PrismSchema schema = findSchemaByNamespace(typeName.getNamespaceURI());
		if (schema == null) {
			return Collections.emptyList();
		}
		return schema.findTypeDefinitionsByType(typeName, definitionClass);
	}

	@NotNull
	@Override
	public <TD extends TypeDefinition> Collection<TD> findTypeDefinitionsByElementName(@NotNull QName name, @NotNull Class<TD> clazz) {
		return findItemDefinitionsByElementName(name, ItemDefinition.class).stream()
				.flatMap(itemDef -> findTypeDefinitionsByType(itemDef.getTypeName(), clazz).stream())
				.collect(Collectors.toList());
	}

	//endregion

	//region Finding items (nonstandard cases)
	@Override
	public <T extends ItemDefinition> T findItemDefinitionByFullPath(Class<? extends Objectable> objectClass, Class<T> defClass,
			QName... itemNames)
			throws SchemaException {
		PrismObjectDefinition objectDefinition = findObjectDefinitionByCompileTimeClass(objectClass);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for " + objectClass);
		}
		return (T) objectDefinition.findItemDefinition(new ItemPath(itemNames), defClass);
	}

	@Override
	public ItemDefinition findItemDefinitionByElementName(QName elementName, @Nullable List<String> ignoredNamespaces) {
		if (StringUtils.isEmpty(elementName.getNamespaceURI())) {
			return resolveGlobalItemDefinitionWithoutNamespace(elementName.getLocalPart(), ItemDefinition.class, true, ignoredNamespaces);
		}
		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findItemDefinitionByElementName(elementName, ItemDefinition.class);
	}

	@Override
	public <T> Class<T> determineCompileTimeClass(QName typeName) {
		if (QNameUtil.noNamespace(typeName)) {
			TypeDefinition td = resolveGlobalTypeDefinitionWithoutNamespace(typeName.getLocalPart(), TypeDefinition.class);
			if (td == null) {
				return null;
			}
			return (Class<T>) td.getCompileTimeClass();
		}
		SchemaDescription desc = findSchemaDescriptionByNamespace(typeName.getNamespaceURI());
		if (desc == null) {
			return null;
		}
		Package pkg = desc.getCompileTimeClassesPackage();
		if (pkg == null) {
			return null;
		}
		return JAXBUtil.findClassForType(typeName, pkg);
	}

	@NotNull
	public <T> Class<T> determineCompileTimeClassNotNull(QName typeName) {
		Class<T> clazz = determineCompileTimeClass(typeName);
		if (clazz != null) {
			return clazz;
		} else {
			throw new IllegalStateException("No class for " + typeName);
		}
	}

	@Override
	public <T> Class<T> getCompileTimeClass(QName xsdType) {
		return determineCompileTimeClass(xsdType);
		// TODO: which one is better (this one or the above)?
		//        SchemaDescription desc = findSchemaDescriptionByNamespace(xsdType.getNamespaceURI());
		//        if (desc == null) {
		//            return null;
		//        }
		//        Map<QName, Class<?>> map = desc.getXsdTypeTocompileTimeClassMap();
		//        if (map == null) {
		//            return null;
		//        }
		//        return (Class<T>) map.get(xsdType);
	}

	@Override
	public Class<? extends ObjectType> getCompileTimeClassForObjectType(QName objectType) {
		PrismObjectDefinition definition = findObjectDefinitionByType(objectType);
		if (definition == null) {
			return null;
		}
		return definition.getCompileTimeClass();
	}

	@Override
	public PrismObjectDefinition determineDefinitionFromClass(Class compileTimeClass) {
		PrismObjectDefinition def = findObjectDefinitionByCompileTimeClass(compileTimeClass);
		if (def != null) {
			return def;
		}
		Class<?> superclass = compileTimeClass.getSuperclass();
		if (superclass == null || superclass == Object.class) {
			return null;
		}
		return determineDefinitionFromClass(superclass);
	}

	@Override
	public <T extends Containerable> ItemDefinition locateItemDefinition(@NotNull QName itemName,
			@Nullable ComplexTypeDefinition complexTypeDefinition,
			@Nullable Function<QName, ItemDefinition> dynamicDefinitionResolver) throws SchemaException {
		ItemDefinition def;
		if (complexTypeDefinition != null) {
			def = complexTypeDefinition.findItemDefinition(itemName);
			if (def != null) {
				return def;
			}
		}
		// not sure about this: shouldn't extension schemas have xsdAnyMarker set?
		if (complexTypeDefinition == null || complexTypeDefinition.isXsdAnyMarker() || complexTypeDefinition.getExtensionForType() != null) {
			def = resolveGlobalItemDefinition(itemName, complexTypeDefinition);
			if (def != null) {
				return def;
			}
		}
		if (dynamicDefinitionResolver != null) {
			def = dynamicDefinitionResolver.apply(itemName);
			if (def != null) {
				return def;
			}
		}
		return null;
	}
	//endregion

	//region Unqualified names resolution
	// TODO fix this temporary and inefficient implementation
	@Override
	public QName resolveUnqualifiedTypeName(QName type) throws SchemaException {
		QName typeFound = null;
		for (SchemaDescription desc: schemaDescriptions) {
			QName typeInSchema = new QName(desc.getNamespace(), type.getLocalPart());
			if (desc.getSchema() != null && desc.getSchema().findComplexTypeDefinition(typeInSchema) != null) {
				if (typeFound != null) {
					throw new SchemaException("Ambiguous type name: " + type);
				} else {
					typeFound = typeInSchema;
				}
			}
		}
		if (typeFound == null) {
			throw new SchemaException("Unknown type: " + type);
		} else {
			return typeFound;
		}
	}

	@Override
	public QName qualifyTypeName(QName typeName) throws SchemaException {
		if (typeName == null || !QNameUtil.isUnqualified(typeName)) {
			return typeName;
		}
		return resolveUnqualifiedTypeName(typeName);
	}

//	private class ParentChildPair {
//		final ComplexTypeDefinition parentDef;
//		final ItemDefinition childDef;
//		public ParentChildPair(ComplexTypeDefinition parentDef, ItemDefinition childDef) {
//			this.parentDef = parentDef;
//			this.childDef = childDef;
//		}
//	}

	// current implementation tries to find all references to the child CTD and select those that are able to resolve path of 'rest'
	// fails on ambiguity
	// it's a bit fragile, as adding new references to child CTD in future may break existing code
	@Override
	public ComplexTypeDefinition determineParentDefinition(@NotNull ComplexTypeDefinition child, @NotNull ItemPath rest) {
		Map<ComplexTypeDefinition, ItemDefinition> found = new HashMap<>();
		for (PrismSchema schema : getSchemas()) {
			if (schema == null) {
				continue;
			}
			for (ComplexTypeDefinition ctd : schema.getComplexTypeDefinitions()) {
				for (ItemDefinition item : ctd.getDefinitions()) {
					if (!(item instanceof PrismContainerDefinition)) {
						continue;
					}
					PrismContainerDefinition<?> itemPcd = (PrismContainerDefinition<?>) item;
					if (itemPcd.getComplexTypeDefinition() == null) {
						continue;
					}
					if (child.getTypeName().equals(itemPcd.getComplexTypeDefinition().getTypeName())) {
						if (!rest.isEmpty() && ctd.findItemDefinition(rest) == null) {
							continue;
						}
						found.put(ctd, itemPcd);
					}
				}
			}
		}
		if (found.isEmpty()) {
			throw new IllegalStateException("Couldn't find definition for parent for " + child.getTypeName() + ", path=" + rest);
		} else if (found.size() > 1) {
			Map<ComplexTypeDefinition, ItemDefinition> notInherited = found.entrySet().stream()
					.filter(e -> !e.getValue().isInherited())
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
			if (notInherited.isEmpty()) {
				throw new IllegalStateException(
						"Couldn't find parent definition for " + child.getTypeName() + ": More than one candidate found: "
								+ notInherited);
			} else if (notInherited.isEmpty()) {
				throw new IllegalStateException(
						"Couldn't find parent definition for " + child.getTypeName() + ": More than one candidate found - and all are inherited: "
								+ found);
			} else {
				return notInherited.keySet().iterator().next();
			}
		} else {
			return found.keySet().iterator().next();
		}
	}

	@Override
	public PrismObjectDefinition determineReferencedObjectDefinition(QName targetTypeName, ItemPath rest) {
		// TEMPORARY HACK -- TODO FIXME
		PrismObjectDefinition def = findObjectDefinitionByType(targetTypeName);
		if (def == null) {
			throw new IllegalStateException("Couldn't find definition for referenced object for " + targetTypeName + ", path=" + rest);
		}
		return def;
	}

	private <TD extends TypeDefinition> TD resolveGlobalTypeDefinitionWithoutNamespace(String typeLocalName, Class<TD> definitionClass) {
		TD found = null;
		for (SchemaDescription schemaDescription : parsedSchemas.values()) {
			PrismSchema schema = schemaDescription.getSchema();
			if (schema == null) {       // is this possible?
				continue;
			}
			TD def = schema.findTypeDefinitionByType(new QName(schema.getNamespace(), typeLocalName), definitionClass);
			if (def != null) {
				if (found != null) {
					throw new IllegalArgumentException("Multiple possible resolutions for unqualified type name " + typeLocalName + " (e.g. in " +
							def.getTypeName() + " and " + found.getTypeName());
				}
				found = def;
			}
		}
		return found;
	}

	@NotNull
	private <TD extends TypeDefinition> Collection<TD> resolveGlobalTypeDefinitionsWithoutNamespace(String typeLocalName, Class<TD> definitionClass) {
		List<TD> rv = new ArrayList<>();
		for (SchemaDescription schemaDescription : parsedSchemas.values()) {
			PrismSchema schema = schemaDescription.getSchema();
			if (schema != null) {
				rv.addAll(schema.findTypeDefinitionsByType(new QName(schema.getNamespace(), typeLocalName), definitionClass));
			}
		}
		return rv;
	}

	/**
	 * Looks for a top-level definition for the specified element name (in all schemas).
	 */
	@Override
	public ItemDefinition resolveGlobalItemDefinition(QName elementQName) throws SchemaException {
		return resolveGlobalItemDefinition(elementQName, (ComplexTypeDefinition) null);
	}

	@Override
	public ItemDefinition resolveGlobalItemDefinition(QName elementQName, PrismContainerDefinition<?> containerDefinition) throws SchemaException {
		return resolveGlobalItemDefinition(elementQName, containerDefinition != null ? containerDefinition.getComplexTypeDefinition() : null);
	}

	@Override
	public ItemDefinition resolveGlobalItemDefinition(QName itemName, @Nullable ComplexTypeDefinition complexTypeDefinition) throws SchemaException {
		if (QNameUtil.noNamespace(itemName)) {
			if (complexTypeDefinition != null && complexTypeDefinition.getDefaultNamespace() != null) {
				itemName = new QName(complexTypeDefinition.getDefaultNamespace(), itemName.getLocalPart());
			}
			else {
				List<String> ignoredNamespaces = complexTypeDefinition != null ?
						complexTypeDefinition.getIgnoredNamespaces() :
						null;
				return resolveGlobalItemDefinitionWithoutNamespace(itemName.getLocalPart(), ItemDefinition.class, true, ignoredNamespaces);
			}
		}
		PrismSchema schema = findSchemaByNamespace(itemName.getNamespaceURI());
		if (schema == null) {
			return null;
		}
		return schema.findItemDefinitionByElementName(itemName, ItemDefinition.class);
	}

//	private <T extends ItemDefinition> T resolveGlobalItemDefinitionWithoutNamespace(String localPart, Class<T> definitionClass) {
//		return resolveGlobalItemDefinitionWithoutNamespace(localPart, definitionClass, true, null);
//	}

	private <ID extends ItemDefinition> List<ID> resolveGlobalItemDefinitionsWithoutNamespace(String localPart, Class<ID> definitionClass) {
		return resolveGlobalItemDefinitionsWithoutNamespace(localPart, definitionClass, null);
	}

	private <ID extends ItemDefinition> ID resolveGlobalItemDefinitionWithoutNamespace(String localPart, Class<ID> definitionClass, boolean exceptionIfAmbiguous, @Nullable List<String> ignoredNamespaces) {
		return DefinitionStoreUtils.getOne(
				resolveGlobalItemDefinitionsWithoutNamespace(localPart, definitionClass, ignoredNamespaces),
				exceptionIfAmbiguous,
				"Multiple possible resolutions for unqualified element name '" + localPart + "'");
	}

	@NotNull
	private <ID extends ItemDefinition> List<ID> resolveGlobalItemDefinitionsWithoutNamespace(String localPart, Class<ID> definitionClass, @Nullable List<String> ignoredNamespaces) {
		List<ID> found = new ArrayList<ID>();
		for (SchemaDescription schemaDescription : parsedSchemas.values()) {
			PrismSchema schema = schemaDescription.getSchema();
			if (schema == null) {       // is this possible?
				continue;
			}
			if (namespaceMatches(schema.getNamespace(), ignoredNamespaces)) {
				continue;
			}
			ItemDefinition def = schema.findItemDefinitionByElementName(new QName(localPart), definitionClass);
			if (def != null) {
				found.add((ID) def);
			}
		}
		return found;
	}


//	private QName resolveElementNameIfNeeded(QName elementName) {
//		return resolveElementNameIfNeeded(elementName, true);
//	}

//	private QName resolveElementNameIfNeeded(QName elementName, boolean exceptionIfAmbiguous) {
//		if (StringUtils.isNotEmpty(elementName.getNamespaceURI())) {
//			return elementName;
//		}
//		ItemDefinition itemDef = resolveGlobalItemDefinitionWithoutNamespace(elementName.getLocalPart(), ItemDefinition.class, exceptionIfAmbiguous, null);
//		if (itemDef != null) {
//			return itemDef.getName();
//		} else {
//			return null;
//		}
//	}

	//endregion

	//region Finding schemas

	@Override
	public PrismSchema getPrismSchema(String namespace) {
		List<PrismSchema> schemas = parsedSchemas.get(namespace).stream()
				.filter(s -> s.getSchema() != null)
				.map(s -> s.getSchema())
				.collect(Collectors.toList());
		if (schemas.size() > 1) {
			throw new IllegalStateException("More than one prism schema for namespace " + namespace);
		} else if (schemas.size() == 1) {
			return schemas.get(0);
		} else {
			return null;
		}
	}

	@Override
	public Collection<PrismSchema> getSchemas() {
		return parsedSchemas.values().stream()
				.filter(s -> s.getSchema() != null)
				.map(s -> s.getSchema())
				.collect(Collectors.toList());
	}

	@Override
	public Collection<SchemaDescription> getSchemaDescriptions() {
		return parsedSchemas.values();
	}


	@Override
	public PrismSchema findSchemaByCompileTimeClass(@NotNull Class<?> compileTimeClass) {
		Package compileTimePackage = compileTimeClass.getPackage();
		if (compileTimePackage == null) {
			return null;			// e.g. for arrays
		}
		for (SchemaDescription desc: schemaDescriptions) {
			if (compileTimePackage.equals(desc.getCompileTimeClassesPackage())) {
				return desc.getSchema();
			}
		}
		return null;
	}

	@Override
	public PrismSchema findSchemaByNamespace(String namespaceURI) {
		SchemaDescription desc = findSchemaDescriptionByNamespace(namespaceURI);
		if (desc == null) {
			return null;
		}
		return desc.getSchema();
	}

	@Override
	public SchemaDescription findSchemaDescriptionByNamespace(String namespaceURI) {
		for (SchemaDescription desc: schemaDescriptions) {
			if (namespaceURI.equals(desc.getNamespace())) {
				return desc;
			}
		}
		return null;
	}

	@Override
	public PrismSchema findSchemaByPrefix(String prefix) {
		SchemaDescription desc = findSchemaDescriptionByPrefix(prefix);
		if (desc == null) {
			return null;
		}
		return desc.getSchema();
	}

	@Override
	public SchemaDescription findSchemaDescriptionByPrefix(String prefix) {
		for (SchemaDescription desc: schemaDescriptions) {
			if (prefix.equals(desc.getUsualPrefix())) {
				return desc;
			}
		}
		return null;
	}

	//endregion

	//region Misc
	public static ItemDefinition createDefaultItemDefinition(QName itemName, PrismContext prismContext) {
		PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(itemName, DEFAULT_XSD_TYPE, prismContext);
		// Set it to multi-value to be on the safe side
		propDef.setMaxOccurs(-1);
		propDef.setDynamic(true);
		return propDef;
	}

	//endregion

	//region Deprecated misc things
	@Override
	@Deprecated
	public <T extends Objectable> PrismObject<T> instantiate(Class<T> compileTimeClass) throws SchemaException {
		return prismContext.createObject(compileTimeClass);
	}
	//endregion

	//region TODO categorize
	/**
	 * Returns true if specified element has a definition that matches specified type
	 * in the known schemas.
	 */
//	@Override
//	public boolean hasImplicitTypeDefinitionOld(QName elementName, QName typeName) {
//		elementName = resolveElementNameIfNeeded(elementName, false);
//		if (elementName == null) {
//			return false;
//		}
//		PrismSchema schema = findSchemaByNamespace(elementName.getNamespaceURI());
//		if (schema == null) {
//			return false;
//		}
//		ItemDefinition itemDefinition = schema.findItemDefinitionByElementName(elementName, ItemDefinition.class);
//		if (itemDefinition == null) {
//			return false;
//		}
//		return QNameUtil.match(typeName, itemDefinition.getTypeName());
//	}

	/**
	 * Answers the question: "If the receiver would get itemName without any other information, will it be able to
	 * derive suitable typeName from it?" If not, we have to provide explicit type definition for serialization.
	 *
	 * By suitable we mean such that can be used to determine specific object type.
	 */
	public boolean hasImplicitTypeDefinition(@NotNull QName itemName, @NotNull QName typeName) {
		List<ItemDefinition> definitions = findItemDefinitionsByElementName(itemName, ItemDefinition.class);
		if (definitions.isEmpty() || definitions.size() > 1) {
			return false;
		}
		ItemDefinition definition = definitions.get(0);
		if (definition.isAbstract()) {
			return false;
		}
		// TODO other conditions?
		return definition.getTypeName().equals(typeName);
	}


	@Override
	public QName determineTypeForClass(Class<?> clazz) {
		if (XmlTypeConverter.canConvert(clazz)) {
			return XsdTypeMapper.toXsdType(clazz);
		} else {
			return ((PrismContextImpl) prismContext).getBeanMarshaller().determineTypeForClass(clazz);
		}
	}

	@Override
	public <T> Class<T> determineClassForType(QName type) {
		if (XmlTypeConverter.canConvert(type)) {
			return XsdTypeMapper.toJavaType(type);
		} else {
			return determineCompileTimeClass(type);
		}
	}

	@NotNull
	public <T> Class<T> determineClassForTypeNotNull(QName typeName) {
		Class<T> clazz = determineClassForType(typeName);
		if (clazz != null) {
			return clazz;
		} else {
			throw new IllegalStateException("No class for " + typeName);
		}
	}

	@Override
	public Class<?> determineClassForItemDefinition(ItemDefinition<?> itemDefinition) {
		if (itemDefinition instanceof PrismContainerDefinition) {
			Class<?> cls = ((PrismContainerDefinition) itemDefinition).getCompileTimeClass();
			if (cls != null) {
				return cls;
			}
		}
		return determineClassForType(itemDefinition.getTypeName());
	}

	@Override
	public <ID extends ItemDefinition> ID selectMoreSpecific(ID def1, ID def2)
			throws SchemaException {
		if (def1 == null) {
			return def2;
		}
		if (def2 == null) {
			return def1;
		}
		if (QNameUtil.match(def1.getTypeName(), def2.getTypeName())) {
			return def1;
		}
		Class<?> cls1 = determineClassForItemDefinition(def1);
		Class<?> cls2 = determineClassForItemDefinition(def2);
		if (cls1 == null || cls2 == null) {
			throw new SchemaException("Couldn't find more specific type from " + def1.getTypeName()
					+ " (" + cls1 + ") and " + def2.getTypeName() + " (" + cls2 + ")");
		}
		if (cls1.isAssignableFrom(cls2)) {
			return def2;
		}
		if (cls2.isAssignableFrom(cls1)) {
			return def1;
		}
		throw new SchemaException("Couldn't find more specific type from " + def1.getTypeName()
				+ " (" + cls1 + ") and " + def2.getTypeName() + " (" + cls2 + ")");
	}

	@Override
	public QName selectMoreSpecific(QName type1, QName type2)
			throws SchemaException {
		if (type1 == null || QNameUtil.match(type1, DOMUtil.XSD_ANYTYPE)) {
			return type2;
		}
		if (type2 == null || QNameUtil.match(type2, DOMUtil.XSD_ANYTYPE)) {
			return type1;
		}
		if (QNameUtil.match(type1, type2)) {
			return type1;
		}
		Class<?> cls1 = determineClassForType(type1);
		Class<?> cls2 = determineClassForType(type2);
		if (cls1 == null || cls2 == null) {
			throw new SchemaException("Couldn't find more specific type from " + type1
					+ " (" + cls1 + ") and " + type2 + " (" + cls2 + ")");
		}
		if (cls1.isAssignableFrom(cls2)) {
			return type2;
		}
		if (cls2.isAssignableFrom(cls1)) {
			return type1;
		}
		// poly string vs string
		if (PolyStringType.class.equals(cls1) || String.class.equals(cls2)) {
			return type1;
		}
		if (PolyStringType.class.equals(cls2) || String.class.equals(cls1)) {
			return type2;
		}
		throw new SchemaException("Couldn't find more specific type from " + type1
				+ " (" + cls1 + ") and " + type2 + " (" + cls2 + ")");
	}

	// TODO implement more efficiently
	@Override
	public boolean areComparable(QName type1, QName type2) throws SchemaException {
		try {
			selectMoreSpecific(type1, type2);
			return true;
		} catch (SchemaException e) {
			return false;
		}
	}

	@Override
	public <ID extends ItemDefinition> ComparisonResult compareDefinitions(@NotNull ID def1, @NotNull ID def2)
			throws SchemaException {
		if (QNameUtil.match(def1.getTypeName(), def2.getTypeName())) {
			return ComparisonResult.EQUAL;
		}
		Class<?> cls1 = determineClassForItemDefinition(def1);
		Class<?> cls2 = determineClassForItemDefinition(def2);
		if (cls1 == null || cls2 == null) {
			return ComparisonResult.NO_STATIC_CLASS;
		}
		boolean cls1AboveOrEqualCls2 = cls1.isAssignableFrom(cls2);
		boolean cls2AboveOrEqualCls1 = cls2.isAssignableFrom(cls1);
		if (cls1AboveOrEqualCls2 && cls2AboveOrEqualCls1) {
			return ComparisonResult.EQUAL;
		} else if (cls1AboveOrEqualCls2) {
			return ComparisonResult.SECOND_IS_CHILD;
		} else if (cls2AboveOrEqualCls1) {
			return ComparisonResult.FIRST_IS_CHILD;
		} else {
			return ComparisonResult.INCOMPATIBLE;
		}
	}

	@Override
	public boolean isAssignableFrom(@NotNull QName superType, @NotNull QName subType) {
		if (QNameUtil.match(superType, subType) || QNameUtil.match(DOMUtil.XSD_ANYTYPE, superType)) {
			return true;
		}
		if (QNameUtil.match(DOMUtil.XSD_ANYTYPE, subType)) {
			return false;
		}
		Class<?> superClass = determineClassForType(superType);
		Class<?> subClass = determineClassForType(subType);
		// TODO consider implementing "strict mode" that would throw an exception in the case of nullness
		return superClass != null && subClass != null && superClass.isAssignableFrom(subClass);
	}

	@Override
	public boolean isContainer(QName typeName) {
		Class<?> clazz = determineClassForType(typeName);
		return clazz != null && Containerable.class.isAssignableFrom(clazz);
	}

	//endregion
}
