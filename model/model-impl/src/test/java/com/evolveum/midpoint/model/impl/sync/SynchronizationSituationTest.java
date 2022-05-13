/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
public class SynchronizationSituationTest extends AbstractUnitTest {

    @BeforeClass
    public void initPrismContextIfNeeded() throws SchemaException, IOException, SAXException {
        // can be ready if other tests were run around, this fixes it for solo run
        if (PrismContext.get() == null) {
            PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        }
    }

    @Test
    public void nullUser() {
        SynchronizationSituation<?> situation =
                new SynchronizationSituation<>(null, null, SynchronizationSituationType.UNMATCHED);
        AssertJUnit.assertNotNull(situation);
        AssertJUnit.assertNull(situation.getCorrelatedOwner());
        AssertJUnit.assertEquals(SynchronizationSituationType.UNMATCHED, situation.getSituation());
    }

    @Test
    public void nullSituation() {
        assertThatThrownBy(() -> new SynchronizationSituation<>(new UserType(), null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Synchronization situation must not be null.");
    }

    @Test
    public void correct() {
        UserType user = new UserType();
        SynchronizationSituation<?> situation =
                new SynchronizationSituation<>(null, user, SynchronizationSituationType.UNMATCHED);
        AssertJUnit.assertNotNull(situation);
        AssertJUnit.assertEquals(user, situation.getCorrelatedOwner());
        AssertJUnit.assertEquals(SynchronizationSituationType.UNMATCHED, situation.getSituation());
    }
}
