/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;

public class TestActivationComputerDefault extends AbstractActivationComputerTest {

    @Override
    protected LifecycleStateModelType createLifecycleModel() {
        return null;
    }

    @Test
    public void testGetDraftAdministrativeEnabled() {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_DRAFT, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetProposedAdministrativeEnabled() {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_PROPOSED, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetSuspendedAdministrativeEnabled() {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_SUSPENDED, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }
}
