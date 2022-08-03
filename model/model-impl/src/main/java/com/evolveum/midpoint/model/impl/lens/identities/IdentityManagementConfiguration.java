/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdentityItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Wraps all the configuration related to management of `identities` container, correlation, and so on.
 *
 * PRELIMINARY VERSION - e.g. no support for object template inclusion, etc
 */
public class IdentityManagementConfiguration {

    @NotNull private final ObjectTemplateType objectTemplate;

    private IdentityManagementConfiguration(@NotNull ObjectTemplateType objectTemplate) {
        this.objectTemplate = objectTemplate;
    }

    public static IdentityManagementConfiguration of(@Nullable ObjectTemplateType objectTemplate) {
        return new IdentityManagementConfiguration(
                Objects.requireNonNullElseGet(
                        objectTemplate,
                        ObjectTemplateType::new));
    }

    public @NotNull Collection<IdentityItemConfiguration> getItems() throws ConfigurationException {
        List<IdentityItemConfiguration> itemConfigurationList = new ArrayList<>();
        for (ObjectTemplateItemDefinitionType itemDefBean : objectTemplate.getItem()) {
            IdentityItemDefinitionType identityDefBean = itemDefBean.getIdentity();
            if (identityDefBean != null) {
                itemConfigurationList.add(
                        IdentityItemConfiguration.of(itemDefBean, identityDefBean));
            }
        }
        return itemConfigurationList;
    }

    public @Nullable IdentityItemConfiguration getForPath(@NotNull ItemPath path) throws ConfigurationException {
        for (ObjectTemplateItemDefinitionType itemDefBean : objectTemplate.getItem()) {
            IdentityItemDefinitionType identityBean = itemDefBean.getIdentity();
            if (identityBean != null) {
                ItemPathType ref = itemDefBean.getRef();
                if (ref != null && ref.getItemPath().equivalent(path)) {
                    return IdentityItemConfiguration.of(itemDefBean, identityBean);
                }
            }
        }
        return null;
    }

    // TODO improve --- TODO what if empty config is legal?
    public boolean hasNoItems() throws ConfigurationException {
        return getItems().isEmpty();
    }
}
