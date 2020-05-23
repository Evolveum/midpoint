package com.evolveum.axiom.lang.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomItemValueFactory;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;


public class AxiomTypeDefinitionImpl extends AbstractBaseDefinition<AxiomTypeDefinition> implements AxiomTypeDefinition {

    public static final AxiomItemValueFactory<AxiomTypeDefinition, AxiomTypeDefinition> FACTORY =AxiomTypeDefinitionImpl::new;

    private final Map<AxiomIdentifier, AxiomItemDefinition> itemDefinitions;
    private final Optional<AxiomTypeDefinition> superType;
    private final Optional<AxiomItemDefinition> argument;
    private final Collection<AxiomIdentifierDefinition> identifiers;

    public AxiomTypeDefinitionImpl(AxiomTypeDefinition def, AxiomTypeDefinition value, Map<AxiomIdentifier, AxiomItem<?>> keywordMap) {
        super(def, null, keywordMap);

        //super(keyword, value, children, keywordMap);
        ImmutableMap.Builder<AxiomIdentifier, AxiomItemDefinition> builder =  ImmutableMap.builder();
        Optional<AxiomItem<AxiomItemDefinition>> itemDef = item(Item.ITEM_DEFINITION.name());
        if(itemDef.isPresent()) {
            supplyAll(builder, itemDef.get().values());
        }
        itemDefinitions = builder.build();

        superType = this.<AxiomTypeDefinition>item(Item.SUPERTYPE_REFERENCE.name()).map(v -> v.onlyValue().get());

        argument = this.<AxiomIdentifier>item(Item.ARGUMENT.name()).flatMap(v -> itemDefinition(v.onlyValue().get()));
        identifiers = upcast(this.<AxiomIdentifierDefinition>item(Item.IDENTIFIER_DEFINITION.name()).map(v -> v.values()).orElse(Collections.emptyList()));
    }

    @Override
    public AxiomTypeDefinition get() {
        return this;
    }

    private <V extends AxiomItemValue<V>> Collection<V> upcast(Collection<AxiomItemValue<V>> itemValue) {
        return (Collection) itemValue;
    }

    @Override
    public <V> Optional<AxiomItem<V>> item(AxiomIdentifier name) {
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
    public Map<AxiomIdentifier, AxiomItemDefinition> itemDefinitions() {
        return itemDefinitions;
    }

    @Override
    public Collection<AxiomIdentifierDefinition> identifierDefinitions() {
        return identifiers;
    }

    private void supplyAll(Builder<AxiomIdentifier, AxiomItemDefinition> builder,
            Collection<AxiomItemValue<AxiomItemDefinition>> values) {
        for(AxiomItemValue<AxiomItemDefinition> v : values) {
            AxiomItemDefinition val = v.get();
            builder.put(val.name(), val);
        }
    }

}
