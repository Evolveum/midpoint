/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author semancik
 *
 */
public class MetadataAsserter<RA> extends AbstractAsserter<RA> {

    private MetadataType metadataType;

    public MetadataAsserter(MetadataType metadataType, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.metadataType = metadataType;
    }

    MetadataType getMetadata() {
        return metadataType;
    }

    public MetadataAsserter<RA> assertNone() {
        assertNull("Unexpected "+desc(), metadataType);
        return this;
    }

    public MetadataAsserter<RA> assertOriginMappingName(String expected) {
        assertEquals("Wrong origin mapping name in " + desc(), expected, getMetadata().getOriginMappingName());
        return this;
    }

    @Override
    protected String desc() {
        return descWithDetails("metadata of "+getDetails());
    }

}
