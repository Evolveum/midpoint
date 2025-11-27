/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Arrays;
import java.util.List;

/**
 * A known properties that can serve as a correlator for an object.
 * They are sorted according to their preference, i.e. the first one is the most preferred.
 */
public enum KnownCorrelator {

    FOCUS_NAME(FocusType.F_NAME, FocusType.class),
    USER_PERSONAL_NUMBER(UserType.F_PERSONAL_NUMBER, UserType.class),
    ROLE_IDENTIFIER(AbstractRoleType.F_IDENTIFIER, AbstractRoleType.class),
    FOCUS_EMAIL_ADDRESS(FocusType.F_EMAIL_ADDRESS, FocusType.class);

    private final ItemName itemName;
    private final Class<? extends FocusType> focusClass;

    KnownCorrelator(ItemName itemName, Class<? extends FocusType> focusClass) {
        this.itemName = itemName;
        this.focusClass = focusClass;
    }

    static List<? extends ItemPath> getAllFor(Class<?> focusClass) {
        return Arrays.stream(values())
                .filter(v -> v.focusClass.isAssignableFrom(focusClass))
                .map(v -> v.itemName)
                .toList();
    }

    public ItemName getItemName() {
        return itemName;
    }
}
