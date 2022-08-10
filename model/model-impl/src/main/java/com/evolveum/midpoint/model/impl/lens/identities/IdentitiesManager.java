/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.FocusIdentityTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitySourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

/**
 * PRELIMINARY/LIMITED IMPLEMENTATION
 */
@Component
public class IdentitiesManager {

    /** See {@link MidpointFunctions#selectIdentityItemValues(Collection, FocusIdentitySourceType, ItemPath)}. */
    public @NotNull Collection<PrismValue> selectIdentityItemValue(
            @Nullable Collection<FocusIdentityType> identities,
            @Nullable FocusIdentitySourceType source,
            @NotNull ItemPath itemPath) {

        Set<PrismValue> selected = new HashSet<>();
        for (FocusIdentityType identityBean : emptyIfNull(identities)) {
            if (source != null) {
                if (!FocusIdentityTypeUtil.matches(identityBean, source)) {
                    continue;
                }
            } else {
                // null source means "any non-own"
                if (FocusIdentityTypeUtil.isOwn(identityBean)) {
                    continue;
                }
            }
            PrismProperty<?> property = identityBean.asPrismContainerValue().findProperty(
                    ItemPath.create(FocusIdentityType.F_DATA, itemPath));
            if (property != null) {
                selected.addAll(property.getClonedValues());
            }
        }
        return selected;
    }

    public static IdentityManagementConfiguration createIdentityConfiguration(ObjectTemplateType objectTemplate)
            throws ConfigurationException {
        return IdentityManagementConfigurationImpl.of(objectTemplate);
    }
}
