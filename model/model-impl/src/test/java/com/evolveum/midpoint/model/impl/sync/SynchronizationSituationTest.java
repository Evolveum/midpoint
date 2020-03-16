/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
public class SynchronizationSituationTest extends AbstractUnitTest {

    @Test
    public void nullUser() {
        SynchronizationSituation situation = new SynchronizationSituation(null, null,
                SynchronizationSituationType.UNMATCHED);
        AssertJUnit.assertNotNull(situation);
        AssertJUnit.assertNull(situation.getCorrelatedOwner());
        AssertJUnit.assertEquals(SynchronizationSituationType.UNMATCHED, situation.getSituation());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullSituation() {
        new SynchronizationSituation(new UserType(), null, null);
    }

    @Test
    public void correct() {
        UserType user = new UserType();
        SynchronizationSituation situation = new SynchronizationSituation(null, user,
                SynchronizationSituationType.UNMATCHED);
        AssertJUnit.assertNotNull(situation);
        AssertJUnit.assertEquals(user, situation.getCorrelatedOwner());
        AssertJUnit.assertEquals(SynchronizationSituationType.UNMATCHED, situation.getSituation());
    }
}
