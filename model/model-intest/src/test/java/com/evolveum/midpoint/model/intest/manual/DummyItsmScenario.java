/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.manual;

import static com.evolveum.midpoint.test.AttrName.icfsName;
import static com.evolveum.midpoint.test.AttrName.ri;
import static com.evolveum.midpoint.test.ObjectClassName.custom;

import org.jetbrains.annotations.NotNull;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.test.AbstractDummyScenario;
import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.ObjectClassName;

/** Represents a dummy ITSM system. */
@SuppressWarnings("WeakerAccess") // there are a lot of constants that will be eventually used from the outside
public class DummyItsmScenario extends AbstractDummyScenario {

    public final Ticket ticket = new Ticket();

    private DummyItsmScenario(@NotNull DummyResourceContoller controller) {
        super(controller);
    }

    public static DummyItsmScenario on(DummyResourceContoller controller) {
        return new DummyItsmScenario(controller);
    }

    public DummyItsmScenario initialize() {
        ticket.initialize();
        return this;
    }

    public class Ticket extends ScenarioObjectClass {

        public static final ObjectClassName OBJECT_CLASS_NAME = custom("ticket");

        public static class AttributeNames {
            public static final AttrName NAME = icfsName();
            public static final AttrName TITLE = ri("title");
            public static final AttrName DESCRIPTION = ri("description");
            public static final AttrName STATE = ri("state");
            public static final AttrName OUTCOME = ri("outcome");
        }

        void initialize() {
            var oc = DummyObjectClass.standard();
            controller.getDummyResource().addStructuralObjectClass(OBJECT_CLASS_NAME.local(), oc);
            controller.addAttrDef(oc, AttributeNames.TITLE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.DESCRIPTION.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.STATE.local(), String.class, false, false);
            controller.addAttrDef(oc, AttributeNames.OUTCOME.local(), String.class, false, false);
        }

        @Override
        public @NotNull ObjectClassName getObjectClassName() {
            return OBJECT_CLASS_NAME;
        }
    }
}
