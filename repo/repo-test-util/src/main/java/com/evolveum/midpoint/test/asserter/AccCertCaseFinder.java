/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class AccCertCaseFinder<RA> {

    private final AccCertCasesAsserter<RA> casesAsserter;
    private String targetOid;
    private QName targetType;

    public AccCertCaseFinder(AccCertCasesAsserter<RA> casesAsserter) {
        this.casesAsserter = casesAsserter;
    }

    public AccCertCaseFinder<RA> targetOid(String targetOid) {
        this.targetOid = targetOid;
        return this;
    }

    public AccCertCaseFinder<RA> targetType(QName targetType) {
        this.targetType = targetType;
        return this;
    }

    public AccCertCaseAsserter<AccCertCasesAsserter<RA>> find() {
        List<AccessCertificationCaseType> matching = casesAsserter.getCases().stream()
                .filter(this::matches)
                .collect(Collectors.toList());
        if (matching.size() > 1) {
            throw new AssertionError("Found more than one case that matches search criteria: " + matching);
        } else if (matching.isEmpty()) {
            throw new AssertionError("Found no case that matches search criteria");
        } else {
            return casesAsserter.forCase(matching.get(0));
        }
    }

    public AccCertCasesAsserter<RA> assertNone() {
        for (AccessCertificationCaseType subcase: casesAsserter.getCases()) {
            if (matches(subcase)) {
                fail("Found subcase while not expecting it: " + subcase);
            }
        }
        return casesAsserter;
    }

    public AccCertCasesAsserter<RA> assertAll() {
        for (AccessCertificationCaseType subcase: casesAsserter.getCases()) {
            if (!matches(subcase)) {
                fail("Found subcase that does not match search criteria: "+subcase);
            }
        }
        return casesAsserter;
    }

    private boolean matches(AccessCertificationCaseType aCase) {
        ObjectReferenceType targetRef = aCase.getTargetRef();
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
