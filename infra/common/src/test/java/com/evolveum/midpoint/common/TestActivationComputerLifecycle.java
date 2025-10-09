/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TestActivationComputerLifecycle extends AbstractActivationComputerTest {

    private static final File SYSTEM_CONFIGURATION_LIFECYCLE_FILE =
            new File("src/test/resources/system-configuration-lifecycle.xml");

    @Override
    protected LifecycleStateModelType createLifecycleModel() throws SchemaException, IOException {
        PrismObject<SystemConfigurationType> systemConfig =
                PrismTestUtil.parseObject(SYSTEM_CONFIGURATION_LIFECYCLE_FILE);
        List<ObjectPolicyConfigurationType> objectPolicyConfigurations =
                systemConfig.asObjectable().getDefaultObjectPolicyConfiguration();
        return objectPolicyConfigurations.get(0).getLifecycleStateModel();
    }

    @Test
    public void testGetProposedAdministrativeEnabled() throws Exception {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType =
                createActivationType(ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_PROPOSED, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ENABLED, effectiveStatus);
    }

    @Test
    public void testGetDraftAdministrativeEnabled() throws Exception {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType =
                createActivationType(ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_DRAFT, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Override
    protected void testComputeDraft(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(SchemaConstants.LIFECYCLE_DRAFT, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    @Override
    protected void testComputeProposed(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(SchemaConstants.LIFECYCLE_PROPOSED, now, administrativeStatus,
                validFrom, validTo, expectedEffective, expectedValidity);
    }

    @Override
    protected void testComputeSuspended(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(SchemaConstants.LIFECYCLE_SUSPENDED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    @Override
    protected void testComputeCharmed(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(LIFECYCLE_STATE_CHARMED, now, administrativeStatus,
                validFrom, validTo, expectedEffective, expectedValidity);
    }

    @Override
    protected void testComputeInhumed(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(LIFECYCLE_STATE_INHUMED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }
}
