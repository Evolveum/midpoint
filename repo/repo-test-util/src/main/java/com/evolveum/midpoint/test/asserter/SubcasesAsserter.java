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

import com.evolveum.midpoint.test.IntegrationTestTools;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Asserts over a set of subcases.
 *
 */
public class SubcasesAsserter<RA> extends AbstractAsserter<CaseAsserter<RA>> {

    @NotNull private final CaseAsserter<RA> parentCaseAsserter;
    @NotNull private final List<CaseType> subcases;

    SubcasesAsserter(@NotNull CaseAsserter<RA> parentCaseAsserter, @NotNull List<CaseType> subcases, String details) {
        super(parentCaseAsserter, details);
        this.parentCaseAsserter = parentCaseAsserter;
        this.subcases = subcases;
    }

    PrismObject<CaseType> getParentCase() {
        return parentCaseAsserter.getObject();
    }

    public @NotNull List<CaseType> getSubcases() {
        return subcases;
    }

    public SubcasesAsserter<RA> assertSubcases(int expected) {
        assertEquals("Wrong number of subcases in " + desc(), expected, getSubcases().size());
        return this;
    }

    public SubcasesAsserter<RA> assertNone() {
        assertSubcases(0);
        return this;
    }

    CaseAsserter<SubcasesAsserter<RA>> forSubcase(CaseType subcase) {
        CaseAsserter<SubcasesAsserter<RA>> asserter = new CaseAsserter<>(subcase.asPrismObject(), this, "subcase of "+getParentCase());
        copySetupTo(asserter);
        return asserter;
    }

    public CaseAsserter<SubcasesAsserter<RA>> single() {
        assertSubcases(1);
        return forSubcase(getSubcases().get(0));
    }

    @Override
    protected String desc() {
        return descWithDetails("subcases of "+ getParentCase());
    }

    public CaseFinder<RA> by() {
        return new CaseFinder<>(this);
    }

    public CaseAsserter<SubcasesAsserter<RA>> forTarget(String targetOid) {
        return by()
                .targetOid(targetOid)
                .find();
    }

    public CaseAsserter<SubcasesAsserter<RA>> forTarget(String targetOid, QName targetType) {
        return by()
                .targetOid(targetOid)
                .targetType(targetType)
                .find();
    }

    public CaseAsserter<SubcasesAsserter<RA>> singleWithoutApprovalSchema() {
        return by()
                .approvalSchemaPresent(false)
                .find();
    }

    public CaseAsserter<SubcasesAsserter<RA>> singleWithApprovalSchema() {
        return by()
                .approvalSchemaPresent(true)
                .find();
    }

    public CaseAsserter<SubcasesAsserter<RA>> singleForTarget(String oid) {
        return by()
                .targetOid(oid)
                .find();
    }

    public SubcasesAsserter<RA> display() {
        display(desc());
        return this;
    }

    public SubcasesAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, subcases);
        return this;
    }
}
