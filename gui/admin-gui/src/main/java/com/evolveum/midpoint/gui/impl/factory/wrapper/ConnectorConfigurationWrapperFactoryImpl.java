/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

@Component
public class ConnectorConfigurationWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ConnectorConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorConfigurationWrapperFactoryImpl.class);

    @Override
    public boolean match(ItemDefinition def) {
        if (def instanceof PrismContainerDefinition && ((PrismContainerDefinition) def).getCompileTimeClass() != null) {
            return ConnectorConfigurationType.class.isAssignableFrom(((PrismContainerDefinition) def).getCompileTimeClass());
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper parent, PrismContainerValue value) {
        List<PrismContainerDefinition> relevantDefinitions = new ArrayList<>();
        List<? extends ItemDefinition> defs = parent.getDefinitions();
        for (ItemDefinition<?> def : defs) {
            if (def instanceof PrismContainerDefinition) {
                relevantDefinitions.add((PrismContainerDefinition) def);
            }
        }
        relevantDefinitions.sort((o1, o2) -> {
            int ord1 = o1.getDisplayOrder() != null ? o1.getDisplayOrder() : Integer.MAX_VALUE;
            int ord2 = o2.getDisplayOrder() != null ? o2.getDisplayOrder() : Integer.MAX_VALUE;
            return Integer.compare(ord1, ord2);
        });
        return relevantDefinitions;
    }

    @Override
    public PrismContainerWrapper<ConnectorConfigurationType> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        ItemName name = def.getItemName();

        PrismContainer<ConnectorConfigurationType> childItem = parent.getNewValue().findContainer(name);
        ItemStatus status = getStatus(childItem);

        if (skipCreateWrapper(def, status, context, childItem == null || CollectionUtils.isEmpty(childItem.getValues()))) {
            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
            return null;
        }

        if (childItem == null || childItem.isEmpty() || childItem.getDefinition().getItemName().equals(ResourceType.F_CONNECTOR_CONFIGURATION)) {

            PrismReference connectorRef = parent.getNewValue().findReference(ResourceType.F_CONNECTOR_REF);
            if (connectorRef != null && connectorRef.getValue() != null && connectorRef.getValue().getRealValue() != null
                    && connectorRef.getValue().getRealValue().getOid() != null) {

                PrismObject<ConnectorType> connector = null;
                try {
                    connector = getModelService().getObject(
                            ConnectorType.class,
                            connectorRef.getValue().getRealValue().getOid(),
                            null,
                            context.getTask(),
                            context.getResult());

                    if (connector != null) {
                        ConnectorType connectorType = connector.asObjectable();
                        PrismSchema schema = ConnectorTypeUtil.parseConnectorSchema(connectorType);
                        PrismContainerDefinition<ConnectorConfigurationType> definition =
                                ConnectorTypeUtil.findConfigurationContainerDefinitionRequired(connectorType, schema);
                        // Fixing (errorneously) set maxOccurs = unbounded. See MID-2317 and related issues.
                        PrismContainerDefinition<ConnectorConfigurationType> definitionFixed = definition.clone();
                        definitionFixed.toMutable().setMaxOccurs(1);

                        if (childItem == null) {
                            childItem = parent.getNewValue().findOrCreateContainer(name);
                        }

//                        childItem = definitionFixed.instantiate();
                        childItem.applyDefinition(definitionFixed, true);
//                        parent.getNewValue().addReplaceExisting(childItem);
                    }
                } catch (Exception e) {
                    LOGGER.error("Couldn't get connector from reference " + connectorRef, e);
                }
            }
        }

        if (childItem == null) {
            childItem = parent.getNewValue().findOrCreateContainer(name);
        }

        return createWrapper(parent, childItem, status, context);
    }

    protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper,
            WrapperContext context, List<ItemWrapper<?,?>> wrappers) throws SchemaException {
        if (def.isMandatory()) {
            def.toMutable().toMutable().setEmphasized(true);
            def.toMutable().setDisplayOrder(50);
        }

        ItemWrapper<?,?> wrapper = createChildWrapper(def, containerValueWrapper, context);

        if (wrapper != null) {
            wrappers.add(wrapper);
        }
    }
}
