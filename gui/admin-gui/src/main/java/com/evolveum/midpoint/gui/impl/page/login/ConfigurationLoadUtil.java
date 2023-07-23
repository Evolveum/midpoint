/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeSelectionModuleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.jetbrains.annotations.NotNull;


public class ConfigurationLoadUtil {

    public static ArchetypeSelectionModuleType loadArchetypeSelectionModuleForLoginRecovery(
            @NotNull Component component, @NotNull SecurityPolicyType securityPolicy) {
        var session = component.getSession();

        var loginRecoveryPolicy = securityPolicy.getLoginNameRecovery();
        var sequenceIdentifier = loginRecoveryPolicy.getAuthenticationSequenceIdentifier();
        var loginRecoverySequence = SecurityUtils.getSequenceByIdentifier(sequenceIdentifier, securityPolicy.getAuthentication());
        var archetypeSelectionModule = loginRecoverySequence.getModule()
                .stream()
                .filter(m -> getArchetypeSelectionModuleByIdentifier(component, m.getIdentifier(), securityPolicy) != null)
                .findFirst()
                .orElse(null);
        if (archetypeSelectionModule == null) {
            session.error(component.getString("No archetype selection module is found"));
            throw new RestartResponseException(PageError.class);
        }
        return getArchetypeSelectionModuleByIdentifier(component, archetypeSelectionModule.getIdentifier(), securityPolicy);
    }

    private static ArchetypeSelectionModuleType getArchetypeSelectionModuleByIdentifier(@NotNull Component component,
            String moduleIdentifier, SecurityPolicyType securityPolicy) {
        if (StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }
        if (securityPolicy == null || securityPolicy.getAuthentication() == null) {
            component.getSession().error(component.getString("Security policy not found"));
            throw new RestartResponseException(PageError.class);
        }
        return securityPolicy.getAuthentication().getModules().getArchetypeSelection()
                .stream()
                .filter(m -> moduleIdentifier.equals(m.getIdentifier()) || moduleIdentifier.equals(m.getName()))
                .findFirst()
                .orElse(null);
    }



}
