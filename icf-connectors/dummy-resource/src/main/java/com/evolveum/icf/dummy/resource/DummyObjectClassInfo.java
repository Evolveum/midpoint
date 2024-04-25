/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;

/**
 * Name and definition for an object class.
 * Needed because {@link DummyObjectClass} does not contain its own.
 */
public record DummyObjectClassInfo(
        @NotNull String name,
        @NotNull DummyObjectClass definition) {

    public boolean isAccount() {
        return DummyAccount.OBJECT_CLASS_NAME.equals(name);
    }

    public boolean isGroup() {
        return DummyGroup.OBJECT_CLASS_NAME.equals(name);
    }
}
