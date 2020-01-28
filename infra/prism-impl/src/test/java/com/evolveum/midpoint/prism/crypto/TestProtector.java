/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.crypto;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestProtector {

    private static final Trace LOGGER = TraceManager.getTrace(TestProtector.class);

    @Test
    public void testProtectorEncryptionRoundTrip() throws Exception {
        String value = "someValue";

        Protector protector256 = PrismInternalTestUtil.createProtector(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC);
        Protector protector128 = PrismInternalTestUtil.createProtector(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES128_CBC);

        ProtectedStringType pdt = new ProtectedStringType();
        pdt.setClearValue(value);
        assertFalse(pdt.isEmpty());
        assertFalse(pdt.isHashed());
        assertFalse(pdt.isEncrypted());

        // WHEN
        protector256.encrypt(pdt);

        // THEN
        assertFalse(pdt.isEmpty());
        assertTrue(pdt.isEncrypted());
        assertFalse(pdt.isHashed());
        assertNull(pdt.getClearValue());

        // WHEN
        protector128.decrypt(pdt);

        // THEN
        assertFalse(pdt.isEmpty());
        assertFalse(pdt.isEncrypted());
        assertFalse(pdt.isHashed());
        AssertJUnit.assertEquals(value, pdt.getClearValue());

        // WHEN
        ProtectedStringType pstEnc = protector256.encryptString(value);

        // THEN
        assertFalse(pstEnc.isEmpty());
        assertTrue(pstEnc.isEncrypted());
        assertFalse(pstEnc.isHashed());

        // WHEN
        String clear = protector256.decryptString(pstEnc);
        assertNotNull(clear);

        // THEN
        AssertJUnit.assertEquals(value, clear);

        // WHEN
        boolean compare1 = protector256.compareCleartext(pdt, pstEnc);

        // THEN
        assertTrue("compare1 failed", compare1);

        // WHEN
        boolean compare2 = protector256.compareCleartext(pstEnc, pdt);

        // THEN
        assertTrue("compare2 failed", compare2);

        ProtectedStringType wrongPst = new ProtectedStringType();
        wrongPst.setClearValue("nonono This is not it");

        // WHEN
        boolean compare5 = protector256.compareCleartext(pdt, wrongPst);

        // THEN
        assertFalse("compare5 unexpected success", compare5);

        // WHEN
        boolean compare6 = protector256.compareCleartext(wrongPst, pdt);

        // THEN
        assertFalse("compare6 unexpected success", compare6);
    }

    @Test
    public void testProtectorHashRoundTrip() throws Exception {
        String value = "someValue";
        ProtectedStringType pst = new ProtectedStringType();
        pst.setClearValue(value);
        assertFalse(pst.isEmpty());

        Protector protector256 = PrismInternalTestUtil.createProtector(Protector.XMLSEC_ENCRYPTION_ALGORITHM_AES256_CBC);

        // WHEN
        protector256.hash(pst);

        // THEN
        assertFalse(pst.isEmpty());
        assertTrue(pst.isHashed());
        assertFalse(pst.isEncrypted());
        assertNull(pst.getClearValue());

        ProtectedStringType checkPstClear = new ProtectedStringType();
        checkPstClear.setClearValue(value);

        // WHEN
        boolean compare1 = protector256.compareCleartext(pst, checkPstClear);

        // THEN
        assertTrue("compare1 failed", compare1);

        // WHEN
        boolean compare2 = protector256.compareCleartext(checkPstClear, pst);

        // THEN
        assertTrue("compare2 failed", compare2);

        ProtectedStringType checkPstEnc = new ProtectedStringType();
        checkPstEnc.setClearValue(value);
        protector256.encrypt(checkPstEnc);

        // WHEN
        boolean compare3 = protector256.compareCleartext(pst, checkPstEnc);

        // THEN
        assertTrue("compare3 failed", compare3);

        // WHEN
        boolean compare4 = protector256.compareCleartext(checkPstEnc, pst);

        // THEN
        assertTrue("compare4 failed", compare4);

        ProtectedStringType wrongPst = new ProtectedStringType();
        wrongPst.setClearValue("nonono This is not it");

        // WHEN
        boolean compare5 = protector256.compareCleartext(pst, wrongPst);

        // THEN
        assertFalse("compare5 unexpected success", compare5);

        // WHEN
        boolean compare6 = protector256.compareCleartext(wrongPst, pst);

        // THEN
        assertFalse("compare6 unexpected success", compare6);

        ProtectedStringType wrongPstEnc = new ProtectedStringType();
        wrongPstEnc.setClearValue("nonono This is not it");
        protector256.encrypt(wrongPstEnc);

        // WHEN
        boolean compare7 = protector256.compareCleartext(pst, wrongPstEnc);

        // THEN
        assertFalse("compare7 unexpected success", compare7);

        // WHEN
        boolean compare8 = protector256.compareCleartext(wrongPstEnc, pst);

        // THEN
        assertFalse("compare8 unexpected success", compare8);

        // change the hash ... comparison should fail
        pst.getHashedDataType().getDigestValue()[1] = 0x12;

        // WHEN
        boolean compare9 = protector256.compareCleartext(pst, checkPstClear);

        // THEN
        assertFalse("compare9 unexpected success", compare9);

        // WHEN
        boolean compare10 = protector256.compareCleartext(checkPstClear, pst);

        // THEN
        assertFalse("compare10 unexpected success", compare10);

        ProtectedStringType pstEncHash = new ProtectedStringType();
        pstEncHash.setClearValue(value);
        assertFalse(pstEncHash.isEmpty());
        protector256.encrypt(pstEncHash);

        // WHEN
        protector256.hash(pstEncHash);

        // THEN
        assertFalse(pstEncHash.isEmpty());
        assertTrue(pstEncHash.isHashed());
        assertFalse(pstEncHash.isEncrypted());
        assertNull(pstEncHash.getClearValue());

        // WHEN
        boolean compare1e = protector256.compareCleartext(checkPstClear, pstEncHash);

        // THEN
        assertTrue("compare1e failed", compare1e);

        // WHEN
        boolean compare2e = protector256.compareCleartext(pstEncHash, checkPstClear);

        // THEN
        assertTrue("compare2e failed", compare2e);

        // WHEN
        boolean compare3e = protector256.compareCleartext(pstEncHash, checkPstEnc);

        // THEN
        assertTrue("compare3e failed", compare3e);

        // WHEN
        boolean compare4e = protector256.compareCleartext(checkPstEnc, pstEncHash);

        // THEN
        assertTrue("compare4e failed", compare4e);
    }
}
