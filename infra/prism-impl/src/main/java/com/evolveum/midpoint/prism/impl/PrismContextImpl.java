/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.delta.DeltaFactoryImpl;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.impl.crypto.KeyStoreBasedProtectorImpl;
import com.evolveum.midpoint.prism.impl.marshaller.*;
import com.evolveum.midpoint.prism.impl.path.CanonicalItemPathImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.impl.schema.SchemaFactoryImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.marshaller.*;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessorRegistry;
import com.evolveum.midpoint.prism.impl.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.metadata.ValueMetadataFactory;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.impl.polystring.AlphanumericPolyStringNormalizer;
import com.evolveum.midpoint.prism.impl.polystring.ConfigurableNormalizer;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.impl.query.QueryFactoryImpl;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.prism.impl.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.schema.*;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.xnode.XNodeFactory;
import com.evolveum.midpoint.prism.impl.xnode.XNodeFactoryImpl;
import com.evolveum.midpoint.prism.xnode.XNodeMutator;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author semancik
 *
 */
public final class PrismContextImpl implements PrismContext {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContextImpl.class);

    private static boolean allowSchemalessSerialization = true;
    private static boolean extraValidation = false;                                        // TODO replace by something serious

    @NotNull private final SchemaRegistryImpl schemaRegistry;
    @NotNull private final QueryConverterImpl queryConverter;
    @NotNull private final LexicalProcessorRegistry lexicalProcessorRegistry;
    @NotNull private PolyStringNormalizer defaultPolyStringNormalizer;
    @NotNull private final PrismUnmarshaller prismUnmarshaller;
    @NotNull private final PrismMarshaller prismMarshaller;
    @NotNull private final BeanMarshaller beanMarshaller;
    @NotNull private final BeanUnmarshaller beanUnmarshaller;
    @NotNull private final HacksImpl hacks;
    @NotNull private final XNodeFactory xnodeFactory;
    @NotNull private final DeltaFactory deltaFactory;
    @NotNull private final QueryFactory queryFactory;
    @NotNull private final ItemFactory itemFactory;
    @NotNull private final DefinitionFactory definitionFactory;
    @NotNull private final ItemPathParser itemPathParser;
    @NotNull private final SchemaFactory schemaFactory;

    @Experimental private ValueMetadataFactory valueMetadataFactory;
    @Experimental private EquivalenceStrategy provenanceEquivalenceStrategy;

    private ParsingMigrator parsingMigrator;
    private PrismMonitor monitor = null;

    private SchemaDefinitionFactory schemaDefinitionFactory;

    @Autowired private Protector defaultProtector;

    // We need to keep this because of deprecated methods and various hacks
    @NotNull private final JaxbDomHack jaxbDomHack;

    private QName defaultRelation;

    private QName objectsElementName;

    // ugly hack
    private QName defaultReferenceTypeName;

    static {
        PrismPrettyPrinter.initialize();
    }

    //region Standard overhead
    private PrismContextImpl(@NotNull SchemaRegistryImpl schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
        schemaRegistry.setPrismContext(this);
        this.queryConverter = new QueryConverterImpl(this);
        this.lexicalProcessorRegistry = new LexicalProcessorRegistry(schemaRegistry);
        PrismBeanInspector inspector = new PrismBeanInspector(this);
        this.beanMarshaller = new BeanMarshaller(this, inspector);
        this.beanUnmarshaller = new BeanUnmarshaller(this, inspector, beanMarshaller);
        this.prismUnmarshaller = new PrismUnmarshaller(this, beanUnmarshaller, schemaRegistry);
        this.prismMarshaller = new PrismMarshaller(beanMarshaller);
        this.jaxbDomHack = new JaxbDomHackImpl(lexicalProcessorRegistry.domProcessor(), this);
        this.hacks = new HacksImpl(this);
        this.xnodeFactory = new XNodeFactoryImpl();
        this.deltaFactory = new DeltaFactoryImpl(this);
        this.queryFactory = new QueryFactoryImpl(this);
        this.itemFactory = new ItemFactoryImpl(this);
        this.definitionFactory = new DefinitionFactoryImpl(this);
        this.itemPathParser = new ItemPathParserImpl(this);
        this.schemaFactory = new SchemaFactoryImpl(this);
        this.defaultPolyStringNormalizer = new AlphanumericPolyStringNormalizer();

        try {
            configurePolyStringNormalizer(null);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // Should not happen
            throw new SystemException(e.getMessage(), e);
        }
    }

    public static PrismContextImpl create(@NotNull SchemaRegistryImpl schemaRegistry) {
        return new PrismContextImpl(schemaRegistry);
    }

    public static PrismContextImpl createEmptyContext(@NotNull SchemaRegistryImpl schemaRegistry) {
        return new PrismContextImpl(schemaRegistry);
    }

    @Override
    public void initialize() throws SchemaException, SAXException, IOException {
        schemaRegistry.initialize();
    }

    @Override
    public void configurePolyStringNormalizer(PolyStringNormalizerConfigurationType configuration) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (configuration == null) {
            defaultPolyStringNormalizer = new AlphanumericPolyStringNormalizer();
            return;
        }

        String className = configuration.getClassName();
        if (className == null) {
            defaultPolyStringNormalizer = new AlphanumericPolyStringNormalizer();
        } else {
            String fullClassName = getNormalizerFullClassName(className);
            Class<?> normalizerClass;
            try {
                normalizerClass = Class.forName(fullClassName);
            } catch (ClassNotFoundException e) {
                throw new ClassNotFoundException("Cannot find class "+fullClassName+": "+e.getMessage(), e);
            }
            defaultPolyStringNormalizer = (PolyStringNormalizer) normalizerClass.newInstance();
        }

        if (defaultPolyStringNormalizer instanceof ConfigurableNormalizer) {
            ((ConfigurableNormalizer)defaultPolyStringNormalizer).configure(configuration);
        }
    }

    private String getNormalizerFullClassName(String shortClassName) {
        if (shortClassName.contains(".")) {
            return shortClassName;
        }
        return AlphanumericPolyStringNormalizer.class.getPackage().getName() + "." + shortClassName;
    }

    public static boolean isAllowSchemalessSerialization() {
        return allowSchemalessSerialization;
    }

    public static void setAllowSchemalessSerialization(boolean allowSchemalessSerialization) {
        PrismContextImpl.allowSchemalessSerialization = allowSchemalessSerialization;
    }

    public static boolean isExtraValidation() {
        return extraValidation;
    }

    public XmlEntityResolver getEntityResolver() {
        return schemaRegistry.getEntityResolver();
    }

    @NotNull
    @Override
    public SchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }

    /**
     * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
     */
    public PrismUnmarshaller getPrismUnmarshaller() {
        return prismUnmarshaller;
    }

    @NotNull
    public PrismMarshaller getPrismMarshaller() {
        return prismMarshaller;
    }

    /**
     * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
     */
    public DomLexicalProcessor getParserDom() {
        return lexicalProcessorRegistry.domProcessor();
    }

    /**
     * WARNING! This is not really public method. It should NOT not used outside the prism implementation.
     */
    @NotNull
    public BeanMarshaller getBeanMarshaller() {
        return beanMarshaller;
    }

    @NotNull
    public BeanUnmarshaller getBeanUnmarshaller() {
        return beanUnmarshaller;
    }

    @NotNull
    public JaxbDomHack getJaxbDomHack() {
        return jaxbDomHack;
    }

    @NotNull
    public SchemaDefinitionFactory getDefinitionFactory() {
        if (schemaDefinitionFactory == null) {
            schemaDefinitionFactory = new SchemaDefinitionFactory();
        }
        return schemaDefinitionFactory;
    }

    public void setDefinitionFactory(SchemaDefinitionFactory schemaDefinitionFactory) {
        this.schemaDefinitionFactory = schemaDefinitionFactory;
    }

    @NotNull
    @Override
    public PolyStringNormalizer getDefaultPolyStringNormalizer() {
        return defaultPolyStringNormalizer;
    }

    private LexicalProcessor getParser(String language) {
        return lexicalProcessorRegistry.processorFor(language);
    }

    private LexicalProcessor getParserNotNull(String language) {
        LexicalProcessor lexicalProcessor = getParser(language);
        if (lexicalProcessor == null) {
            throw new SystemException("No parser for language '"+language+"'");
        }
        return lexicalProcessor;
    }

    @Override
    public Protector getDefaultProtector() {
        return defaultProtector;
    }

    public void setDefaultProtector(Protector defaultProtector) {
        this.defaultProtector = defaultProtector;
    }

    @NotNull
    @Override
    public QueryConverter getQueryConverter() {
        return queryConverter;
    }

    @Override
    public PrismMonitor getMonitor() {
        return monitor;
    }

    @Override
    public void setMonitor(PrismMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public QName getDefaultRelation() {
        return defaultRelation;
    }

    public void setDefaultRelation(QName defaultRelation) {
        this.defaultRelation = defaultRelation;
    }

    @Override
    public QName getObjectsElementName() {
        return objectsElementName;
    }

    public void setObjectsElementName(QName objectsElementName) {
        this.objectsElementName = objectsElementName;
    }

    @Override
    public QName getDefaultReferenceTypeName() {
        return defaultReferenceTypeName;
    }

    public void setDefaultReferenceTypeName(QName defaultReferenceTypeName) {
        this.defaultReferenceTypeName = defaultReferenceTypeName;
    }

    //endregion

    //region Parsing
    @NotNull
    @Override
    public PrismParser parserFor(@NotNull File file) {
        return new PrismParserImplIO(new ParserFileSource(file), null, getDefaultParsingContext(), this, null, null, null, null);
    }

    @NotNull
    @Override
    public PrismParser parserFor(@NotNull InputStream stream) {
        return new PrismParserImplIO(new ParserInputStreamSource(stream), null, getDefaultParsingContext(), this, null, null, null, null);
    }

    @NotNull
    @Override
    public PrismParserNoIO parserFor(@NotNull String data) {
        return new PrismParserImplNoIO(new ParserStringSource(data), null, getDefaultParsingContext(), this, null, null, null, null);
    }

    @NotNull
    @Override
    public PrismParserNoIO parserFor(@NotNull RootXNode xnode) {
        return new PrismParserImplNoIO(new ParserXNodeSource((RootXNodeImpl) xnode), null, getDefaultParsingContext(), this, null, null, null, null);
    }

    @NotNull
    @Override
    public PrismParserNoIO parserFor(@NotNull Element data) {
        return new PrismParserImplNoIO(new ParserElementSource(data), null, getDefaultParsingContext(), this, null, null, null, null);
    }

    @NotNull
    @Override
    public String detectLanguage(@NotNull File file) throws IOException {
        return lexicalProcessorRegistry.detectLanguage(file);
    }

    @Override
    public ParsingMigrator getParsingMigrator() {
        return parsingMigrator;
    }

    @Override
    public void setParsingMigrator(ParsingMigrator parsingMigrator) {
        this.parsingMigrator = parsingMigrator;
    }
    //endregion

    //region adopt(...) methods
    /**
     * Set up the specified object with prism context instance and schema definition.
     */
    @Override
    public <C extends Containerable> void adopt(PrismContainer<C> container, Class<C> declaredType) throws SchemaException {
        container.revive(this);
        getSchemaRegistry().applyDefinition(container, declaredType, false);
    }

    @Override
    public <C extends Containerable> void adopt(PrismContainer<C> container) throws SchemaException {
        adopt(container, container.getCompileTimeClass());
    }

    @Override
    public void adopt(Objectable objectable) throws SchemaException {
        adopt(objectable.asPrismObject(), objectable.getClass());
    }

    @Override
    public void adopt(Containerable containerable) throws SchemaException {
        containerable.asPrismContainerValue().revive(this);
    }

    @Override
    public void adopt(PrismContainerValue value) throws SchemaException {
        value.revive(this);
    }

    @Override
    public <T extends Objectable> void adopt(ObjectDelta<T> delta) throws SchemaException {
        delta.revive(this);
        getSchemaRegistry().applyDefinition(delta, delta.getObjectTypeClass(), false);
    }

    @Override
    public <C extends Containerable, O extends Objectable> void adopt(C containerable, Class<O> type, ItemPath path) throws SchemaException {
        PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
        adopt(prismContainerValue, type, path);
    }

    @Override
    public <C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, Class<O> type,
            ItemPath path) throws SchemaException {
        prismContainerValue.revive(this);
        getSchemaRegistry().applyDefinition(prismContainerValue, type, path, false);
    }

    @Override
    public <C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, QName typeName,
            ItemPath path) throws SchemaException {
        prismContainerValue.revive(this);
        getSchemaRegistry().applyDefinition(prismContainerValue, typeName, path, false);
    }
    //endregion

    @NotNull
    @Override
    public PrismSerializer<String> serializerFor(@NotNull String language) {
        return new PrismSerializerImpl<>(new SerializerStringTarget(this, language), null, null, null, this, null);
    }

    @NotNull
    @Override
    public PrismSerializer<String> xmlSerializer() {
        return serializerFor(LANG_XML);
    }

    @NotNull
    @Override
    public PrismSerializer<String> jsonSerializer() {
        return serializerFor(LANG_JSON);
    }

    @NotNull
    @Override
    public PrismSerializer<String> yamlSerializer() {
        return serializerFor(LANG_YAML);
    }

    @NotNull
    @Override
    public PrismSerializer<Element> domSerializer() {
        return new PrismSerializerImpl<>(new SerializerDomTarget(this), null, null, null, this, null);
    }

    @NotNull
    @Override
    public PrismSerializer<RootXNode> xnodeSerializer() {
        return new PrismSerializerImpl<>(new SerializerXNodeTarget(this), null, null, null, this, null);
    }

    //endregion

    @NotNull
    @Override
    public <T extends Objectable> PrismObject<T> createObject(@NotNull Class<T> clazz) throws SchemaException {
        PrismObjectDefinition<T> definition = schemaRegistry.findObjectDefinitionByCompileTimeClass(clazz);
        if (definition == null) {
            throw new SchemaException("Definition for prism object holding " + clazz + " couldn't be found");
        }
        return definition.instantiate();
    }

    @NotNull
    @Override
    public <T extends Objectable> T createObjectable(@NotNull Class<T> clazz) throws SchemaException {
        return createObject(clazz).asObjectable();
    }

    @NotNull
    @Override
    public <O extends Objectable> PrismObject<O> createKnownObject(@NotNull Class<O> clazz) {
        try {
            return createObject(clazz);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected SchemaException while instantiating " + clazz + ": " + e.getMessage(), e);
        }
    }

    @NotNull
    @Override
    public <O extends Objectable> O createKnownObjectable(@NotNull Class<O> clazz) {
        return createKnownObject(clazz).asObjectable();
    }

    @NotNull
    public LexicalProcessorRegistry getLexicalProcessorRegistry() {
        return lexicalProcessorRegistry;
    }

    @Override
    public boolean isDefaultRelation(QName relation) {
        return relation == null || defaultRelation != null && QNameUtil.match(relation, defaultRelation);
    }

    @Override
    public boolean relationsEquivalent(QName relation1, QName relation2) {
        if (isDefaultRelation(relation1)) {
            return isDefaultRelation(relation2);
        } else {
            return QNameUtil.match(relation1, relation2);
        }
    }

    @Override
    public boolean relationMatches(QName relationQuery, QName relation) {
        return QNameUtil.match(relationQuery, PrismConstants.Q_ANY) || relationsEquivalent(relationQuery, relation);
    }

    @Override
    public boolean relationMatches(@NotNull List<QName> relationQuery, QName relation) {
        return relationQuery.stream().anyMatch(rq -> relationMatches(rq, relation));
    }

    @Override
    public ParsingContext getDefaultParsingContext() {
        return ParsingContextImpl.createDefault();
    }

    @Override
    public ParsingContext createParsingContextForAllowMissingRefTypes() {
        return ParsingContextImpl.allowMissingRefTypes();
    }

    @Override
    public ParsingContext createParsingContextForCompatibilityMode() {
        return ParsingContextImpl.createForCompatibilityMode();
    }

    @Override
    public UniformItemPath emptyPath() {
        return UniformItemPath.empty();
    }

    @Override
    public UniformItemPath path(Object... namesOrIdsOrSegments) {
        return UniformItemPath.create(namesOrIdsOrSegments);
    }

    @Override
    public Hacks hacks() {
        return hacks;
    }

    @Override
    public XNodeFactory xnodeFactory() {
        return xnodeFactory;
    }

    @Override
    public XNodeMutator xnodeMutator() {
        return hacks;
    }

    @Override
    public KeyStoreBasedProtector createInitializedProtector(KeyStoreBasedProtectorBuilder builder) {
        KeyStoreBasedProtectorImpl protector = new KeyStoreBasedProtectorImpl(builder);
        protector.init();
        return protector;
    }

    @Override
    public KeyStoreBasedProtector createProtector(KeyStoreBasedProtectorBuilder builder) {
        return new KeyStoreBasedProtectorImpl(builder);
    }

    @NotNull
    @Override
    public UniformItemPath toUniformPath(ItemPath path) {
        return UniformItemPath.from(path);
    }

    @Override
    public UniformItemPath toUniformPathKeepNull(ItemPath path) {
        return path != null ? UniformItemPath.from(path) : null;
    }

    @Override
    public UniformItemPath toUniformPath(ItemPathType path) {
        return UniformItemPath.from(path.getItemPath());
    }

    @Override
    public CanonicalItemPath createCanonicalItemPath(ItemPath itemPath, Class<? extends Containerable> clazz) {
        return new CanonicalItemPathImpl(itemPath, clazz, this);
    }

    @Override
    public CanonicalItemPath createCanonicalItemPath(ItemPath itemPath) {
        return new CanonicalItemPathImpl(itemPath, null, null);
    }

    @Override
    public <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass) throws SchemaException {
        return new DeltaBuilder<>(objectClass, this);
    }

    @Override
    public S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass) {
        return QueryBuilder.queryFor(queryClass, this);
    }

    @Override
    @NotNull
    public DeltaFactory deltaFactory() {
        return deltaFactory;
    }

    @NotNull
    @Override
    public QueryFactory queryFactory() {
        return queryFactory;
    }

    @NotNull
    @Override
    public ItemFactory itemFactory() {
        return itemFactory;
    }

    @NotNull
    @Override
    public DefinitionFactory definitionFactory() {
        return definitionFactory;
    }

    @NotNull
    @Override
    public ItemPathParser itemPathParser() {
        return itemPathParser;
    }

    // This is instance method to allow calling it via PrismContext interface
    @Override
    public void setExtraValidation(boolean value) {
        PrismContextImpl.extraValidation = value;
    }

    @NotNull
    @Override
    public SchemaFactory schemaFactory() {
        return schemaFactory;
    }

    @Override
    public void setValueMetadataFactory(ValueMetadataFactory valueMetadataFactory) {
        this.valueMetadataFactory = valueMetadataFactory;
    }

    @Override
    public ValueMetadataFactory getValueMetadataFactory() {
        return valueMetadataFactory;
    }

    @Override
    public EquivalenceStrategy getProvenanceEquivalenceStrategy() {
        return provenanceEquivalenceStrategy;
    }

    public void setProvenanceEquivalenceStrategy(EquivalenceStrategy provenanceEquivalenceStrategy) {
        this.provenanceEquivalenceStrategy = provenanceEquivalenceStrategy;
    }
}
