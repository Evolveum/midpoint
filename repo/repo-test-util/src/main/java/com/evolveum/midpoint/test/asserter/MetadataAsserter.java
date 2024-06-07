/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CHANNEL_USER_URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author semancik
 *
 */
public class MetadataAsserter<RA> extends AbstractAsserter<RA> {

    private final MetadataType metadata;

    public MetadataAsserter(MetadataType metadata, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.metadata = metadata;
    }

    MetadataType getMetadata() {
        return metadata;
    }

    public MetadataAsserter<RA> assertNone() {
        assertNull("Unexpected "+desc(), metadata);
        return this;
    }

    public MetadataAsserter<RA> assertPresent() {
        assertNotNull("Missing "+desc(), metadata);
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("metadata of "+getDetails());
    }

    public MetadataAsserter<RA> assertModifyTaskOid(String expectedOid) {
        String realOid = getOid(metadata.getModifyTaskRef());
        assertThat(realOid).as("modify task ref OID").isEqualTo(expectedOid);
        return this;
    }

    public MetadataAsserter<RA> assertRequestTimestampPresent() {
        assertThat(metadata.getRequestTimestamp()).as("request timestamp").isNotNull();
        return this;
    }

    public MetadataAsserter<RA> assertCreateTimestampPresent() {
        assertThat(metadata.getCreateTimestamp()).as("create timestamp").isNotNull();
        return this;
    }

    public MetadataAsserter<RA> assertCreateTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("create timestamp", start, end, metadata.getCreateTimestamp());
        return this;
    }

    public MetadataAsserter<RA> assertModifyTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("modify timestamp", start, end, metadata.getModifyTimestamp());
        return this;
    }

    public MetadataAsserter<RA> assertLastProvisioningTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("last provisioning timestamp", start, end, metadata.getLastProvisioningTimestamp());
        return this;
    }

    public MetadataAsserter<RA> assertCreator() {
        if (expectedActor != null) {
            assertThat(getOid(metadata.getCreatorRef())).as("creator OID").isEqualTo(expectedActor.getOid());
        }
        return this;
    }

    public MetadataAsserter<RA> assertModifier() {
        if (expectedActor != null) {
            assertThat(getOid(metadata.getModifierRef())).as("modifier OID").isEqualTo(expectedActor.getOid());
        }
        return this;
    }

    public MetadataAsserter<RA> assertRequestor() {
        if (expectedActor != null) {
            assertThat(getOid(metadata.getRequestorRef())).as("requestor OID").isEqualTo(expectedActor.getOid());
        }
        return this;
    }

    public MetadataAsserter<RA> assertLastProvisioningTimestampPresent(boolean expected) {
        return expected ? assertLastProvisioningTimestampPresent() : assertLastProvisioningTimestampNotPresent();
    }

    public MetadataAsserter<RA> assertLastProvisioningTimestampPresent() {
        assertThat(metadata.getLastProvisioningTimestamp()).as("last provisioning timestamp").isNotNull();
        return this;
    }

    public MetadataAsserter<RA> assertLastProvisioningTimestampNotPresent() {
        assertThat(metadata.getLastProvisioningTimestamp()).as("last provisioning timestamp").isNull();
        return this;
    }

    public MetadataAsserter<RA> assertCreateChannel(String expected) {
        assertThat(metadata.getCreateChannel()).as("create channel").isEqualTo(expected);
        return this;
    }

    public MetadataAsserter<RA> assertModifyChannel(String expected) {
        assertThat(metadata.getModifyChannel()).as("modify channel").isEqualTo(expected);
        return this;
    }

    public MetadataAsserter<RA> assertCreateMetadataComplex(XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        return assertCreateTimestamp(startTime, endTime)
                .assertCreator()
                .assertRequestor()
                .assertCreateChannel(CHANNEL_USER_URI);
    }

    public MetadataAsserter<RA> assertModifyMetadataComplex(XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        return assertModifyTimestamp(startTime, endTime)
                .assertModifier()
                .assertModifyChannel(CHANNEL_USER_URI);
    }
}
