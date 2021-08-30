/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceFeatureType;

import org.assertj.core.api.Assertions;

public class UserInterfaceFeatureAsserter<RA, F extends UserInterfaceFeatureType> extends AbstractAsserter<RA> {

    private final F feature;

    public UserInterfaceFeatureAsserter(F feature, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.feature = feature;
    }

    public UserInterfaceFeatureAsserter<RA, F> identifier(String identifier) {
        Assertions.assertThat(feature.getIdentifier()).isEqualTo(identifier);
        return this;
    }

    public UserInterfaceFeatureAsserter<RA, F> visibility(UserInterfaceElementVisibilityType visibility) {
        Assertions.assertThat(feature.getVisibility()).isEqualTo(visibility);
        return this;
    }

    public DisplayTypeAsserter<UserInterfaceFeatureAsserter<RA, F>> displayType() {
        return new DisplayTypeAsserter<>(feature.getDisplay(), this, "from virtual container " + feature);
    }

    public UserInterfaceFeatureAsserter<RA, F> assertDisplayOrder(int order) {
        Assertions.assertThat(feature.getDisplayOrder()).isEqualTo(order);
        return this;
    }

    @Override
    protected String desc() {
        return "virtual containers";
    }

    public F getFeature() {
        return feature;
    }
}
