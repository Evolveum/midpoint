/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.PrismSchemaTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

public class PrismSchemaWrapper extends PrismContainerWrapperImpl<PrismSchemaType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismSchemaWrapper.class);

    private final ItemPath wrapperPath;

    public PrismSchemaWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<PrismSchemaType> item, ItemStatus status, ItemPath wrapperPath) {
        super(parent, item, status);
        this.wrapperPath = wrapperPath;
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {

        Collection<D> deltas = new ArrayList<>();
        PrismObjectWrapper<ObjectType> objectWrapper = findObjectWrapper();
        PrismPropertyDefinition<SchemaDefinitionType> propertyDef = objectWrapper.getItem().getDefinition().findPropertyDefinition(wrapperPath);

        for (PrismContainerValueWrapper<PrismSchemaType> pVal : getValues()) {
            LOGGER.trace("Processing delta for value:\n {}", pVal);
            PropertyDelta<SchemaDefinitionType> delta = propertyDef.createEmptyDelta(wrapperPath);
            switch (pVal.getStatus()) {
                case ADDED:

                    PrismContainerValue<PrismSchemaType> valueToAdd = pVal.getNewValue().clone();
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

                    delta.addValueToAdd(createSchemaValue(objectWrapper, valueToAdd));
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
                                    createSchemaValue(
                                            objectWrapper,
                                            WebPrismUtil.cleanupEmptyContainerValue(pVal.getNewValue().clone())));
                            deltas.add((D) delta);
                            break;
                        }
                        LOGGER.trace("Computed deltas:\n {}", subDeltas);
                    }

                    break;
                case DELETED:
                    PrismProperty<SchemaDefinitionType> schemaProperty = objectWrapper.getItem().findProperty(wrapperPath);
                    delta.addValueToDelete(schemaProperty.getValue().clone());
                    deltas.add((D) delta);
                    LOGGER.trace("Computed delta: \n {}", delta.debugDump());
                    break;
            }
        }
        return deltas;
    }

    private PrismPropertyValue<SchemaDefinitionType> createSchemaValue(
            PrismObjectWrapper<ObjectType> objectWrapper, PrismContainerValue<PrismSchemaType> value) throws SchemaException {
        @NotNull ObjectType objectBean = objectWrapper.getObject().asObjectable();
        String lifecycleState = objectBean.getLifecycleState();
        @NotNull PrismSchemaType prismSchemaBean = value.asContainerable();
        SchemaDefinitionType schemaDefBean = PrismSchemaTypeUtil.convertToSchemaDefinitionType(prismSchemaBean, lifecycleState);
        return PrismContext.get().itemFactory().createPropertyValue(schemaDefBean);
    }
}
