/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * Just like {@link PrismContainerWrapperFactoryImpl} but sets {@link PrismContainerValueWrapper#isHeterogenous()} flag to `true`.
 *
 * @author katka
 */
@Component
public class HeterogenousContainerWrapperFactory<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public PrismContainerValueWrapper<C> createValueWrapper(PrismContainerWrapper<C> parent,
            PrismContainerValue<C> value, ValueStatus status, WrapperContext context)
            throws SchemaException {
        PrismContainerValueWrapper<C> containerValueWrapper = super.createValueWrapper(parent, value, status, context);
        containerValueWrapper.setHeterogenous(true);
        return containerValueWrapper;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<C> parent, PrismContainerValue<C> value) {
        return parent.getDefinitions().stream().filter(def ->
                filterDefinitions(value, def, parent.getStatus())).collect(Collectors.toList());
    }

    protected boolean filterDefinitions(PrismContainerValue<C> value, ItemDefinition<?> def, ItemStatus status) {
        Item<?, ?> child = value.findItem(def.getItemName());
        return (child != null && (!child.isEmpty() || shouldFilterDefinitionForEmptyContainer(value, def, status)))
                || !(def instanceof PrismContainerDefinition);
    }

    /**
     * Policy action empty containers should be displayed in GUI (see #10928)
     * @param value
     * @param def
     * @param status
     * @return
     */
    private boolean shouldFilterDefinitionForEmptyContainer(PrismContainerValue<C> value, ItemDefinition<?> def, ItemStatus status) {
        return ItemStatus.NOT_CHANGED.equals(status) && PolicyActionType.class.isAssignableFrom(def.getTypeClass());
    }

    /**
     *
     * match single value containers which contains a looot of other conainers, e.g. policy rule, policy action, notification configuration, etc
     */
    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> itemDef, PrismContainerValue<C> parent) {
        if (itemDef.getTypeClass() != null
                && (itemDef.getTypeClass().isAssignableFrom(CompositeCorrelatorType.class)
                || itemDef.getTypeClass().isAssignableFrom(SecretsProvidersType.class)
                || itemDef.getTypeClass().isAssignableFrom(SchemaHandlingType.class)
                || itemDef.getTypeClass().isAssignableFrom(AssociatedResourceObjectTypeDefinitionType.class)
                || itemDef.getTypeClass().isAssignableFrom(ShadowAssociationDefinitionType.class)
                || itemDef.getTypeClass().isAssignableFrom(AdminGuiConfigurationType.class))) {
            return false;
        }

        QName typeName = itemDef.getTypeName();

        if (CollectionRefSpecificationType.COMPLEX_TYPE.equals(typeName)
                && itemDef.getItemName().equivalent(CollectionRefSpecificationType.F_BASE_COLLECTION_REF)) {
            return true;
        }

        if (ObjectParentSelectorType.COMPLEX_TYPE.equals(typeName)
                && itemDef.getItemName().equivalent(ObjectSelectorType.F_PARENT)) {
            // The ObjectSelectorType#parent points back to ObjectSelectorType
            // (via ObjectParentSelectorType), so normally an endless recursion would occur here.
            // Therefore, we treat it as a heterogeneous container. At least for now.
            //
            // TODO review this; see MID-8910.
            return true;
        }

        if (!(itemDef instanceof PrismContainerDefinition<?> containerDef)) {
            return false;
        }

        if (containerDef.isElaborate()
                && parent.getCompileTimeClass() != null
                && WebPrismUtil.findContainerValueParent(parent, parent.getCompileTimeClass()) != null) {
            return true;
        }

        if (containerDef.isMultiValue() && isNotPolicyConstraint(containerDef)) {
            return false;
        }

        List<? extends ItemDefinition<?>> childDefs = containerDef.getDefinitions();
        int multiValuedChildContainers = 0;
        for (ItemDefinition<?> childItemDef : childDefs) {
            if (childItemDef instanceof PrismContainerDefinition<?> && childItemDef.isMultiValue()) {
                multiValuedChildContainers++;
            }
        }

        return multiValuedChildContainers > 2;
    }

    private boolean isNotPolicyConstraint(PrismContainerDefinition<?> containerDef) {
        if (containerDef.getCompileTimeClass() == null) {
            return true;
        }

        return !AbstractPolicyConstraintType.class.isAssignableFrom(containerDef.getCompileTimeClass());
    }

    @Override
    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 110;
    }

}
