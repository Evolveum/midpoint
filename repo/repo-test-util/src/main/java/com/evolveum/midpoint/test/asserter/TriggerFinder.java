/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.testng.AssertJUnit;

import static org.testng.AssertJUnit.assertEquals;

/**
 *
 * Note: considered to align this with ParentOrgRefFinder into some kind of common superclass.
 * But the resulting structure of generics is just too insane. It is lesser evil to have copy&pasted code.
 *
 * @author semancik
 */
public class TriggerFinder<O extends ObjectType, OA extends PrismObjectAsserter<O,RA>, RA> {

    private final TriggersAsserter<O,OA,RA> triggersAsserter;
    private String originDescription;

    public TriggerFinder(TriggersAsserter<O,OA,RA> triggersAsserter) {
        this.triggersAsserter = triggersAsserter;
    }

    public TriggerFinder<O,OA,RA> originDescription(String originDescription) {
        this.originDescription = originDescription;
        return this;
    }

    public TriggerAsserter<TriggersAsserter<O,OA,RA>> find() throws ObjectNotFoundException, SchemaException {
        TriggerType found = null;
        for (TriggerType trigger: triggersAsserter.getTriggers()) {
            if (matches(trigger)) {
                if (found == null) {
                    found = trigger;
                } else {
                    fail("Found more than one trigger that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no trigger that matches search criteria");
        }
        return triggersAsserter.forTrigger(found);
    }

    public TriggersAsserter<O,OA,RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
        int foundCount = 0;
        for (TriggerType trigger: triggersAsserter.getTriggers()) {
            if (matches(trigger)) {
                foundCount++;
            }
        }
        assertEquals("Wrong number of triggers for specified criteria in "+triggersAsserter.desc(), expectedCount, foundCount);
        return triggersAsserter;
    }

    private boolean matches(TriggerType trigger) throws ObjectNotFoundException, SchemaException {

        if (originDescription != null) {
            if (!originDescription.equals(trigger.getOriginDescription())) {
                return false;
            }
        }

        // TODO: more criteria
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
