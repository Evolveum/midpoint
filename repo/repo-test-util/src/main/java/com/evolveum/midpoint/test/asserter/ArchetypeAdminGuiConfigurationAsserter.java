/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeAdminGuiConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;

import org.assertj.core.api.Assertions;

public class ArchetypeAdminGuiConfigurationAsserter<RA> extends AbstractAsserter<RA> {

    private ArchetypeAdminGuiConfigurationType archetypeAdminGuiConfig;

    public ArchetypeAdminGuiConfigurationAsserter(ArchetypeAdminGuiConfigurationType archetypeAdminGuiConfig, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.archetypeAdminGuiConfig = archetypeAdminGuiConfig;
    }

    public GuiObjectDetailsPageAsserter<ArchetypeAdminGuiConfigurationAsserter<RA>> objectDetails() {
        Assertions.assertThat(archetypeAdminGuiConfig).isNotNull();
        GuiObjectDetailsPageType details = archetypeAdminGuiConfig.getObjectDetails();
        return new GuiObjectDetailsPageAsserter<>(details, this, "from archetype admin gui " + archetypeAdminGuiConfig);
    }

    @Override
    protected String desc() {
        return "archetype admin gui config";
    }
}
