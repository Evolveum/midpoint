/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
