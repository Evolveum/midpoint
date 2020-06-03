package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomComplexValue;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueFactory;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;


public class AxiomTypeDefinitionImpl extends AbstractBaseDefinition implements AxiomTypeDefinition {

    public static final AxiomValueFactory<Collection<AxiomItem<?>>, AxiomTypeDefinition> FACTORY =AxiomTypeDefinitionImpl::new;

    private final Map<AxiomName, AxiomItemDefinition> itemDefinitions;
    private final Optional<AxiomTypeDefinition> superType;
    private final Optional<AxiomItemDefinition> argument;
    private final Collection<AxiomIdentifierDefinition> identifiers;

    public AxiomTypeDefinitionImpl(AxiomTypeDefinition def, Collection<AxiomItem<?>> value, Map<AxiomName, AxiomItem<?>> keywordMap) {
        super(def, null, keywordMap);

        //super(keyword, value, children, keywordMap);
        ImmutableMap.Builder<AxiomName, AxiomItemDefinition> builder =  ImmutableMap.builder();
        Optional<AxiomItem<AxiomItemDefinition>> itemDef = item(Item.ITEM_DEFINITION.name());
        if(itemDef.isPresent()) {
            supplyAll(name(),builder, itemDef.get().values());
        }
        itemDefinitions = builder.build();

        superType = onlyValue(AxiomTypeDefinition.class,Item.SUPERTYPE_REFERENCE, Item.REF_TARGET).map(v -> from(v.asComplex().get()));


        argument = this.<AxiomName>item(Item.ARGUMENT.name()).flatMap(v -> itemDefinition(v.onlyValue().value()));
        identifiers = Collections2.transform((this.item(Item.IDENTIFIER_DEFINITION).map(v -> v.values()).orElse(Collections.emptyList())),
                 AxiomIdentifierDefinitionImpl::from);
    }

    public static AxiomTypeDefinition from(AxiomComplexValue value) {
        if(value instanceof AxiomTypeDefinition) {
            return (AxiomTypeDefinition) value;
        }
        return new AxiomTypeDefinitionImpl(value.type().get(), null, value.asComplex().get().itemMap());
    }

    @Override
    public <V> Optional<AxiomItem<V>> item(AxiomName name) {
        return super.item(name);
    }

    @Override
    public Optional<AxiomItemDefinition> argument() {
        if (!argument.isPresent() && superType().isPresent()) {
            return superType().get().argument();
        }
        return argument;
    }

    @Override
    public Optional<AxiomTypeDefinition> superType() {
        return superType;
    }

    @Override
    public Map<AxiomName, AxiomItemDefinition> itemDefinitions() {
        return itemDefinitions;
    }

    @Override
    public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
        return identifiers;
    }

    private void supplyAll(AxiomName type, Builder<AxiomName, AxiomItemDefinition> builder,
            Collection<AxiomValue<AxiomItemDefinition>> values) {
        for(AxiomValue<AxiomItemDefinition> v : values) {
            AxiomItemDefinition val = AxiomItemDefinitionImpl.from(v);
            AxiomName name = Inheritance.adapt(type, val.name());
            builder.put(name, val);
        }
    }

}
