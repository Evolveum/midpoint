/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SuperObjectTypeReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

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

    public static @Nullable QName getObjectClassName(@NotNull ResourceObjectTypeDefinitionType bean) {
        ResourceObjectTypeDelineationType delineation = bean.getDelineation();
        if (delineation != null) {
            QName objectClassName = delineation.getObjectClass();
            if (objectClassName != null) {
                return objectClassName;
            }
        }
        return bean.getObjectClass();
    }

    public static @NotNull List<QName> getAuxiliaryObjectClassNames(@NotNull ResourceObjectTypeDefinitionType bean) {
        ResourceObjectTypeDelineationType delineation = bean.getDelineation();
        if (delineation != null) {
            List<QName> auxiliaryObjectClasses = delineation.getAuxiliaryObjectClass();
            if (!auxiliaryObjectClasses.isEmpty()) {
                return auxiliaryObjectClasses;
            }
        }
        return bean.getAuxiliaryObjectClass();
    }

    /** Reference to a super-type of an object type. */
    public static abstract class SuperReference {

        static class ByName extends SuperReference {

            @NotNull private final ShadowKindType kind;
            @NotNull private final String intent;

            ByName(@NotNull ShadowKindType kind, @NotNull String intent) {
                this.kind = kind;
                this.intent = intent;
            }

            @Override
            public boolean matches(@NotNull ResourceObjectTypeDefinitionType bean) {
                return ResourceObjectTypeDefinitionTypeUtil.matches(bean, kind, intent);
            }

            @Override
            public String toString() {
                return "Supertype reference by name (" + kind + "/" + intent + ")";
            }
        }

        static class ById extends SuperReference {

            private final long id;

            ById(long id) {
                this.id = id;
            }

            @Override
            public boolean matches(@NotNull ResourceObjectTypeDefinitionType bean) {
                Long internalId = bean.getInternalId();
                return internalId != null && id == internalId;
            }

            @Override
            public String toString() {
                return "Supertype reference by internal ID (" + id + ")";
            }
        }

        // TODO improve error messages
        public static @NotNull SuperReference of(@NotNull SuperObjectTypeReferenceType bean) throws ConfigurationException {
            ShadowKindType kind = bean.getKind();
            String intent = bean.getIntent();
            Long internalId = bean.getInternalId();
            if (internalId != null) {
                configCheck(kind == null, "Kind cannot be specified with internalId in %s", bean);
                configCheck(intent == null, "Intent cannot be specified with internalId in %s", bean);
                return new ById(internalId);
            } else {
                configCheck(kind != null, "Kind must be specified in %s", bean);
                configCheck(intent != null, "Intent must be specified in %s", bean);
                return new ByName(kind, intent);
            }
        }

        public abstract boolean matches(@NotNull ResourceObjectTypeDefinitionType bean);
    }
}
