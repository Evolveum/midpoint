/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.util.QNameUtil;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 *
 */
public class CaseFinder<RA> {

    private final SubcasesAsserter<RA> subcasesAsserter;
    private String targetOid;
    private QName targetType;

    public CaseFinder(SubcasesAsserter<RA> subcasesAsserter) {
        this.subcasesAsserter = subcasesAsserter;
    }

    public CaseFinder<RA> targetOid(String targetOid) {
        this.targetOid = targetOid;
        return this;
    }

    public CaseFinder<RA> targetType(QName targetType) {
        this.targetType = targetType;
        return this;
    }

    public CaseAsserter<SubcasesAsserter<RA>> find() {
        CaseType found = null;
        for (CaseType subcase: subcasesAsserter.getSubcases()) {
            if (matches(subcase)) {
                if (found == null) {
                    found = subcase;
                } else {
                    fail("Found more than one subcase that matches search criteria");
                }
            }
        }
        if (found == null) {
            throw new AssertionError("Found no subcase that matches search criteria");
        } else {
            return subcasesAsserter.forSubcase(found);
        }
    }

    public SubcasesAsserter<RA> assertNone() {
        for (CaseType subcase: subcasesAsserter.getSubcases()) {
            if (matches(subcase)) {
                fail("Found subcase while not expecting it: " + subcase);
            }
        }
        return subcasesAsserter;
    }

    public SubcasesAsserter<RA> assertAll() {
        for (CaseType subcase: subcasesAsserter.getSubcases()) {
            PrismObject<ShadowType> assignmentTarget = null;
            if (!matches(subcase)) {
                fail("Found subcase that does not match search criteria: "+subcase);
            }
        }
        return subcasesAsserter;
    }

    private boolean matches(CaseType subcase) {
        ObjectReferenceType targetRef = subcase.getTargetRef();
        if (targetOid != null) {
            if (targetRef == null || !targetOid.equals(targetRef.getOid())) {
                return false;
            }
        }
        if (targetType != null) {
            if (targetRef == null || !QNameUtil.match(targetType, targetRef.getType())) {
                return false;
            }
        }
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
