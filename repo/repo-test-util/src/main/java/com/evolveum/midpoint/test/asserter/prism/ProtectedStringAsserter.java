/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.testng.AssertJUnit;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class ProtectedStringAsserter<RA> extends AbstractAsserter<RA> {

    private ProtectedStringType protectedString;

    public ProtectedStringAsserter(ProtectedStringType protectedString) {
        super();
        this.protectedString = protectedString;
    }

    public ProtectedStringAsserter(ProtectedStringType protectedString, String detail) {
        super(detail);
        this.protectedString = protectedString;
    }

    public ProtectedStringAsserter(ProtectedStringType protectedString, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.protectedString = protectedString;
    }

    public ProtectedStringType getProtectedString() {
        return protectedString;
    }

    public ProtectedStringAsserter<RA> assertIsEncrypted() {
        assertTrue("Non-encrypted procted string in "+desc(), protectedString.isEncrypted());
        return this;
    }

    public ProtectedStringAsserter<RA> assertIsHashed() {
        assertTrue("Non-encrypted procted string in "+desc(), protectedString.isHashed());
        return this;
    }

    public ProtectedStringAsserter<RA> assertHasClearValue() {
        assertNotNull("No clear value procted string in "+desc(), protectedString.getClearValue());
        return this;
    }

    public ProtectedStringAsserter<RA> assertNoClearValue() {
        assertNull("Unexpected clear value procted string in "+desc(), protectedString.getClearValue());
        return this;
    }

    public ProtectedStringAsserter<RA> assertClearValue(String expected) {
        AssertJUnit.assertEquals("Wrong clear value in "+desc(), expected, protectedString.getClearValue());
        return this;
    }

    /**
     * Asserts that protected string is completely equal, including all the randomized things (salt, IV).
     */
    public ProtectedStringAsserter<RA> assertEquals(ProtectedStringType expected) {
        AssertJUnit.assertEquals("Non-equal protected string in "+desc(), expected, protectedString);
        return this;
    }

    /**
     * Checks whether the value inside protected string matches with specified value.
     * Works for all types: encrypted, hashed, clear.
     */
    public ProtectedStringAsserter<RA> assertCompareCleartext(String expected) throws SchemaException, EncryptionException {
        ProtectedStringType expectedPs = new ProtectedStringType();
        expectedPs.setClearValue(expected);
        assertTrue("Wrong value, cannot match in "+desc(), getProtector().compareCleartext(expectedPs, protectedString));
        return this;
    }

    protected String desc() {
        return descWithDetails(protectedString);
    }

    public ProtectedStringAsserter<RA> display() {
        display(desc());
        return this;
    }

    public ProtectedStringAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, protectedString);
        return this;
    }
}
