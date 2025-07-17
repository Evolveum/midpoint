/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.test.ObjectClassName.custom;

import java.time.ZonedDateTime;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.test.AbstractDummyScenario;
import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.ObjectClassName;

/**
 * Represents the basic scenario for smart integration service integration tests.
 *
 * Assumes non-legacy schema when used by midPoint resource.
 */
@SuppressWarnings("WeakerAccess")
public class DummyScenario extends AbstractDummyScenario {

    private DummyScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public final Account account = new Account();

    public static DummyScenario on(DummyResourceContoller controller) {
        return new DummyScenario(controller);
    }

    public DummyScenario initialize() {
        account.initialize();
        return this;
    }

    public class Account extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("account");

        public static class AttributeNames {
            public static final AttrName FULLNAME = AttrName.ri(DummyAccount.ATTR_FULLNAME_NAME);
            public static final AttrName DESCRIPTION = AttrName.ri(DummyAccount.ATTR_DESCRIPTION_NAME);
            public static final AttrName PERSONAL_NUMBER = AttrName.ri("personalNumber");
            public static final AttrName EMAIL = AttrName.ri("email");
            public static final AttrName PHONE = AttrName.ri("phone");
            public static final AttrName STATUS = AttrName.ri("status");
            public static final AttrName CREATED = AttrName.ri("created");
            public static final AttrName LAST_LOGIN = AttrName.ri("lastLogin");
        }

        void initialize() {
            var oc = controller.getDummyResource().getAccountObjectClass();
            controller.addAttrDef(oc, AttributeNames.FULLNAME.local(), String.class, true, false);
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.PERSONAL_NUMBER.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.EMAIL.local(), String.class, true, false);
            controller.addAttrDef(oc, AttributeNames.PHONE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.STATUS.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.CREATED.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, AttributeNames.LAST_LOGIN.local(), ZonedDateTime.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }
}
