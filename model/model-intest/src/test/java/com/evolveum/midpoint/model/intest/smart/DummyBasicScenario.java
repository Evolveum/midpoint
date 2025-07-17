/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import static com.evolveum.midpoint.test.ObjectClassName.*;

import com.evolveum.icf.dummy.resource.DummyObjectClass;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.test.AbstractDummyScenario;
import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.ObjectClassName;

import java.time.ZonedDateTime;

/**
 * Represents the basic scenario for smart integration service integration tests.
 *
 * Assumes non-legacy schema when used by midPoint resource.
 */
@SuppressWarnings("WeakerAccess")
public class DummyBasicScenario extends AbstractDummyScenario {

    private DummyBasicScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public final Account account = new Account();
    public final Group group = new Group();
    public final Privilege privilege = new Privilege();
    public final OrganizationalUnit organizationalUnit = new OrganizationalUnit();

    public static DummyBasicScenario on(DummyResourceContoller controller) {
        return new DummyBasicScenario(controller);
    }

    public DummyBasicScenario initialize() {
        account.initialize();
        //group.initialize(); // TODO reenable (currently fails)
        //privilege.initialize();
        organizationalUnit.initialize();
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
            public static final AttrName TYPE = AttrName.ri("type");
            public static final AttrName DEPARTMENT = AttrName.ri("department");
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
            controller.addAttrDef(oc, AttributeNames.TYPE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.DEPARTMENT.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.CREATED.local(), ZonedDateTime.class, false, false);
            controller.addAttrDef(oc, AttributeNames.LAST_LOGIN.local(), ZonedDateTime.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Group extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("group");

        public static class AttributeNames {
            public static final AttrName DESCRIPTION = AttrName.ri("description");
            public static final AttrName DISPLAY_NAME = AttrName.ri("displayName");
            public static final AttrName TYPE = AttrName.ri("type");
            public static final AttrName OWNER = AttrName.ri("owner");
        }

        void initialize() {
            var oc = controller.getDummyResource().getAccountObjectClass();
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.DISPLAY_NAME.local(), String.class, true, false);
            controller.addAttrDef(oc, AttributeNames.TYPE.local(), String.class, true, false);
            controller.addAttrDef(oc, AttributeNames.OWNER.local(), String.class, true, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class Privilege extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("privilege");

        public static class AttributeNames {
            public static final AttrName NAME = AttrName.ri("name");
            public static final AttrName DESCRIPTION = AttrName.ri("description");
            public static final AttrName LEVEL = AttrName.ri("level");
            public static final AttrName ASSIGNED_TO = AttrName.ri("assignedTo");
        }

        void initialize() {
            var oc = controller.getDummyResource().getPrivilegeObjectClass();
            controller.addAttrDef(oc, AttributeNames.NAME.local(), String.class, true, false);
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.LEVEL.local(), Integer.class, false, false);
            controller.addAttrDef(oc, AttributeNames.ASSIGNED_TO.local(), String.class, true, true);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }

    public class OrganizationalUnit extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("organizationalUnit");

        public static class AttributeNames {
            public static final AttrName NAME = AttrName.ri("name");
            public static final AttrName CN = AttrName.ri("cn");
            public static final AttrName DESCRIPTION = AttrName.ri("description");
            public static final AttrName MANAGER = AttrName.ri("manager");
            public static final AttrName LOCATION = AttrName.ri("location");
            public static final AttrName PARENT_UNIT = AttrName.ri("parentUnit");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
            controller.addAttrDef(oc, AttributeNames.NAME.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.CN.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.MANAGER.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.LOCATION.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.PARENT_UNIT.local(), String.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }
}
