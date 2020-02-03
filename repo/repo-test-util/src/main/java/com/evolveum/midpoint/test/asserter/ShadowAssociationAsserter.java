/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class ShadowAssociationAsserter<R> extends AbstractAsserter<R> {

    private List<ShadowAssociationType> associations;

    public ShadowAssociationAsserter(List<ShadowAssociationType> associations, R returnAsserter, String details) {
        super(returnAsserter, details);
        this.associations = associations;
    }

    public ShadowAssociationAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of values in "+desc(), expected, associations.size());
        return this;
    }

    public ShadowAssociationAsserter<R> assertShadowOids(String... expectedShadowOids) {
        for (String expectedShadowOid: expectedShadowOids) {
            ShadowAssociationType association = findByShadowOid(expectedShadowOid);
            if (association == null) {
                fail("Expected association shadow OID "+expectedShadowOid+" in "+desc()+" but there was none. Association present: "+prettyPrintShadowOids());
            }
        }
        for (ShadowAssociationType existingAssociation : associations) {
            if (!ArrayUtils.contains(expectedShadowOids, existingAssociation.getShadowRef().getOid())) {
                fail("Unexpected association shadow OID "+existingAssociation.getShadowRef().getOid()+" in "+desc()+". Expected attributes: "+ArrayUtils.toString(expectedShadowOids));
            }
        }
        return this;
    }

    public ShadowReferenceAsserter<ShadowAssociationAsserter<R>> singleShadowRef() {
        assertSize(1);
        PrismReferenceValue refVal = associations.get(0).getShadowRef().asReferenceValue();
        ShadowReferenceAsserter<ShadowAssociationAsserter<R>> asserter = new ShadowReferenceAsserter<>(refVal, null, this, "shadowRef in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    private @Nullable ShadowAssociationType findByShadowOid(String shadowOid) {
        for (ShadowAssociationType association : associations) {
            if (shadowOid.equals(association.getShadowRef().getOid())) {
                return association;
            }
        }
        return null;
    }

    private String prettyPrintShadowOids() {
        StringBuilder sb = new StringBuilder();
        Iterator<ShadowAssociationType> iterator = associations.iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next().getShadowRef().getOid()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }


    public ShadowAssociationAsserter<R> assertAny() {
        assertNotNull("No associations in "+desc(), associations);
        assertFalse("No associations in "+desc(), associations.isEmpty());
        return this;
    }

    public <T> ShadowAssociationAsserter<R> assertNone() {
        assertTrue("Unexpected association values in "+desc()+": "+associations, associations.isEmpty());
        return this;
    }

    protected String desc() {
        return getDetails();
    }

}
