/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.configItem;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ResourceObjectTypeDefinitionConfigItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Helps with {@link ResourceObjectTypeDefinitionType} objects.
 */
public class ResourceObjectTypeDefinitionTypeUtil {

    public static @NotNull ShadowKindType getKind(@NotNull ResourceObjectTypeDefinitionType bean) {
        return Objects.requireNonNullElse(bean.getKind(), ShadowKindType.ACCOUNT);
    }

    public static @NotNull String getIntent(@NotNull ResourceObjectTypeDefinitionType bean) {
        return Objects.requireNonNullElse(bean.getIntent(), SchemaConstants.INTENT_DEFAULT);
    }

    public static boolean matches(
            @NotNull ResourceObjectTypeDefinitionType bean,
            @NotNull ShadowKindType kind,
            @NotNull String intent) {
        return getKind(bean) == kind && getIntent(bean).equals(intent);
    }

    @Deprecated // use the config item instead
    public static @Nullable QName getObjectClassName(@NotNull ResourceObjectTypeDefinitionType bean) {
        ResourceObjectTypeDelineationType delineation = bean.getDelineation();
        QName newName = delineation != null ? delineation.getObjectClass() : null;
        QName legacyName = bean.getObjectClass();
        // This should be a configuration exception; but let's not bother all the callers with handling it.
        stateCheck(newName == null || legacyName == null || QNameUtil.match(newName, legacyName),
                "Contradicting legacy (%s) vs delineation-based (%s) object class names in %s",
                legacyName, newName, bean);
        return MiscUtil.getFirstNonNull(newName, legacyName);
    }

    @Deprecated // use the CI directly
    public static @NotNull List<QName> getAuxiliaryObjectClassNames(@NotNull ResourceObjectTypeDefinitionType bean) {
        try {
            return configItem(bean, ConfigurationItemOrigin.undeterminedSafe(), ResourceObjectTypeDefinitionConfigItem.class)
                    .getAuxiliaryObjectClassNames();
        } catch (ConfigurationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Reference to a super-type of an object type. */
    public static class SuperReference {

        @NotNull private final ShadowKindType kind;
        @NotNull private final String intent;

        SuperReference(@NotNull ShadowKindType kind, @NotNull String intent) {
            this.kind = kind;
            this.intent = intent;
        }

        public boolean matches(@NotNull ResourceObjectTypeDefinitionType bean) {
            return ResourceObjectTypeDefinitionTypeUtil.matches(bean, kind, intent);
        }

        @Override
        public String toString() {
            return "Supertype reference by name (" + kind + "/" + intent + ")";
        }

        // TODO improve error messages
        public static @NotNull SuperReference of(@NotNull ResourceObjectTypeIdentificationType bean) throws ConfigurationException {
            ShadowKindType kind = bean.getKind();
            String intent = bean.getIntent();
            configCheck(kind != null, "Kind must be specified in %s", bean);
            configCheck(intent != null, "Intent must be specified in %s", bean);
            return new SuperReference(kind, intent);
        }
    }
}
