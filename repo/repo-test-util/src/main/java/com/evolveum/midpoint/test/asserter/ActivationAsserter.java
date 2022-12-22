/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

/**
 * @author semancik
 *
 */
public class ActivationAsserter<RA> extends AbstractAsserter<RA> {

    private ActivationType activationType;

    public ActivationAsserter(ActivationType activationType, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.activationType = activationType;
    }

    ActivationType getActivation() {
        return activationType;
    }

    public ActivationAsserter<RA> assertNone() {
        assertNull("Unexpected "+desc(), activationType);
        return this;
    }

    public ActivationAsserter<RA> assertAdministrativeStatus(ActivationStatusType expected) {
        assertEquals("Wrong administrative status in " + desc(), expected, getActivation().getAdministrativeStatus());
        return this;
    }

    public ActivationAsserter<RA> assertNoAdministrativeStatus() {
        assertNull("Unexpected administrative status in " + desc() + ": " + getActivation().getAdministrativeStatus(), getActivation().getAdministrativeStatus());
        return this;
    }

    public ActivationAsserter<RA> assertValidFrom(XMLGregorianCalendar expected) {
        assertEquals("Wrong validFrom in " + desc(), expected, getActivation().getValidFrom());
        return this;
    }

    public ActivationAsserter<RA> assertValidFrom(Date expected) {
        assertEquals("Wrong validFrom in " + desc(), XmlTypeConverter.createXMLGregorianCalendar(expected), getActivation().getValidFrom());
        return this;
    }

    public ActivationAsserter<RA> assertNoValidFrom() {
        assertNull("Unexpected validFrom in " + desc() + ": " +  getActivation().getValidFrom(), getActivation().getValidFrom());
        return this;
    }


    public ActivationAsserter<RA> assertValidTo(XMLGregorianCalendar expected) {
        assertEquals("Wrong validTo in " + desc(), expected, getActivation().getValidTo());
        return this;
    }

    public ActivationAsserter<RA> assertValidTo(Date expected) {
        assertEquals("Wrong validTo in " + desc(), XmlTypeConverter.createXMLGregorianCalendar(expected), getActivation().getValidTo());
        return this;
    }

    public ActivationAsserter<RA> assertNoValidTo() {
        assertNull("Unexpected validTo in " + desc() + ": " +  getActivation().getValidTo(), getActivation().getValidTo());
        return this;
    }

    public ActivationAsserter<RA> assertEffectiveStatus(ActivationStatusType expected) {
        assertEquals("Wrong effective status in " + desc(), expected, getActivation().getEffectiveStatus());
        return this;
    }

    public ActivationAsserter<RA> assertNoEffectiveStatus() {
        assertNull("Unexpected effective status in " + desc() + ": " + getActivation().getEffectiveStatus(), getActivation().getEffectiveStatus());
        return this;
    }

    public ActivationAsserter<RA> assertEnableTimestampPresent() {
        assertThat(getActivation().getEnableTimestamp()).as("enable timestamp in " + desc()).isNotNull();
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("activation of "+getDetails());
    }

}
