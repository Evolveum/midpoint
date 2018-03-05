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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Radovan Semancik
 *
 */
public class PrismSchemaImpl implements PrismSchema {

	//private static final long serialVersionUID = 5068618465625931984L;

	//private static final Trace LOGGER = TraceManager.getTrace(PrismSchema.class);

	@NotNull protected final Collection<Definition> definitions = new ArrayList<>();
	@NotNull private final Map<QName, ItemDefinition<?>> itemDefinitionMap = new HashMap<>();		// key is the item name (qualified or unqualified)
	protected String namespace;			// may be null if not properly initialized
	protected PrismContext prismContext;

	// Item definitions that couldn't be created when parsing the schema because of unresolvable CTD.
	// (Caused by the fact that the type resides in another schema.)
	// These definitions are to be resolved after parsing the set of schemas.
	@NotNull private final List<DefinitionSupplier> delayedItemDefinitions = new ArrayList<>();

	protected PrismSchemaImpl(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public PrismSchemaImpl(@NotNull String namespace, PrismContext prismContext) {
		if (StringUtils.isEmpty(namespace)) {
			throw new IllegalArgumentException("Namespace can't be null or empty.");
		}
		this.namespace = namespace;
		this.prismContext = prismContext;
	}

	//region Trivia
	@Override
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(@NotNull String namespace) {
		this.namespace = namespace;
	}

	@NotNull
	@Override
	public Collection<Definition> getDefinitions() {
		return Collections.unmodifiableCollection(definitions);
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public <T extends Definition> List<T> getDefinitions(@NotNull Class<T> type) {
		return definitions.stream()
				.filter(def -> type.isAssignableFrom(def.getClass()))
				.map(def -> (T) def)
				.collect(Collectors.toList());
	}

	void addDelayedItemDefinition(DefinitionSupplier supplier) {
		delayedItemDefinitions.add(supplier);
	}

	@NotNull
	List<DefinitionSupplier> getDelayedItemDefinitions() {
		return delayedItemDefinitions;
	}

	@Override
	public boolean isEmpty() {
		return definitions.isEmpty();
	}

	public void add(@NotNull Definition def) {
		definitions.add(def);
		if (def instanceof ItemDefinition) {
			ItemDefinition<?> itemDef = (ItemDefinition<?>) def;
			itemDefinitionMap.put(itemDef.getName(), itemDef);
		}
	}

	@Override
	public PrismContext getPrismContext() {
		return prismContext;
	}
	//endregion

	//region XSD parsing and serialization
	// TODO: cleanup this chaos
	// used for report, connector, resource schemas
	public static PrismSchema parse(Element element, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException {
		return parse(element, prismContext.getEntityResolver(), new PrismSchemaImpl(prismContext), isRuntime, shortDescription,
				false, prismContext);
	}

	// used for parsing prism schemas; only in exceptional cases
	public static PrismSchema parse(Element element, EntityResolver resolver, boolean isRuntime, String shortDescription,
			boolean allowDelayedItemDefinitions, PrismContext prismContext) throws SchemaException {
		return parse(element, resolver, new PrismSchemaImpl(prismContext), isRuntime, shortDescription, allowDelayedItemDefinitions, prismContext);
	}

	// main entry point for parsing standard prism schemas
	public static void parseSchemas(Element wrapperElement, XmlEntityResolver resolver,
			List<SchemaDescription> schemaDescriptions,
			boolean allowDelayedItemDefinitions, PrismContext prismContext) throws SchemaException {

		for (SchemaDescription schemaDescription : schemaDescriptions) {
			setSchemaNamespace((PrismSchemaImpl) schemaDescription.getSchema(), schemaDescription.getDomElement());
		}
		DomToSchemaProcessor processor = new DomToSchemaProcessor(resolver, prismContext);
		processor.parseSchemas(schemaDescriptions, wrapperElement, allowDelayedItemDefinitions, "multiple schemas");
	}

	// used for connector and resource schemas
	protected static PrismSchema parse(Element element, PrismSchemaImpl schema, boolean isRuntime, String shortDescription, PrismContext prismContext) throws SchemaException {
		return parse(element, prismContext.getEntityResolver(), schema, isRuntime, shortDescription, false, prismContext);
	}

	private static PrismSchema parse(Element element, EntityResolver resolver, PrismSchemaImpl schema, boolean isRuntime,
			String shortDescription, boolean allowDelayedItemDefinitions, PrismContext prismContext) throws SchemaException {
		if (element == null) {
			throw new IllegalArgumentException("Schema element must not be null in "+shortDescription);
		}
		setSchemaNamespace(schema, element);

		DomToSchemaProcessor processor = new DomToSchemaProcessor(resolver, prismContext);
		processor.parseSchema(schema, element, isRuntime, allowDelayedItemDefinitions, shortDescription);
		return schema;
	}

	private static void setSchemaNamespace(PrismSchemaImpl prismSchema, Element xsdSchema) throws SchemaException {
		String targetNamespace = DOMUtil.getAttribute(xsdSchema, DOMUtil.XSD_ATTR_TARGET_NAMESPACE);
		if (StringUtils.isEmpty(targetNamespace)) {
			throw new SchemaException("Schema does not have targetNamespace specification");
		}
		prismSchema.setNamespace(targetNamespace);
	}

	@NotNull
	@Override
	public Document serializeToXsd() throws SchemaException {
		SchemaToDomProcessor processor = new SchemaToDomProcessor();
		processor.setPrismContext(prismContext);
		return processor.parseSchema(this);
	}
	//endregion

	//region Creating definitions
	/**
	 * Creates a new property container definition and adds it to the schema.
	 *
	 * This is a preferred way how to create definition in the schema.
	 *
	 * @param localTypeName
	 *            type name "relative" to schema namespace
	 * @return new property container definition
	 */
	public PrismContainerDefinitionImpl createPropertyContainerDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		QName name = new QName(getNamespace(), toElementName(localTypeName));
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinitionImpl(typeName, prismContext);
		PrismContainerDefinitionImpl def = new PrismContainerDefinitionImpl(name, cTypeDef, prismContext);
		add(cTypeDef);
		add(def);
		return def;
	}

	public PrismContainerDefinitionImpl createPropertyContainerDefinition(String localElementName, String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		QName name = new QName(getNamespace(), localElementName);
		ComplexTypeDefinition cTypeDef = findComplexTypeDefinitionByType(typeName);
		if (cTypeDef == null) {
			cTypeDef = new ComplexTypeDefinitionImpl(typeName, prismContext);
			add(cTypeDef);
		}
		PrismContainerDefinitionImpl def = new PrismContainerDefinitionImpl(name, cTypeDef, prismContext);
		add(def);
		return def;
	}

	public ComplexTypeDefinition createComplexTypeDefinition(QName typeName) {
		ComplexTypeDefinition cTypeDef = new ComplexTypeDefinitionImpl(typeName, prismContext);
		add(cTypeDef);
		return cTypeDef;
	}

	/**
	 * Creates a top-level property definition and adds it to the schema.
	 *
	 * This is a preferred way how to create definition in the schema.
	 *
	 * @param localName
	 *            element name "relative" to schema namespace
	 * @param typeName
	 *            XSD type name of the element
	 * @return new property definition
	 */
	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getNamespace(), localName);
		return createPropertyDefinition(name, typeName);
	}

	/*
	 * Creates a top-level property definition and adds it to the schema.
	 *
	 * This is a preferred way how to create definition in the schema.
	 *
	 * @param localName
	 *            element name "relative" to schema namespace
	 * @param localTypeName
	 *            XSD type name "relative" to schema namespace
	 * @return new property definition
	 */
//	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
//		QName name = new QName(getNamespace(), localName);
//		QName typeName = new QName(getNamespace(), localTypeName);
//		return createPropertyDefinition(name, typeName);
//	}

	/**
	 * Creates a top-level property definition and adds it to the schema.
	 *
	 * This is a preferred way how to create definition in the schema.
	 *
	 * @param name
	 *            element name
	 * @param typeName
	 *            XSD type name of the element
	 * @return new property definition
	 */
	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		PrismPropertyDefinition def = new PrismPropertyDefinitionImpl(name, typeName, prismContext);
		add(def);
		return def;
	}

	/**
	 * Internal method to create a "nice" element name from the type name.
	 */
	private String toElementName(String localTypeName) {
		String elementName = StringUtils.uncapitalize(localTypeName);
		if (elementName.endsWith("Type")) {
			return elementName.substring(0, elementName.length() - 4);
		}
		return elementName;
	}
	//endregion

	//region Pretty printing
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		IdentityHashMap<Definition, Object> seen = new IdentityHashMap<>();
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(toString()).append("\n");
		Iterator<Definition> i = definitions.iterator();
		while (i.hasNext()) {
			Definition def = i.next();
			sb.append(def.debugDump(indent+1, seen));
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Schema(ns=" + namespace + ")";
	}
	//endregion

	//region Implementation of DefinitionsStore methods (commented out)
//	private final SearchContexts searchContexts = new SearchContexts(this);			// pre-existing object to avoid creating them each time
//
//	@Override
//	public GlobalDefinitionSearchContext<PrismObjectDefinition<? extends Objectable>> findObjectDefinition() {
//		return searchContexts.pod;
//	}
//
//	@Override
//	public GlobalDefinitionSearchContext<PrismContainerDefinition<? extends Containerable>> findContainerDefinition() {
//		return searchContexts.pcd;
//	}
//
//	@Override
//	public GlobalDefinitionSearchContext<ItemDefinition<?>> findItemDefinition() {
//		return searchContexts.id;
//	}
//
//	@Override
//	public GlobalDefinitionSearchContext<ComplexTypeDefinition> findComplexTypeDefinition() {
//		return searchContexts.ctd;
//	}
//
//	@Override
//	public GlobalDefinitionSearchContext<PrismPropertyDefinition> findPropertyDefinition() {
//		return searchContexts.ppd;
//	}
//
//	@Override
//	public GlobalDefinitionSearchContext<PrismReferenceDefinition> findReferenceDefinition() {
//		return searchContexts.prd;
//	}
	//endregion

	//region Finding definitions

	// items

	@Override
    @NotNull
	public <ID extends ItemDefinition> List<ID> findItemDefinitionsByCompileTimeClass(
			@NotNull Class<?> compileTimeClass, @NotNull Class<ID> definitionClass) {
		List<ID> found = new ArrayList<>();
		for (Definition def: definitions) {
			if (definitionClass.isAssignableFrom(def.getClass())) {
				if (def instanceof PrismContainerDefinition) {
					@SuppressWarnings("unchecked")
					ID contDef = (ID) def;
					if (compileTimeClass.equals(((PrismContainerDefinition) contDef).getCompileTimeClass())) {
						found.add(contDef);
					}
				} else if (def instanceof PrismPropertyDefinition) {
					if (compileTimeClass.equals(prismContext.getSchemaRegistry().determineClassForType(def.getTypeName()))) {
						@SuppressWarnings("unchecked")
						ID itemDef = (ID) def;
						found.add(itemDef);
					}
				} else {
					// skipping the definition (PRD is not supported yet)
				}
			}
		}
		return found;
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinitionByType(@NotNull QName typeName, @NotNull Class<ID> definitionClass) {
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definitionClass.isAssignableFrom(definition.getClass())) {
				@SuppressWarnings("unchecked")
				ID itemDef = (ID) definition;
				if (QNameUtil.match(typeName, itemDef.getTypeName())) {
					return itemDef;
				}
			}
		}
		return null;
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public <ID extends ItemDefinition> List<ID> findItemDefinitionsByElementName(@NotNull QName elementName,
			@NotNull Class<ID> definitionClass) {
		List<Definition> matching = new ArrayList<>();
		CollectionUtils.addIgnoreNull(matching, itemDefinitionMap.get(elementName));
		if (QNameUtil.hasNamespace(elementName)) {
			CollectionUtils.addIgnoreNull(matching, itemDefinitionMap.get(QNameUtil.unqualify(elementName)));
		} else if (namespace != null) {
			CollectionUtils.addIgnoreNull(matching, itemDefinitionMap.get(new QName(namespace, elementName.getLocalPart())));
		}
		return matching.stream()
				.filter(d -> definitionClass.isAssignableFrom(d.getClass()))
				.map(d -> (ID) d)
				.collect(Collectors.toList());
	}

	//	private Map<Class<? extends Objectable>, PrismObjectDefinition> classToDefCache = Collections.synchronizedMap(new HashMap<>());
//	@Override
//	public <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByCompileTimeClass(@NotNull Class<O> type) {
//		if (classToDefCache.containsKey(type)) {		// there may be null values
//			return classToDefCache.get(type);
//		}
//		PrismObjectDefinition<O> definition = scanForPrismObjectDefinition(type);
//		classToDefCache.put(type, definition);
//		return definition;
//	}
//	private <T extends Objectable> PrismObjectDefinition<T> scanForPrismObjectDefinition(Class<T> type) {
//		for (Definition def: getDefinitions()) {
//			if (def instanceof PrismObjectDefinition<?>) {
//				PrismObjectDefinition<?> objDef = (PrismObjectDefinition<?>)def;
//				if (type.equals(objDef.getCompileTimeClass())) {
//					classToDefCache.put(type, objDef);
//					return (PrismObjectDefinition<T>) objDef;
//				}
//			}
//		}
//		return null;
//	}



	@Override
	public <C extends Containerable> ComplexTypeDefinition findComplexTypeDefinitionByCompileTimeClass(@NotNull Class<C> compileTimeClass) {
		for (Definition def: definitions) {
			if (def instanceof ComplexTypeDefinition) {
				ComplexTypeDefinition ctd = (ComplexTypeDefinition) def;
				if (compileTimeClass.equals(ctd.getCompileTimeClass())) {
					return ctd;
				}
			}
		}
		return null;
	}

	@Nullable
	@Override
	public <TD extends TypeDefinition> TD findTypeDefinitionByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass) {
		Collection<TD> definitions = findTypeDefinitionsByType(typeName, definitionClass);
		return !definitions.isEmpty() ?
				definitions.iterator().next() : null;		// TODO treat multiple results somehow
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public <TD extends TypeDefinition> Collection<TD> findTypeDefinitionsByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass) {
		return (List) definitions.stream()
				.filter(def -> definitionClass.isAssignableFrom(def.getClass()) && QNameUtil.match(typeName, def.getTypeName()))
				.collect(Collectors.toList());
	}

	@Nullable
	@Override
	public <TD extends TypeDefinition> TD findTypeDefinitionByCompileTimeClass(@NotNull Class<?> compileTimeClass, @NotNull Class<TD> definitionClass) {
		// TODO: check for multiple definition with the same type
		for (Definition definition : definitions) {
			if (definitionClass.isAssignableFrom(definition.getClass()) && compileTimeClass.equals(((TD) definition).getCompileTimeClass())) {
				return (TD) definition;
			}
		}
		return null;
	}

	//endregion
}
