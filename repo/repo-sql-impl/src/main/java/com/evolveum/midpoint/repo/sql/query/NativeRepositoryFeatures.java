/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ImmutableMultimap;

class NativeRepositoryFeatures {

    private static ImmutableMultimap<Class<?>,ItemName> differences = ImmutableMultimap.<Class<?>,ItemName>builder()
            .putAll(ResourceType.class, ResourceType.F_ABSTRACT, ResourceType.F_SUPER)
            .putAll(ConnectorType.class, ConnectorType.F_DISPLAY_NAME)
            .putAll(ShadowType.class, ShadowType.F_CORRELATION)
            .putAll(UserType.class, UserType.F_PERSONAL_NUMBER)
            .putAll(ObjectType.class, ObjectType.F_EFFECTIVE_MARK_REF)
            .build();

    private static ImmutableMultimap<Class<?>,ItemName> supportedOnlyOnNative = null;

    static boolean isSupported(Class<?> schemaType, ItemName firstName) {
        var itemPaths = supported().get(schemaType);
        return QNameUtil.contains(itemPaths, firstName);
    }

    private static ImmutableMultimap<Class<?>,ItemName>  supported() {
        if (supportedOnlyOnNative == null) {
            var builder = ImmutableMultimap.<Class<?>,ItemName>builder();
            for (var t : ObjectTypes.getAllObjectTypes()) {
                for (var v : differences.entries()) {
                    if (v.getKey().isAssignableFrom(t)) {
                        builder.putAll(t, v.getValue());
                    }
                }
            }
            supportedOnlyOnNative = builder.build();
        }
        return supportedOnlyOnNative;
    }

}
