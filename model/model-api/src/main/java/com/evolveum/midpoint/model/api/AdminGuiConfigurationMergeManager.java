/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface AdminGuiConfigurationMergeManager {

    List<ContainerPanelConfigurationType> mergeContainerPanelConfigurationType(List<ContainerPanelConfigurationType> defaultPanels, List<ContainerPanelConfigurationType> configuredPanels);

    GuiObjectDetailsPageType mergeObjectDetailsPageConfiguration(GuiObjectDetailsPageType defaultPageConfiguration, ArchetypePolicyType archetypePolicyType, OperationResult result) throws SchemaException, ConfigurationException;

    <DP extends GuiObjectDetailsPageType> DP mergeObjectDetailsPageConfiguration(DP defaultPageConfiguration, DP compiledPageType);

    GuiShadowDetailsPageType mergeShadowDetailsPageConfiguration(GuiShadowDetailsPageType defaultPageConfiguration, GuiShadowDetailsPageType compiledPageType);

    List<VirtualContainersSpecificationType> mergeVirtualContainers(GuiObjectDetailsPageType currentObjectDetails, GuiObjectDetailsPageType superObjectDetails);

    <C extends Containerable> List<C> mergeContainers(List<C> currentContainers, List<C> superContainers, Function<C, Predicate<C>> predicate, BiFunction<C, C, C> mergeFunction);

    DisplayType mergeDisplayType(DisplayType currentDisplayType, DisplayType superDisplayType);
}
