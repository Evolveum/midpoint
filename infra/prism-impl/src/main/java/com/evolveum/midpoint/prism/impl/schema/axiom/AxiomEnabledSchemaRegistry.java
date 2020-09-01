package com.evolveum.midpoint.prism.impl.schema.axiom;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.lang.antlr.AxiomModelStatementSource;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.impl.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.prism.impl.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.PrismReferenceDefinitionImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class AxiomEnabledSchemaRegistry extends SchemaRegistryImpl {

    private static final Lazy<AxiomModelStatementSource> PRISM_MODEL = ModelReactorContext.sourceFromResource("/prism-model.axiom");
    private static final Lazy<AxiomSchemaContext> PRISM_BASE = Lazy.from(() -> prismSources(ModelReactorContext.defaultReactor()).computeSchemaContext());

    private static final String PRISM_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/common/prism";
    private static final String COMMON_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    private static final String PRISM_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";

    private static final QName OBJECT_REFERENCE_TYPE = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "ObjectReferenceType");

    private static final AxiomName PROPERTY_ITEM = AxiomName.from(PRISM_NAMESPACE, "PropertyItemDefinition");
    private static final AxiomName CONTAINER_ITEM = AxiomName.from(PRISM_NAMESPACE, "ContainerItemDefinition");
    private static final AxiomName REFERENCE_ITEM = AxiomName.from(PRISM_NAMESPACE, "ReferenceItemDefinition");

    private static final String XSD = "http://www.w3.org/2001/XMLSchema";


    private static final BiMap<AxiomName, QName> AXIOM_XSD_TYPES = ImmutableBiMap.<AxiomName, QName>builder()
            .put(AxiomName.builtIn("String"), DOMUtil.XSD_STRING)
            .put(AxiomName.builtIn("DateTime"), DOMUtil.XSD_DATETIME)
            .put(AxiomName.builtIn("Uri"), DOMUtil.XSD_ANYURI)
            .put(AxiomName.builtIn("Integer"), DOMUtil.XSD_INT)
            .put(AxiomName.from(PRISM_NAMESPACE, "ItemPath"), new QName(PRISM_TYPES,"ItemPathType"))
            .build();
    private static final AxiomName DISPLAY_NAME = PROPERTY_ITEM.localName("displayName");
    private static final String AXIOM_SUFFIX = ".axiom";

    AxiomSchemaContext currentContext;
    Collection<AxiomModelStatementSource> sources = new HashSet<>();

    private PrismContainerDefinition<?> valueMetadata;

    public AxiomEnabledSchemaRegistry() {
        super();
    }

    @Override
    protected void parseAdditionalSchemas() throws SchemaException {
        parseAxiomSchemas();
        enhanceMetadata();
    }

    static final ModelReactorContext prismSources(ModelReactorContext context) {
        context.loadModelFromSource(PRISM_MODEL.get());
        return context;
    }

    private void parseAxiomSchemas() {
        ModelReactorContext context = ModelReactorContext.reactor(PRISM_BASE.get());
        prismSources(context);
        for (AxiomModelStatementSource source : sources) {
            context.loadModelFromSource(source);
        }
        currentContext = context.computeSchemaContext();
    }

    // FIXME: Should we enhance? or should we just return Axiom metadata?
    void enhanceMetadata() {
        QName targetType = this.getValueMetadataTypeName();
        AxiomTypeDefinition axiomMetadata = currentContext.getType(AxiomValue.METADATA_TYPE).orElseThrow(() -> new IllegalStateException("Axiom ValueMetadata type not present"));

        PrismContainerDefinition<?> targetDef = findContainerDefinitionByType(targetType);
        Preconditions.checkState(targetDef != null,"Value metadata type needs to be available");
        valueMetadata = targetDef;
        Preconditions.checkState(targetDef.canModify(), "Value metadata definition not can be modified");
        copyItemDefs(asMutable(targetDef.getComplexTypeDefinition()),axiomMetadata);

    }

    private void copyItemDefs(MutableComplexTypeDefinition target, AxiomTypeDefinition source) {
        for (Entry<AxiomName, AxiomItemDefinition> entry : source.itemDefinitions().entrySet()) {
            MutableItemDefinition<?> prismified = prismify(entry.getValue());
            QName name = qName(entry.getValue().name());
            MutableComplexTypeDefinition realTarget = primaryOrExtension(target, entry.getValue());
            if(realTarget.containsItemDefinition(name)) {
                realTarget.replaceDefinition(name , prismified);
            } else {
                realTarget.add(prismified);
            }
        }
    }

    private MutableComplexTypeDefinition primaryOrExtension(MutableComplexTypeDefinition target,
            AxiomItemDefinition value) {
        if (value.name().namespace().equals(target.getTypeName().getNamespaceURI())) {
            return target;
        }
        return extensionFor(target, value.name().namespace());
    }

    private MutableComplexTypeDefinition extensionFor(MutableComplexTypeDefinition target, String namespace) {
        PrismContainerDefinition<Containerable> extContainer = target.findContainerDefinition(PrismConstants.EXTENSION_LOCAL_NAME);
        return asMutable(extContainer.getComplexTypeDefinition());
    }

    private MutableItemDefinition<?> prismify(AxiomItemDefinition value) {

        if(isType(value, PROPERTY_ITEM)) {
            return prismifyProperty(value);
        }

        if(isType(value, CONTAINER_ITEM)) {
            return prismifyContainer(value);
        }
        if(isType(value, REFERENCE_ITEM)) {
            return prismifyReference(value);
        }
        throw new UnsupportedOperationException("Not implemented mapping for " + value.asComplex().get().type().get().name());
    }

    private MutableItemDefinition<?> prismifyReference(AxiomItemDefinition value) {
        QName elementName = qName(value.name());
        QName typeName = OBJECT_REFERENCE_TYPE;
        PrismReferenceDefinitionImpl ret = new PrismReferenceDefinitionImpl(elementName, typeName, getPrismContext());



        // FIXME

        return fillDetails(ret,value);
    }

    private MutableItemDefinition<?> prismifyContainer(AxiomItemDefinition value) {
        QName elementName = qName(value.name());
        ComplexTypeDefinition complexTypeDefinition = prismifyStructured(value.typeDefinition());
        PrismContainerDefinitionImpl<?> container = new PrismContainerDefinitionImpl(elementName, complexTypeDefinition, getPrismContext());
        return fillDetails(container, value);
    }

    private MutableItemDefinition<?> fillDetails(MutableItemDefinition<?> target, AxiomItemDefinition source) {
        set(target::setDisplayName, source, DISPLAY_NAME);
        target.setMinOccurs(source.minOccurs());
        target.setMaxOccurs(source.maxOccurs());
        target.setDocumentation(source.documentation());


        return target;
    }


    private String displayName(AxiomItemDefinition source) {
        return source.asComplex().flatMap(v -> v.item(DISPLAY_NAME)).map(v -> v.onlyValue().value().toString()).orElse("");
    }

    private void set(Consumer<String> target, AxiomItemDefinition source, AxiomName itemName) {
        Optional<String> value = source.asComplex().flatMap(v -> v.item(DISPLAY_NAME)).map(v -> v.onlyValue().value().toString());
        if(value.isPresent()) {
            target.accept(value.get());
        }
    }

    private MutablePrismPropertyDefinition<?> prismifyProperty(AxiomItemDefinition value) {
        QName elementName = qName(value.name());
        QName typeName = prismify(value.typeDefinition());

        PrismPropertyDefinitionImpl<?> property = new PrismPropertyDefinitionImpl<>(
                elementName, typeName, getPrismContext());
        return property;
    }

    private QName prismify(AxiomTypeDefinition typeDefinition) {
        QName prismName = qName(typeDefinition.name());
        TypeDefinition maybe = findTypeDefinitionByType(prismName);
        if(maybe != null) {
            return maybe.getTypeName();
        }
        if(typeDefinition.isComplex()) {
            return prismifyStructured(typeDefinition).getTypeName();
        }
        QName maybeXsd = AXIOM_XSD_TYPES.get(typeDefinition.name());
        if(maybeXsd != null) {
            return maybeXsd;
        }
        throw new UnsupportedOperationException(typeDefinition.name().toString());
    }

    private ComplexTypeDefinition prismifyStructured(AxiomTypeDefinition typeDefinition) {
        QName prismName = qName(typeDefinition.name());
        ComplexTypeDefinition maybe = findComplexTypeDefinitionByType(prismName);
        if(maybe != null) {
            return maybe;
        }

        ComplexTypeDefinitionImpl typeDef = new ComplexTypeDefinitionImpl(prismName, getPrismContext());
        reuseXjcClassIfExists(typeDef);
        copyItemDefs(typeDef, typeDefinition);
        return typeDef;
    }

    private void reuseXjcClassIfExists(ComplexTypeDefinitionImpl typeDef) {
        ComplexTypeDefinition maybeClass = findComplexTypeDefinitionByType(typeQname(typeDef.getTypeName()));
        if(maybeClass != null) {
            typeDef.setCompileTimeClass(maybeClass.getCompileTimeClass());
        }

    }

    private @NotNull QName typeQname(QName name) {
        return new QName(name.getNamespaceURI(), name.getLocalPart() + "Type");
    }

    private QName qName(AxiomName name) {
        return AxiomBased.qName(name);
    }

    private boolean isProperty(AxiomItemDefinition value) {
        return (PROPERTY_ITEM.equals(value.asComplex().get().type().get().name()));
    }

    private boolean isType(AxiomItemDefinition value, AxiomName type) {
        return type.equals(value.asComplex().get().type().get().name());
    }

    private MutableComplexTypeDefinition asMutable(ComplexTypeDefinition complexTypeDefinition) {
        return (MutableComplexTypeDefinition) complexTypeDefinition;
    }

    @Override
    public synchronized @NotNull PrismContainerDefinition<?> getValueMetadataDefinition() {
        return valueMetadata;
    }

    public void addAxiomSource(AxiomModelStatementSource source) {
        sources.add(source);
    }


    @Override
    protected void loadPrismSchemaFileDescription(File file) throws SchemaException, IOException {
        if(file.getName().endsWith(AXIOM_SUFFIX)) {
            addAxiomSource(AxiomModelStatementSource.from(file));
        } else {
            super.loadPrismSchemaFileDescription(file);
        }
    }

}
