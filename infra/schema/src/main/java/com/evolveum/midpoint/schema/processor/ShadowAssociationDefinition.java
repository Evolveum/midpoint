/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.List;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.schema.config.InboundMappingConfigItem;
import com.evolveum.midpoint.schema.config.MappingConfigItem;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition of an association item, e.g., `ri:group`.
 *
 * @see ShadowItemDefinition
 */
public interface ShadowAssociationDefinition
        extends
        PrismContainerDefinition<ShadowAssociationValueType>,
        ShadowItemDefinition<ShadowAssociation, ShadowAssociationValueType> {

    /** Creates a filter that provides all shadows eligible as the target value for this association. */
    ObjectFilter createTargetObjectsFilter();

    ResourceObjectDefinition getTargetObjectDefinition();

    ContainerDelta<ShadowAssociationValueType> createEmptyDelta();

    boolean isSimulated();

    ShadowAssociationClassSimulationDefinition getSimulationDefinition();

    ShadowAssociationClassSimulationDefinition getSimulationDefinitionRequired();

    ShadowAssociationClassDefinition getAssociationClassDefinition();

    boolean isEntitlement();

    // TODO move the following methods to ShadowItemDefinition
    MappingConfigItem getOutboundMapping() throws ConfigurationException;
    List<InboundMappingConfigItem> getInboundMappings() throws ConfigurationException;
}
