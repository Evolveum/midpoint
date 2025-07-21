/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.test.AbstractDummyScenario;
import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.ObjectClassName;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.test.ObjectClassName.custom;

/**
 * Represents the HR resource for smart integration service integration tests.
 *
 * Assumes non-legacy schema when used by midPoint resource.
 */
@SuppressWarnings("WeakerAccess")
public class DummyHrScenario extends AbstractDummyScenario {

    private DummyHrScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public final Person person = new Person();
    public final Department department = new Department();

    public static DummyHrScenario on(DummyResourceContoller controller) {
        return new DummyHrScenario(controller);
    }

    public DummyHrScenario initialize() {
        person.initialize();
        department.initialize();
        return this;
    }

    public class Person extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("person");

        public static class AttributeNames {
            public static final AttrName NUMBER = AttrName.ri("number");
            public static final AttrName FULLNAME = AttrName.ri(DummyAccount.ATTR_FULLNAME_NAME);
            //public static final AttrName EMAIL = AttrName.ri("email");
            //public static final AttrName PHONE = AttrName.ri("phone");
            //public static final AttrName STATUS = AttrName.ri("status");
            //public static final AttrName TYPE = AttrName.ri("type");
            //public static final AttrName VALID_FROM = AttrName.ri("validFrom");
            //public static final AttrName VALID_TO = AttrName.ri("validTo");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
            controller.addAttrDef(oc, AttributeNames.NUMBER.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.FULLNAME.local(), String.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Department extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("department");

        public static class AttributeNames {
            public static final AttrName CODE = AttrName.ri("code");
            public static final AttrName NAME = AttrName.ri("name");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
            controller.addAttrDef(oc, AttributeNames.CODE.local(), String.class, true, false);
            controller.addAttrDef(oc, AttributeNames.NAME.local(), String.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    // TODO add link between person and department
}
