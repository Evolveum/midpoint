/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class AssociationMappingExpressionWrapper<C extends Containerable> extends PrismContainerWrapperImpl<C> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismSchemaWrapper.class);

    private final ItemPath wrapperPath;
    private final ExpressionType expression;

    public AssociationMappingExpressionWrapper(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<C> item,
            ItemStatus status,
            ItemPath wrapperPath,
            ExpressionType expression) {
        super(parent, item, status);
        this.wrapperPath = wrapperPath;
        this.expression = expression;
    }

    public ExpressionType getExpression() {
        return expression;
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {

        Collection<D> deltas = new ArrayList<>();
        PrismObjectWrapper<ObjectType> objectWrapper = findObjectWrapper();
        PrismPropertyDefinition<ExpressionType> propertyDef = objectWrapper.getItem().getDefinition().findPropertyDefinition(wrapperPath);

        for (PrismContainerValueWrapper<C> pVal : getValues()) {
            LOGGER.trace("Processing delta for value:\n {}", pVal);
            PropertyDelta<ExpressionType> delta = propertyDef.createEmptyDelta(wrapperPath);
            switch (pVal.getStatus()) {
                case ADDED:

                    PrismContainerValue<C> valueToAdd = pVal.getNewValue().clone();
                    if (valueToAdd.isEmpty() || valueToAdd.isIdOnly()) {
                        break;
                    }

                    valueToAdd = WebPrismUtil.cleanupEmptyContainerValue(valueToAdd);
                    if (valueToAdd == null || valueToAdd.isIdOnly()) {
                        LOGGER.trace("Value is empty, skipping delta creation.");
                        break;
                    }

                    if (valueToAdd.isEmpty()) {
                        LOGGER.trace("Value is empty, skipping delta creation.");
                        break;
                    }

                    delta.addValueToAdd(createSchemaValue(valueToAdd));
                    deltas.add((D) delta);
                    LOGGER.trace("Computed delta: \n {}", delta);
                    break;
                case NOT_CHANGED:
                    for (ItemWrapper iw : pVal.getItems()) {
                        LOGGER.trace("Start computing modifications for {}", iw);
                        Collection subDeltas = iw.getDelta();
                        if (CollectionUtils.isNotEmpty(subDeltas)) {
                            LOGGER.trace("Deltas computed for {}", iw);
                            delta.addValueToAdd(
                                    createSchemaValue(WebPrismUtil.cleanupEmptyContainerValue(pVal.getNewValue().clone())));
                            deltas.add((D) delta);
                        }
                        LOGGER.trace("Computed deltas:\n {}", subDeltas);
                    }

                    break;
                case DELETED:
                    PrismProperty<ExpressionType> schemaProperty = objectWrapper.getItem().findProperty(wrapperPath);
                    delta.addValueToDelete(schemaProperty.getValue().clone());
                    deltas.add((D) delta);
                    LOGGER.trace("Computed delta: \n {}", delta.debugDump());
                    break;
            }
        }
        return deltas;
    }

    protected abstract PrismPropertyValue<ExpressionType> createSchemaValue(PrismContainerValue<C> value) throws SchemaException;
}
