/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

public abstract class AbstractActivationComputerTest extends AbstractUnitTest {

    protected static final XMLGregorianCalendar YEAR_START = XmlTypeConverter.createXMLGregorianCalendar(2013, 1, 1, 0, 0, 0);
    protected static final XMLGregorianCalendar SPRING_EQUINOX = XmlTypeConverter.createXMLGregorianCalendar(2013, 3, 20, 11, 2, 0);
    protected static final XMLGregorianCalendar SUMMER_SOLSTICE = XmlTypeConverter.createXMLGregorianCalendar(2013, 6, 21, 5, 4, 0);
    protected static final XMLGregorianCalendar AUTUMN_EQUINOX = XmlTypeConverter.createXMLGregorianCalendar(2013, 9, 22, 20, 4, 0);
    protected static final XMLGregorianCalendar WINTER_SOLSTICE = XmlTypeConverter.createXMLGregorianCalendar(2013, 12, 21, 17, 11, 0);

    // Undefined state
    protected static final String LIFECYCLE_STATE_LIMBO = "limbo";

    protected static final String LIFECYCLE_STATE_CHARMED = "charmed";
    protected static final String LIFECYCLE_STATE_INHUMED = "inhumed";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    protected abstract LifecycleStateModelType createLifecycleModel()
            throws SchemaException, IOException;

    @Test
    public void testGetAdministrativeEnabled() throws Exception {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus =
                activationComputer.getEffectiveStatus(null, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ENABLED, effectiveStatus);
    }

    @Test
    public void testGetAdministrativeDisabled() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus =
                activationComputer.getEffectiveStatus(null, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetAdministrativeArchived() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ARCHIVED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus =
                activationComputer.getEffectiveStatus(null, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ARCHIVED, effectiveStatus);
    }

    @Test
    public void testGetLimboAdministrativeEnabled() {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus =
                activationComputer.getEffectiveStatus(LIFECYCLE_STATE_LIMBO, activationType, null);

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetActiveAdministrativeEnabled() throws Exception {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_ACTIVE, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ENABLED, effectiveStatus);
    }

    @Test
    public void testGetActiveAdministrativeDisabled() throws Exception {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_ACTIVE, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetDeprecatedAdministrativeDisabled() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_DEPRECATED, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetDeprecatedAdministrativeEnabled() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_DEPRECATED, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ENABLED, effectiveStatus);
    }

    @Test
    public void testGetActiveAdministrativeArchived() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ARCHIVED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_ACTIVE, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ARCHIVED, effectiveStatus);
    }

    @Test
    public void testGetArchivedAdministrativeEnabled() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                SchemaConstants.LIFECYCLE_ARCHIVED, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ARCHIVED, effectiveStatus);
    }

    @Test
    public void testGetBeforeValidity() throws Exception {
        // GIVEN
        Clock clock = createClock(YEAR_START);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(null, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                null, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testGetInValidity() throws Exception {
        // GIVEN
        Clock clock = createClock(SUMMER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(null, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                null, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.ENABLED, effectiveStatus);
    }

    @Test
    public void testGetAfterValidity() throws Exception {
        // GIVEN
        Clock clock = createClock(WINTER_SOLSTICE);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(null, SPRING_EQUINOX, AUTUMN_EQUINOX);

        // WHEN
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(
                null, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", ActivationStatusType.DISABLED, effectiveStatus);
    }

    @Test
    public void testComputeAdministrativeEnabledBefore() throws Exception {
        testCompute(YEAR_START,
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.ENABLED, TimeIntervalStatusType.BEFORE);
    }

    @Test
    public void testComputeAdministrativeEnabledIn() throws Exception {
        testCompute(SUMMER_SOLSTICE,
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.ENABLED, TimeIntervalStatusType.IN);
    }

    @Test
    public void testComputeAdministrativeEnabledAfter() throws Exception {
        testCompute(WINTER_SOLSTICE,
                ActivationStatusType.ENABLED, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.ENABLED, TimeIntervalStatusType.AFTER);
    }

    @Test
    public void testComputeAdministrativeArchivedBefore() throws Exception {
        testCompute(YEAR_START,
                ActivationStatusType.ARCHIVED, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.ARCHIVED, TimeIntervalStatusType.BEFORE);
    }

    @Test
    public void testComputeAdministrativeDisabledIn() throws Exception {
        testCompute(SUMMER_SOLSTICE,
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.DISABLED, TimeIntervalStatusType.IN);
    }

    @Test
    public void testComputeAdministrativeDisabledAfter() throws Exception {
        testCompute(WINTER_SOLSTICE,
                ActivationStatusType.DISABLED, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.DISABLED, TimeIntervalStatusType.AFTER);
    }

    @Test
    public void testComputeBefore() throws Exception {
        testCompute(YEAR_START,
                null, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.DISABLED, TimeIntervalStatusType.BEFORE);
    }

    @Test
    public void testComputeIn() throws Exception {
        testCompute(SUMMER_SOLSTICE,
                null, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.ENABLED, TimeIntervalStatusType.IN);
    }

    @Test
    public void testComputeAfter() throws Exception {
        testCompute(WINTER_SOLSTICE,
                null, SPRING_EQUINOX, AUTUMN_EQUINOX,
                ActivationStatusType.DISABLED, TimeIntervalStatusType.AFTER);
    }

    protected void testCompute(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(null, now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
        testComputeDraft(now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
        testComputeProposed(now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
        testComputeSuspended(now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
        testCompute(SchemaConstants.LIFECYCLE_ACTIVE, now, administrativeStatus,
                validFrom, validTo, expectedEffective, expectedValidity);
        testCompute(SchemaConstants.LIFECYCLE_DEPRECATED, now, administrativeStatus,
                validFrom, validTo, expectedEffective, expectedValidity);
        testCompute(SchemaConstants.LIFECYCLE_FAILED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
        testCompute(SchemaConstants.LIFECYCLE_SUSPENDED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
        testCompute(SchemaConstants.LIFECYCLE_ARCHIVED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.ARCHIVED, expectedValidity);
        testComputeLimbo(now, administrativeStatus, validFrom, validTo, expectedValidity);
        testComputeCharmed(now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
        testComputeInhumed(now, administrativeStatus, validFrom, validTo, expectedEffective, expectedValidity);
    }

    protected void testComputeDraft(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(SchemaConstants.LIFECYCLE_DRAFT, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    protected void testComputeProposed(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(SchemaConstants.LIFECYCLE_PROPOSED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    protected void testComputeSuspended(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(SchemaConstants.LIFECYCLE_SUSPENDED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    protected void testComputeLimbo(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            TimeIntervalStatusType expectedValidity) throws SchemaException, IOException {
        testCompute(LIFECYCLE_STATE_LIMBO, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    protected void testComputeCharmed(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(LIFECYCLE_STATE_CHARMED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    protected void testComputeInhumed(
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        testCompute(LIFECYCLE_STATE_INHUMED, now, administrativeStatus,
                validFrom, validTo, ActivationStatusType.DISABLED, expectedValidity);
    }

    protected void testCompute(String lifecycleState,
            XMLGregorianCalendar now, ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo,
            ActivationStatusType expectedEffective, TimeIntervalStatusType expectedValidity)
            throws SchemaException, IOException {
        // GIVEN
        Clock clock = createClock(now);
        ActivationComputer activationComputer = createActivationComputer(clock);
        ActivationType activationType = createActivationType(administrativeStatus, validFrom, validTo);

        // WHEN
        activationComputer.computeEffective(lifecycleState, activationType, createLifecycleModel());

        // THEN
        assertEquals("Unexpected effective status", expectedEffective, activationType.getEffectiveStatus());
        assertEquals("Unexpected validity status", expectedValidity, activationType.getValidityStatus());
    }

    protected ActivationComputer createActivationComputer(Clock clock) {
        return new ActivationComputer(clock);
    }

    protected Clock createClock(XMLGregorianCalendar date) {
        Clock clock = new Clock();
        clock.override(date);
        return clock;
    }

    protected ActivationType createActivationType(ActivationStatusType administrativeStatus,
            XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo) {
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(administrativeStatus);
        activationType.setValidFrom(validFrom);
        activationType.setValidTo(validTo);
        return activationType;
    }
}
