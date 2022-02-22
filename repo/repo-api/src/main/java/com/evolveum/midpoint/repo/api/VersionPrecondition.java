/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class VersionPrecondition<T extends ObjectType> implements ModificationPrecondition<T>, Serializable {

    @NotNull private final String expectedVersion;

    public VersionPrecondition(String expectedVersion) {
        this.expectedVersion = normalize(expectedVersion);
    }

    public VersionPrecondition(@NotNull PrismObject<T> object) {
        this.expectedVersion = normalize(object.getVersion());
    }

    @Override
    public boolean holds(PrismObject<T> object) throws PreconditionViolationException {
        String realVersion = normalize(object.getVersion());
        if (!expectedVersion.equals(realVersion)) {
            throw new PreconditionViolationException("Real version of the object (" + object.getVersion()
                    + ") does not match expected one (" + expectedVersion + ") for " + object);
        }
        return true;
    }

    private String normalize(String version) {
        // this is a bit questionable - actually, null version should not occur here
        return version != null ? version : "0";
    }
}
