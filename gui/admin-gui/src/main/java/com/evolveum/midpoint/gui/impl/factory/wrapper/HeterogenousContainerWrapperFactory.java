/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

/**
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
        return parent.getDefinitions().stream().filter(def -> filterDefinitins(value, def)).collect(Collectors.toList());
    }

    private boolean filterDefinitins(PrismContainerValue<C> value, ItemDefinition<?> def) {
        Item<?, ?> child = value.findItem(def.getItemName());
        return (child != null && !child.isEmpty()) || !(def instanceof PrismContainerDefinition);
    }

    /**
     *
     * match single value containers which contains a looot of other conainers, e.g. policy rule, policy action, notification configuration, etc
     */
    @Override
    public boolean match(ItemDefinition<?> def) {
        if (def.getTypeClass() != null && def.getTypeClass().isAssignableFrom(CompositeCorrelatorType.class)) {
            return false;
        }

        QName defName = def.getTypeName();

        if (CollectionRefSpecificationType.COMPLEX_TYPE.equals(defName)
        && def.getItemName().equivalent(CollectionRefSpecificationType.F_BASE_COLLECTION_REF)) {
            return true;
        }

        // TODO
//        if (TaskPartsDefinitionType.COMPLEX_TYPE.equals(defName)) {
//            return true;
//        }

        if (!(def instanceof PrismContainerDefinition)) {
            return false;
        }

        PrismContainerDefinition<?> containerDef = (PrismContainerDefinition<?>) def;

        if (containerDef.isMultiValue() && isNotPolicyConstraint(containerDef)) {
            return false;
        }

        List<? extends ItemDefinition> defs = containerDef.getDefinitions();
        int containers = 0;
        for (ItemDefinition<?> itemDef : defs) {
            if (itemDef instanceof PrismContainerDefinition<?> && itemDef.isMultiValue()) {
                containers++;
            }
        }

        return containers > 2;

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
