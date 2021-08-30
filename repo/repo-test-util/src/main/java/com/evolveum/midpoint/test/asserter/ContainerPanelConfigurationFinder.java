/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ContainerPanelConfigurationFinder<RA> extends UserInterfaceFeatureFinder<ContainerPanelConfigurationAsserter<ContainerPanelConfigurationsAsserter<RA>>, ContainerPanelConfigurationType> {

    private ContainerPanelConfigurationsAsserter<RA> containerPanelConfigurationsAsserter;

    public ContainerPanelConfigurationFinder(ContainerPanelConfigurationsAsserter<RA> containerPanelConfigurationsAsserter) {
        this.containerPanelConfigurationsAsserter = containerPanelConfigurationsAsserter;
    }

    @Override
    public ContainerPanelConfigurationFinder<RA> identifier(String identifier) {
        super.identifier(identifier);
        return this;
    }

    @Override
    public ContainerPanelConfigurationFinder<RA> displayName(String displayName) {
        super.displayName(displayName);
        return this;
    }

    @Override
    public ContainerPanelConfigurationAsserter<ContainerPanelConfigurationsAsserter<RA>> find() {
        return super.find();
    }

    @Override
    protected ContainerPanelConfigurationAsserter<ContainerPanelConfigurationsAsserter<RA>> find(Predicate<ContainerPanelConfigurationType> filter) {
        List<ContainerPanelConfigurationType> foundVirtualContainers = containerPanelConfigurationsAsserter.getPanels()
                .stream()
                .filter(filter)
                .collect(Collectors.toList());
        Assertions.assertThat(foundVirtualContainers).hasSize(1);
        return new ContainerPanelConfigurationAsserter<>(foundVirtualContainers.iterator().next(), containerPanelConfigurationsAsserter, "from list of virtual containers " + containerPanelConfigurationsAsserter.getPanels());
    }

}
