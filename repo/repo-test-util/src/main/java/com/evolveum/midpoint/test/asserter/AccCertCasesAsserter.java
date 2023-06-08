/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

/**
 * Asserts over a set of certification cases.
 */
public class AccCertCasesAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final List<AccessCertificationCaseType> cases;

    AccCertCasesAsserter(@NotNull RA parentCaseAsserter, @NotNull List<AccessCertificationCaseType> cases, String details) {
        super(parentCaseAsserter, details);
        this.cases = cases;
    }

    public @NotNull List<AccessCertificationCaseType> getCases() {
        return cases;
    }

    public AccCertCasesAsserter<RA> assertCases(int expected) {
        assertEquals("Wrong number of cases in " + desc(), expected, getCases().size());
        return this;
    }

    public AccCertCasesAsserter<RA> assertNone() {
        assertCases(0);
        return this;
    }

    AccCertCaseAsserter<AccCertCasesAsserter<RA>> forCase(AccessCertificationCaseType subcase) {
        AccCertCaseAsserter<AccCertCasesAsserter<RA>> asserter = new AccCertCaseAsserter<>(subcase, this, "subcase of "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public AccCertCaseAsserter<AccCertCasesAsserter<RA>> single() {
        assertCases(1);
        return forCase(getCases().get(0));
    }

    @Override
    protected String desc() {
        return descWithDetails("subcases"); // TODO
    }

    public AccCertCaseFinder<RA> by() {
        return new AccCertCaseFinder<>(this);
    }

    public AccCertCaseAsserter<AccCertCasesAsserter<RA>> forTarget(String targetOid) {
        return by()
                .targetOid(targetOid)
                .find();
    }

    public AccCertCaseAsserter<AccCertCasesAsserter<RA>> forTarget(String targetOid, QName targetType) {
        return by()
                .targetOid(targetOid)
                .targetType(targetType)
                .find();
    }

    public AccCertCaseAsserter<AccCertCasesAsserter<RA>> singleForTarget(String oid) {
        return by()
                .targetOid(oid)
                .find();
    }

    public AccCertCasesAsserter<RA> display() {
        display(desc());
        return this;
    }

    public AccCertCasesAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, cases);
        return this;
    }
}
