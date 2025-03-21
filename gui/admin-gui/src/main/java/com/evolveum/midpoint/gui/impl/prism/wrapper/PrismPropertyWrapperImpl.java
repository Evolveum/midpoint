/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;

import org.jetbrains.annotations.Nullable;

/**
 * @author katka
 */
public class PrismPropertyWrapperImpl<T> extends ItemWrapperImpl<PrismProperty<T>, PrismPropertyValueWrapper<T>> implements PrismPropertyWrapper<T> {

    @Serial private static final long serialVersionUID = 1L;

    private String predefinedValuesOid;
    private boolean muteDeltaCreate;

    public PrismPropertyWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<T> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return getItemDefinition().getAllowedValues();
    }

    @Override
    public Collection<? extends DisplayableValue<T>> getSuggestedValues() {
        return getItemDefinition().getSuggestedValues();
    }

    @Override
    public T defaultValue() {
        return getItemDefinition().defaultValue();
    }

    @Override
    public Boolean isIndexed() {
        return getItemDefinition().isIndexed();
    }

    @Override
    public QName getMatchingRuleQName() {
        return getItemDefinition().getMatchingRuleQName();
    }

    @Override
    public @NotNull MatchingRule<T> getMatchingRule() {
        return getItemDefinition().getMatchingRule();
    }

    @Override
    public @NotNull PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return getItemDefinition().createEmptyDelta(path);
    }

    @NotNull
    @Override
    public PrismPropertyDefinition<T> clone() {
        return getItemDefinition().clone();
    }

    @Override
    public @NotNull ItemDefinition<PrismProperty<T>> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public @NotNull PrismPropertyDefinitionMutator<T> mutator() {
        return getItemDefinition().mutator();
    }

    @NotNull
    @Override
    public PrismProperty<T> instantiate() {
        return getItemDefinition().instantiate();
    }

    @NotNull
    @Override
    public PrismProperty<T> instantiate(QName name) {
        return getItemDefinition().instantiate(name);
    }

    @Override
    public String getPredefinedValuesOid() {
        return predefinedValuesOid;
    }

    @Override
    public void setPredefinedValuesOid(String predefinedValuesOid) {
        this.predefinedValuesOid = predefinedValuesOid;
    }

    @Override
    public boolean isEmpty() {
        if (super.isEmpty()) {
            return true;
        }
        List<PrismPropertyValue<T>> pVals = getItem().getValues();
        boolean allEmpty = true;
        for (PrismPropertyValue<T> pVal : pVals) {
            if (pVal.getRealValue() != null) {
                allEmpty = false;
                break;
            }
        }

        return allEmpty;
    }

    @SuppressWarnings("unchecked")
    public PrismPropertyDefinition<T> getItemDefinition() {
        return super.getItemDefinition();
    }

    @Override
    public boolean isImmutable() {
        // TODO
        return false;
    }

    @Override
    public void freeze() {
        // TODO
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        // TODO
        return false;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        // TODO
    }

    @Override
    protected void removeNotChangedStatusValue(PrismPropertyValueWrapper<T> valueWrapper, Item rawItem) {
        if (!isSingleValue()) {
            super.removeNotChangedStatusValue(valueWrapper, rawItem);
            return;
        }
        valueWrapper.setRealValue(null);
        valueWrapper.setStatus(ValueStatus.MODIFIED);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected PrismPropertyValue<T> createNewEmptyValue(ModelServiceLocator locator) {
        return locator.getPrismContext().itemFactory().createPropertyValue();
    }

    @Override
    public Class<T> getTypeClass() {
        //noinspection unchecked
        return (Class<T>) super.getTypeClass();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    public void setMuteDeltaCreate(boolean muteDeltaCreate) {
        this.muteDeltaCreate = muteDeltaCreate;
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {
        if (muteDeltaCreate) {
            return Collections.emptyList();
        }
        return super.getDelta();
    }
}
