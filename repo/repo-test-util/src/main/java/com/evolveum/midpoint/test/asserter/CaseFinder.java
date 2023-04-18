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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 *
 */
public class CaseFinder<RA> {

    private final SubcasesAsserter<RA> subcasesAsserter;
    private String targetOid;
    private QName targetType;
    private Boolean approvalSchemaPresent;

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

    public CaseFinder<RA> approvalSchemaPresent(Boolean approvalSchemaPresent) {
        this.approvalSchemaPresent = approvalSchemaPresent;
        return this;
    }

    public CaseAsserter<SubcasesAsserter<RA>> find() {
        List<CaseType> matching = subcasesAsserter.getSubcases().stream()
                .filter(this::matches)
                .collect(Collectors.toList());
        if (matching.size() > 1) {
            throw new AssertionError("Found more than one subcase that matches search criteria: " + matching);
        } else if (matching.isEmpty()) {
            throw new AssertionError("Found no subcase that matches search criteria");
        } else {
            return subcasesAsserter.forSubcase(matching.get(0));
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
        if (approvalSchemaPresent != null
                && !approvalSchemaPresent.equals(hasApprovalSchema(subcase))) {
            return false;
        }
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

    private boolean hasApprovalSchema(CaseType aCase) {
        ApprovalContextType ctx = aCase.getApprovalContext();
        return ctx != null && ctx.getApprovalSchema() != null;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }
}
