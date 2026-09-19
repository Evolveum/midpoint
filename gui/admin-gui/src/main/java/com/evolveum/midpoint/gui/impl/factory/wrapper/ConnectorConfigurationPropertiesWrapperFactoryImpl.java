/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.util.ConnectorConfigurationGroupingUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.springframework.stereotype.Component;

/**
 * Wrapper factory for the ICF {@code configurationProperties} container nested in
 * {@code connectorConfiguration}.
 *
 * <p>It hooks the grouped-section (virtual container) configuration into the wrapper context, so the
 * GUI can render the connector configuration properties as collapsible sections in both the details
 * view and the resource wizard. The virtual containers are materialized by the object wrapper factory
 * (as top-level virtual items) and their items are resolved against the object using the full,
 * object-relative item paths.</p>
 */
@Component
public class ConnectorConfigurationPropertiesWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorConfigurationPropertiesWrapperFactoryImpl.class);

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition
                && SchemaConstants.ICF_CONFIGURATION_PROPERTIES_NAME.equals(def.getItemName());
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public PrismContainerWrapper<Containerable> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def,
            WrapperContext context) throws SchemaException {
        forceVirtualContainerConfiguration(parent, def, context);
        return super.createWrapper(parent, def, context);
    }

    private void forceVirtualContainerConfiguration(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) {
        if (parent == null || parent.getPath() == null) {
            return;
        }
        List<ConnectorConfigurationGroupingUtil.Group> groups =
                ConnectorConfigurationGroupingUtil.getConfigurationGroups((PrismContainerDefinition<?>) def);
        if (groups == null || groups.isEmpty()) {
            return;
        }
        ItemPath itemPathPrefix = parent.getPath().append(def.getItemName());
        ContainerPanelConfigurationType configuration =
                ConnectorConfigurationGroupingUtil.createContainerPanelConfiguration(
                        (PrismContainerDefinition<?>) def, groups, itemPathPrefix);
        LOGGER.trace("Hooking up {} virtual containers for {}", configuration.getContainer().size(), def.getItemName());
        context.forceCreateVirtualContainer(configuration.getContainer());
    }
}
