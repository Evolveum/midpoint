package com.evolveum.midpoint.prism.impl.schema.axiom;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.MutableTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class AxiomEnabledSchemaRegistry extends SchemaRegistryImpl {

    private static final Lazy<AxiomModelStatementSource> PRISM_MODEL = ModelReactorContext.sourceFromResource("/prism-model.axiom");
    private static final Lazy<AxiomSchemaContext> PRISM_BASE = Lazy.from(() -> prismSources(ModelReactorContext.defaultReactor()).computeSchemaContext());

    private static final String PRISM_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/common/prism";
    private static final AxiomName PROPERTY_ITEM_DEF = AxiomName.from(PRISM_NAMESPACE, "PropertyItemDefinition");
    private static final String XSD = "http://www.w3.org/2001/XMLSchema";


    private static final BiMap<AxiomName, QName> AXIOM_XSD_TYPES = ImmutableBiMap.<AxiomName, QName>builder()
            .put(AxiomName.builtIn("String"), new QName(XSD,"string"))
            .build();

    AxiomSchemaContext currentContext;
    Collection<AxiomModelStatementSource> sources = new HashSet<>();

    private PrismContainerDefinition<?> valueMetadata;

    public AxiomEnabledSchemaRegistry() {
        super();
    }

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
            target.add(prismify(entry.getValue()));
        }
    }

    private MutableItemDefinition<?> prismify(AxiomItemDefinition value) {

        if(isProperty(value)) {
            return prismifyProperty(value);
        }


        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented mapping");
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
        throw new UnsupportedOperationException();
    }

    private TypeDefinition prismifyStructured(AxiomTypeDefinition typeDefinition) {
        throw new UnsupportedOperationException("Can not prismify structured");
    }

    private QName qName(AxiomName name) {
        return AxiomBased.qName(name);
    }

    private boolean isProperty(AxiomItemDefinition value) {
        return (PROPERTY_ITEM_DEF.equals(value.asComplex().get().type().get().name()));
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

}
