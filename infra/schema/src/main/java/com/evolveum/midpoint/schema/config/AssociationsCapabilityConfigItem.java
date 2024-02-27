/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AssociationsCapabilityType;

public class AssociationsCapabilityConfigItem
        extends ConfigurationItem<AssociationsCapabilityType> {

    @SuppressWarnings("unused") // called dynamically
    public AssociationsCapabilityConfigItem(@NotNull ConfigurationItem<AssociationsCapabilityType> original) {
        super(original);
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull List<SimulatedAssociationClassConfigItem> getAssociationClasses() {
        return children(
                value().getAssociationClass(),
                SimulatedAssociationClassConfigItem.class,
                AssociationsCapabilityType.F_ASSOCIATION_CLASS);
    }

    public SimulatedAssociationClassConfigItem getAssociationClass(@NotNull QName className) throws ConfigurationException {
        List<SimulatedAssociationClassConfigItem> matching = new ArrayList<>();
        for (SimulatedAssociationClassConfigItem ac : getAssociationClasses()) {
            if (QNameUtil.match(className, ac.getName())) {
                matching.add(ac);
            }
        }
        return single(matching, "Multiple definitions for simulated association '%s' in %s", className, DESC);
    }
}
