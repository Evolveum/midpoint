package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.assertj.core.api.Assertions;

import java.util.List;

public class ContainerPanelConfigurationsAsserter<RA> extends AbstractAsserter<RA> {

    private List<ContainerPanelConfigurationType> panels;

    public ContainerPanelConfigurationsAsserter(List<ContainerPanelConfigurationType> panels, RA returnAsserters, String details) {
        super(returnAsserters, details);
        this.panels = panels;
    }


    public ContainerPanelConfigurationsAsserter<RA> assertSize(int expectedPanels) {
        Assertions.assertThat(panels).hasSize(expectedPanels);
        return this;
    }

    List<ContainerPanelConfigurationType> getPanels() {
        Assertions.assertThat(panels).isNotEmpty();
        return panels;
    }

    public ContainerPanelConfigurationFinder<RA> by() {
        return new ContainerPanelConfigurationFinder<>(this);
    }

    public ContainerPanelConfigurationAsserter<ContainerPanelConfigurationsAsserter<RA>> byIdentifier(String identifier) {
        return by().identifier(identifier).find();
    }

    public ContainerPanelConfigurationAsserter<ContainerPanelConfigurationsAsserter<RA>> byDisplayName(String displayName) {
        return by().displayName(displayName).find();
    }

    @Override
    protected String desc() {
        return "container panel configuration";
    }
}
