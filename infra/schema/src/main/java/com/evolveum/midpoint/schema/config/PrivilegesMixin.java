/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionPrivilegesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public interface PrivilegesMixin<T extends Serializable & Cloneable> extends ConfigurationItemable<T> {

    default @Nullable ExecutionPrivilegesSpecificationType getPrivileges(
            @Nullable ObjectReferenceType legacyRunAsRef,
            @Nullable ExecutionPrivilegesSpecificationType privileges) throws ConfigurationException {
        if (privileges != null) {
            if (legacyRunAsRef != null) {
                throw new ConfigurationException(
                        "Both privileges and legacy runAsRef are present in " + fullDescription());
            } else {
                return privileges;
            }
        } else {
            if (legacyRunAsRef != null) {
                return new ExecutionPrivilegesSpecificationType()
                        .runAsRef(legacyRunAsRef.clone());
            } else {
                return null;
            }
        }
    }
}
