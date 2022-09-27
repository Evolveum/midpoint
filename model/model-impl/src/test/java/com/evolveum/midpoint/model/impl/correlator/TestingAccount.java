/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

/**
 * Account used for both correlation and matching tests.
 * (Some of the attributes may not be relevant or even present for both cases.)
 */
public class TestingAccount implements DebugDumpable {

    static final String ATTR_CORRELATOR = "correlator";
    private static final ItemName ATTR_CORRELATOR_QNAME = new ItemName(NS_RI, ATTR_CORRELATOR);

    static final String ATTR_EMPLOYEE_NUMBER = "employeeNumber";
    static final String ATTR_COST_CENTER = "costCenter";

    static final String ATTR_GIVEN_NAME = "givenName";
    static final String ATTR_FAMILY_NAME = "familyName";
    static final String ATTR_DATE_OF_BIRTH = "dateOfBirth";
    static final String ATTR_NATIONAL_ID = "nationalId";
    static final String ATTR_HONORIFIC_PREFIX = "honorificPrefix";

    static final String ATTR_TEST = "test"; // used for the test itself
    private static final ItemName ATTR_TEST_QNAME = new ItemName(NS_RI, ATTR_TEST);

    @NotNull protected final ShadowType shadow;

    public TestingAccount(@NotNull PrismObject<ShadowType> shadow) {
        this.shadow = shadow.asObjectable();
    }

    protected String getTestString() {
        try {
            return ShadowUtil.getAttributeValue(shadow, ATTR_TEST_QNAME);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    public String getCorrelator() {
        try {
            return ShadowUtil.getAttributeValue(shadow, TestingAccount.ATTR_CORRELATOR_QNAME);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
    }

    public int getNumber() {
        return Integer.parseInt(shadow.getName().getOrig());
    }

    public @NotNull ShadowAttributesType getAttributes() {
        return shadow.getAttributes();
    }

    public @NotNull ShadowType getShadow() {
        return shadow;
    }

    @Override
    public String toString() {
        return "TestingAccount{" +
                "shadow=" + shadow +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "shadow", shadow, indent + 1);
        return sb.toString();
    }
}
