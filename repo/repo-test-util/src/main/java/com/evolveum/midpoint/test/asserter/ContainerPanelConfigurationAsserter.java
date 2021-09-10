/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.assertj.core.api.Assertions;

public class ContainerPanelConfigurationAsserter<RA> extends UserInterfaceFeatureAsserter<RA, ContainerPanelConfigurationType> {

    public ContainerPanelConfigurationAsserter(ContainerPanelConfigurationType panelConfiguration, RA returnAsserter, String details) {
        super(panelConfiguration, returnAsserter, details);
    }

    public ContainerPanelConfigurationAsserter<RA> identifier(String identifier) {
        super.identifier(identifier);
        return this;
    }

    public ContainerPanelConfigurationAsserter<RA> visibility(UserInterfaceElementVisibilityType visibility) {
        super.visibility(visibility);
        return this;
    }

    public ContainerPanelConfigurationAsserter<RA> assertDisplayOrder(int order) {
        super.assertDisplayOrder(order);
        return this;
    }

    public VirtualContainersSpecificationAsserter<ContainerPanelConfigurationAsserter<RA>> container() {
        return new VirtualContainersSpecificationAsserter<>(getFeature().getContainer(), this, "from container panel configuration " + getFeature());
    }

    @Override
    protected String desc() {
        return "virtual containers";
    }

}
