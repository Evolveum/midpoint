/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.test.asserter.prism.PolyStringAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * @author semancik
 *
 */
public class CaseAsserter<RA> extends PrismObjectAsserter<CaseType,RA> {

    public CaseAsserter(PrismObject<CaseType> focus) {
        super(focus);
    }

    public CaseAsserter(PrismObject<CaseType> focus, String details) {
        super(focus, details);
    }

    public CaseAsserter(PrismObject<CaseType> focus, RA returnAsserter, String details) {
        super(focus, returnAsserter, details);
    }

    public static CaseAsserter<Void> forCase(PrismObject<CaseType> object) {
        return new CaseAsserter<>(object);
    }

    public static CaseAsserter<Void> forCase(PrismObject<CaseType> object, String details) {
        return new CaseAsserter<>(object, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public CaseAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public CaseAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public CaseAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public CaseAsserter<RA> display() {
        super.display();
        return this;
    }

    public CaseAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }


    @Override
    public PolyStringAsserter<CaseAsserter<RA>> name() {
        return (PolyStringAsserter<CaseAsserter<RA>>)super.name();
    }

    @Override
    public CaseAsserter<RA> assertNoTrigger() {
        super.assertNoTrigger();
        return this;
    }
}
