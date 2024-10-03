/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ProvenanceMetadataUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.prism.Referencable.getOid;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CHANNEL_USER_URI;

import static org.assertj.core.api.Assertions.assertThat;

public class ValueMetadataValueAsserter<RA extends AbstractAsserter<?>>
        extends PrismContainerValueAsserter<ValueMetadataType, RA> {

    public ValueMetadataValueAsserter(PrismContainerValue<ValueMetadataType> valueMetadataValue, RA parentAsserter, String details) {
        super(valueMetadataValue, parentAsserter, details);
    }

    public ValueMetadataValueAsserter(ValueMetadataType valueMetadataValue, RA parentAsserter, String details) {
        //noinspection unchecked
        super(valueMetadataValue.asPrismContainerValue(), parentAsserter, details);
    }

    @Override
    public ValueMetadataValueAsserter<RA> assertSize(int expected) {
        return (ValueMetadataValueAsserter<RA>) super.assertSize(expected);
    }

    @Override
    public ValueMetadataValueAsserter<RA> assertItemsExactly(QName... expectedItems) {
        return (ValueMetadataValueAsserter<RA>) super.assertItemsExactly(expectedItems);
    }

    @Override
    public ValueMetadataValueAsserter<RA> assertAny() {
        return (ValueMetadataValueAsserter<RA>) super.assertAny();
    }

    @Override
    public <T> ValueMetadataValueAsserter<RA> assertPropertyValuesEqual(ItemPath path, T... expectedValues) {
        return (ValueMetadataValueAsserter<RA>) super.assertPropertyValuesEqual(path, expectedValues);
    }

    @Override
    public <T> ValueMetadataValueAsserter<RA> assertPropertyValuesEqualRaw(ItemPath path, T... expectedValues) {
        return (ValueMetadataValueAsserter<RA>) super.assertPropertyValuesEqualRaw(path, expectedValues);
    }

    @Override
    public ValueMetadataValueAsserter<RA> assertNoItem(ItemPath itemName) {
        return (ValueMetadataValueAsserter<RA>) super.assertNoItem(itemName);
    }

    public ProvenanceMetadataAsserter<ValueMetadataValueAsserter<RA>> provenance() {
        String details = "provenance in " + getDetails();
        return new ProvenanceMetadataAsserter<>(
                MiscUtil.assertNonNull(getBean().getProvenance(), "no %s", details),
                this, details);
    }

    private @NotNull ValueMetadataType getBean() {
        return getPrismValue().asContainerable();
    }

    @Override
    public <CC extends Containerable> PrismContainerValueAsserter<CC, ValueMetadataValueAsserter<RA>> containerSingle(QName subcontainerQName) {
        return (PrismContainerValueAsserter<CC, ValueMetadataValueAsserter<RA>>) super.containerSingle(subcontainerQName);
    }

    public ValueMetadataValueAsserter<RA> assertInternalOrigin() {
        if (!ProvenanceMetadataUtil.hasOrigin(getBean(), SystemObjectsType.ORIGIN_INTERNAL.value())) {
            fail("Internal origin is not present in the metadata: " + getPrismValue().debugDump());
        }
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertNoMappingSpec() {
        return provenance().assertNoMappingSpec().end();
    }

    public ValueMetadataValueAsserter<RA> assertMappingName(String expected) {
        return provenance().assertMappingName(expected).end();
    }

    public ValueMetadataValueAsserter<RA> assertMappingObjectType(@NotNull ResourceObjectTypeIdentification expected) {
        return provenance().assertMappingObjectType(expected).end();
    }

    public ValueMetadataValueAsserter<RA> assertMappingAssociationType(@Nullable QName expected) {
        return provenance().assertMappingAssociationType(expected).end();
    }

    public ValueMetadataValueAsserter<RA> assertMappingTag(@Nullable String expected) {
        return provenance().assertMappingTag(expected).end();
    }

    public ValueMetadataValueAsserter<RA> assertMappingObjectOid(@NotNull String expected) {
        return provenance().assertMappingObjectOid(expected).end();
    }

    public ValueMetadataValueAsserter<RA> assertModifyTaskOid(String expectedOid) {
        String realOid = getOid(getStorageRequired().getModifyTaskRef());
        assertThat(realOid).as("modify task ref OID").isEqualTo(expectedOid);
        return this;
    }

    private @NotNull StorageMetadataType getStorageRequired() {
        var storage = getBean().getStorage();
        assertThat(storage).as("storage metadata").isNotNull();
        return storage;
    }

    public ValueMetadataValueAsserter<RA> assertRequestTimestampPresent() {
        assertThat(getProcessRequired().getRequestTimestamp()).as("request timestamp").isNotNull();
        return this;
    }

    private ProvenanceMetadataType getProvenanceRequired() {
        var provenance = getBean().getProvenance();
        assertThat(provenance).as("provenance metadata").isNotNull();
        return provenance;
    }

    private ProcessMetadataType getProcessRequired() {
        var process = getBean().getProcess();
        assertThat(process).as("process metadata").isNotNull();
        return process;
    }

    public ValueMetadataValueAsserter<RA> assertCreateTimestampPresent() {
        assertThat(getStorageRequired().getCreateTimestamp()).as("create timestamp").isNotNull();
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertCreateTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("create timestamp in " + desc(), start, end, getStorageRequired().getCreateTimestamp());
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertModifyTimestampPresent() {
        assertThat(getStorageRequired().getModifyTimestamp()).as("modify timestamp").isNotNull();
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertModifyTimestampNotPresent() {
        var storage = getBean().getStorage();
        if (storage != null) {
            assertThat(storage.getModifyTimestamp()).as("modify timestamp").isNull();
        }
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertModifyTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("modify timestamp", start, end, getStorageRequired().getModifyTimestamp());
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertLastProvisioningTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("last provisioning timestamp", start, end, getProvisioningRequired().getLastProvisioningTimestamp());
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertCreator() {
        if (expectedActor != null) {
            assertThat(getOid(getStorageRequired().getCreatorRef()))
                    .as("creator OID")
                    .isEqualTo(expectedActor.getOid());
        }
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertModifier() {
        if (expectedActor != null) {
            assertThat(getOid(getStorageRequired().getModifierRef()))
                    .as("modifier OID")
                    .isEqualTo(expectedActor.getOid());
        }
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertRequestor() {
        if (expectedActor != null) {
            assertThat(getOid(getProcessRequired().getRequestorRef()))
                    .as("requestor OID")
                    .isEqualTo(expectedActor.getOid());
        }
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertLastProvisioningTimestampPresent(boolean expected) {
        return expected ? assertLastProvisioningTimestampPresent() : assertLastProvisioningTimestampNotPresent();
    }

    public ValueMetadataValueAsserter<RA> assertLastProvisioningTimestampPresent() {
        assertThat(getProvisioningRequired().getLastProvisioningTimestamp()).as("last provisioning timestamp").isNotNull();
        return this;
    }

    private ProvisioningMetadataType getProvisioningRequired() {
        var provisioning = getBean().getProvisioning();
        assertThat(provisioning).as("provisioning metadata").isNotNull();
        return provisioning;
    }

    public ValueMetadataValueAsserter<RA> assertLastProvisioningTimestampNotPresent() {
        var provisioning = getBean().getProvisioning();
        if (provisioning != null) {
            assertThat(provisioning.getLastProvisioningTimestamp()).as("last provisioning timestamp").isNull();
        }
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertAcquisitionChannel(String expected) {
        var channels = getProvenanceRequired().getAcquisition().stream()
                .map(a -> a.getChannel())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        assertThat(channels).as("channels in acquisition").containsExactlyInAnyOrder(expected);
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertCreateChannel(String expected) {
        assertThat(getStorageRequired().getCreateChannel()).as("create channel").isEqualTo(expected);
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertModifyChannel(String expected) {
        assertThat(getStorageRequired().getModifyChannel()).as("modify channel").isEqualTo(expected);
        return this;
    }

    public ValueMetadataValueAsserter<RA> assertCreateMetadataComplex(XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        return assertCreateTimestamp(startTime, endTime)
                .assertCreator()
                .assertRequestor()
                .assertCreateChannel(CHANNEL_USER_URI);
    }

    public ValueMetadataValueAsserter<RA> assertModifyMetadataComplex(XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        return assertModifyTimestamp(startTime, endTime)
                .assertModifier()
                .assertModifyChannel(CHANNEL_USER_URI);
    }

    public ValueMetadataValueAsserter<RA> assertModifyApprovers(String... oids) {
        assertThat(getProcessRequired().getModifyApproverRef())
                .as("modify approver refs")
                .extracting(ref -> ref.getOid())
                .containsExactlyInAnyOrder(oids);
        return this;
    }

    protected String desc() {
        // TODO handling of details
        return "metadata of " + getDetails();
    }

    @Override
    public ValueMetadataValueAsserter<RA> display() {
        return (ValueMetadataValueAsserter<RA>) super.display();
    }
}
