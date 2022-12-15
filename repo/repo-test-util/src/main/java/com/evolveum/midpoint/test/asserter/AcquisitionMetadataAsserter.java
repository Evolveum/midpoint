/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;

public class AcquisitionMetadataAsserter<RA extends AbstractAsserter<?>>
        extends PrismContainerValueAsserter<ProvenanceAcquisitionType, RA> {

    AcquisitionMetadataAsserter(
            ProvenanceAcquisitionType acquisition, RA returnAsserter, String detail) {
        //noinspection unchecked
        super(acquisition.asPrismContainerValue(), returnAsserter, detail);
    }

    public AcquisitionMetadataAsserter<RA> assertResourceRef(String oid) {
        assertRefEquals(ProvenanceAcquisitionType.F_RESOURCE_REF, oid);
        return this;
    }

    public AcquisitionMetadataAsserter<RA> assertOriginRef(String oid) {
        assertRefEquals(ProvenanceAcquisitionType.F_ORIGIN_REF, oid);
        return this;
    }

    public AcquisitionMetadataAsserter<RA> assertOriginRef(ObjectReferenceType ref) {
        assertRefEquals(ProvenanceAcquisitionType.F_ORIGIN_REF, ref);
        return this;
    }

    @SuppressWarnings("unused")
    public AcquisitionMetadataAsserter<RA> assertActorRef(String oid) {
        assertRefEquals(ProvenanceAcquisitionType.F_ACTOR_REF, oid);
        return this;
    }

    public AcquisitionMetadataAsserter<RA> assertChannel(String expected) {
        assertPropertyEquals(ProvenanceAcquisitionType.F_CHANNEL, expected);
        return this;
    }

    public AcquisitionMetadataAsserter<RA> assertTimestampBetween(XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        assertTimestampBetween(ProvenanceAcquisitionType.F_TIMESTAMP, startTs, endTs);
        return this;
    }

    public AcquisitionMetadataAsserter<RA> assertTimestampBefore(XMLGregorianCalendar timestamp) {
        assertTimestampBetween(ProvenanceAcquisitionType.F_TIMESTAMP, null, timestamp);
        return this;
    }

    public AcquisitionMetadataAsserter<RA> assertTimestamp(XMLGregorianCalendar timestamp) {
        assertTimestampBetween(ProvenanceAcquisitionType.F_TIMESTAMP, timestamp, timestamp);
        return this;
    }
}
