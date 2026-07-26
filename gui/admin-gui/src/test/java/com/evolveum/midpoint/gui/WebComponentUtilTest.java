/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Tests detection of operations submitted for workflow approval.
 *
 * An {@link OperationResult} being in progress is not sufficient to identify
 * approval processing. The workflow path additionally records the approval case
 * OID in the result, so both conditions must be present.
 */
public class WebComponentUtilTest {

    private static final String CASE_OID = "00000000-0000-0000-0000-000000000034";

    @Test
    public void testIsOperationSubmittedForApproval() {
        assertFalse(WebComponentUtil.isOperationSubmittedForApproval(null));

        OperationResult success = new OperationResult("success");
        success.recordSuccess();
        assertFalse(WebComponentUtil.isOperationSubmittedForApproval(success));

        OperationResult inProgressWithoutCaseOid = new OperationResult("inProgressWithoutCaseOid");
        inProgressWithoutCaseOid.setInProgress();
        assertFalse(WebComponentUtil.isOperationSubmittedForApproval(inProgressWithoutCaseOid));

        OperationResult inProgressWithCaseOid = new OperationResult("inProgressWithCaseOid");
        inProgressWithCaseOid.setInProgress();
        inProgressWithCaseOid.setCaseOid(CASE_OID);
        assertTrue(WebComponentUtil.isOperationSubmittedForApproval(inProgressWithCaseOid));

        OperationResult successWithCaseOid = new OperationResult("successWithCaseOid");
        successWithCaseOid.recordSuccess();
        successWithCaseOid.setCaseOid(CASE_OID);
        assertFalse(WebComponentUtil.isOperationSubmittedForApproval(successWithCaseOid));
    }
}
