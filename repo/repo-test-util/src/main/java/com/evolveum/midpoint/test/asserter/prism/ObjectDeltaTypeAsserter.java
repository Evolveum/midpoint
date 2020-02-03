/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 */
public class ObjectDeltaTypeAsserter<RA> extends AbstractAsserter<RA> {

    private ObjectDeltaType delta;

    public ObjectDeltaTypeAsserter(ObjectDeltaType delta) {
        super();
        this.delta = delta;
    }

    public ObjectDeltaTypeAsserter(ObjectDeltaType delta, String details) {
        super(details);
        this.delta = delta;
    }

    public ObjectDeltaTypeAsserter(ObjectDeltaType delta, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.delta = delta;
    }

    public static ObjectDeltaTypeAsserter<Void> forDelta(ObjectDeltaType delta) {
        return new ObjectDeltaTypeAsserter<>(delta);
    }

    public static ObjectDeltaTypeAsserter<Void> forDelta(ObjectDeltaType delta, String details) {
        return new ObjectDeltaTypeAsserter<>(delta, details);
    }

    public ObjectDeltaTypeAsserter<RA> assertAdd() {
        assertChangeType(ChangeTypeType.ADD);
        return this;
    }

    public ObjectDeltaTypeAsserter<RA> assertModify() {
        assertChangeType(ChangeTypeType.MODIFY);
        return this;
    }

    public ObjectDeltaTypeAsserter<RA> assertDelete() {
        assertChangeType(ChangeTypeType.DELETE);
        return this;
    }

    public ObjectDeltaTypeAsserter<RA> assertChangeType(ChangeTypeType expected) {
        assertEquals("Wrong change type in "+desc(), expected, delta.getChangeType());
        return this;
    }

    public ObjectDeltaTypeAsserter<RA> assertHasModification(ItemPath itemPath) {
        for (ItemDeltaType itemDelta: delta.getItemDelta()) {
            if (itemPath.equivalent(itemDelta.getPath().getItemPath())) {
                return this;
            }
        }
        fail("No modification for "+itemPath+" in "+desc());
        return null; // not reached
    }

    public ObjectDeltaTypeAsserter<RA> assertNoModification(ItemPath itemPath) {
        for (ItemDeltaType itemDelta: delta.getItemDelta()) {
            if (itemPath.equivalent(itemDelta.getPath().getItemPath())) {
                fail("Unexpected modification for "+itemPath+" in "+desc());
            }
        }
        return this;
    }

    protected String desc() {
        return descWithDetails(delta);
    }

    public ObjectDeltaTypeAsserter<RA> display() {
        if (getDetails() != null) {
            display(getDetails());
        } else {
            display("ObjectDeltaType");
        }
        return this;
    }

    public ObjectDeltaTypeAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, PrettyPrinter.debugDump(delta, 1));
        return this;
    }
}
