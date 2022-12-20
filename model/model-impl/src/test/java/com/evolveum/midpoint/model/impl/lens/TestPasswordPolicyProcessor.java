/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestPasswordPolicyProcessor<F extends FocusType> extends AbstractLensTest {

    static final File TEST_DIR = new File(AbstractLensTest.TEST_DIR, "ppolicy");

    private static final String OLD_PASSWORD = USER_JACK_PASSWORD;

    private static final String PASSWORD1 = "ch4nGedPa33word1";
    private static final String PASSWORD2 = "ch4nGedPa33word2";
    private static final String PASSWORD3 = "ch4nGedPa33word3";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    abstract Class<F> getType();

    abstract TestResource<?> getTestResource();

    private String getOid() {
        return getTestResource().oid;
    }

    @Test
    public void test000initPasswordPolicyForHistory() throws Exception {
        setPasswordHistoryLength(3);
    }

    @Test
    public void test100CreateFocusWithPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        addObject(getTestResource(), task, result);

        // THEN
        PrismObject<F> focusAfter = getFocus();
        assertPasswordHistoryEntries(focusAfter);
    }

    @NotNull
    private PrismObject<F> getFocus() throws Exception {
        PrismObject<F> focusAfter = getObject(getType(), getOid());
        assertNotNull("Focus was not found.", focusAfter);
        return focusAfter;
    }

    @Test
    public void test110ModifyPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyFocusChangePassword(getType(), getOid(), PASSWORD1, task, result);

        // THEN
        PrismObject<F> focusAfter = getFocus();

        F focusBeanAfter = focusAfter.asObjectable();
        CredentialsType credentialsAfter = focusBeanAfter.getCredentials();
        assertNotNull("No credentials found", credentialsAfter);

        PasswordType passwordAfter = credentialsAfter.getPassword();
        assertNotNull("No password found", passwordAfter);
        ProtectedStringType passwordValueAfter = passwordAfter.getValue();
        assertNotNull("Password mustn't be null", passwordValueAfter);
        assertPasswords(PASSWORD1, passwordValueAfter);
        assertPasswordHistoryEntries(passwordAfter, OLD_PASSWORD);
    }

    @Test
    public void test120ModifyPasswordSecondTime() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyFocusChangePassword(getType(), getOid(), PASSWORD2, task, result);

        // THEN
        PrismObject<F> focusAfter = getFocus();

        F focusBeanAfter = focusAfter.asObjectable();
        CredentialsType credentialsAfter = focusBeanAfter.getCredentials();
        assertNotNull("No credentials found", credentialsAfter);

        PasswordType passwordAfter = credentialsAfter.getPassword();
        assertNotNull("No password found", passwordAfter);
        ProtectedStringType passwordValueAfter = passwordAfter.getValue();
        assertNotNull("Password mustn't be null", passwordValueAfter);
        assertPasswords(PASSWORD2, passwordValueAfter);
        assertPasswordHistoryEntries(passwordAfter, OLD_PASSWORD, PASSWORD1);
    }

    @Test
    public void test130ModifyPasswordThirdTime() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyFocusChangePassword(getType(), getOid(), PASSWORD3, task, result);

        // THEN
        PrismObject<F> focusAfter = getFocus();

        F focusBeanAfter = focusAfter.asObjectable();
        CredentialsType credentialsAfter = focusBeanAfter.getCredentials();
        assertNotNull("No credentials found", credentialsAfter);

        PasswordType passwordAfter = credentialsAfter.getPassword();
        assertNotNull("No password found", passwordAfter);
        ProtectedStringType passwordValueAfter = passwordAfter.getValue();
        assertNotNull("Password mustn't be null", passwordValueAfter);
        assertPasswords(PASSWORD3, passwordValueAfter);
        assertPasswordHistoryEntries(passwordAfter, PASSWORD1, PASSWORD2);
    }

    @Test
    public void test140ModifyPasswordOldPassword1() throws Exception {
        doTestModifyPasswordExpectFailure(PASSWORD1);
    }

    @Test
    public void test150ModifyPasswordOldPassword2() throws Exception {
        doTestModifyPasswordExpectFailure(PASSWORD2);
    }

    @Test
    public void test160ModifyPasswordSamePassword3() throws Exception {
        doTestModifyPasswordExpectFailure(PASSWORD3);
    }

    private void doTestModifyPasswordExpectFailure(String password) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        try {
            // WHEN
            modifyFocusChangePassword(getType(), getOid(), password, task, result);

            fail("Expected PolicyViolationException but didn't get one.");
        } catch (PolicyViolationException ex) {
            displayExpectedException(ex);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    @Test
    public void test200InitNoHistoryPasswordPolicy() throws Exception {
        setPasswordHistoryLength(0);
    }

    @Test
    public void test201deleteFocus() throws Exception {
        // WHEN
        deleteObject(getType(), getOid());

        try {
            getObject(getType(), getOid());
            fail("Unexpected focus object, should be deleted.");
        } catch (ObjectNotFoundException ex) {
            // this is OK;
        }
    }

    @Test
    public void test210CreateFocusNoPasswordHistory() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        addObject(getTestResource(), task, result);

        // THEN
        PrismObject<F> focus = getFocus();

        F focusBean = focus.asObjectable();
        CredentialsType credentials = focusBean.getCredentials();
        assertNotNull("Focus has no credentials", credentials);

        PasswordType password = credentials.getPassword();
        assertNotNull("Focus has no password", password);

        List<PasswordHistoryEntryType> historyEntries = password.getHistoryEntry();
        assertEquals("Wrong # of history entries", 0, historyEntries.size());
    }

    @Test
    public void test220ModifyPasswordNoPasswordHistory() throws Exception {
        modifyPasswordNoHistory();
    }

    @Test
    public void test230ModifySamePasswordNoPasswordHistory() throws Exception {
        modifyPasswordNoHistory();
    }

    private void modifyPasswordNoHistory() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        ProtectedStringType newValue = new ProtectedStringType();
        newValue.setClearValue("n0Hist0ryEntr7");

        modifyObjectReplaceProperty(getType(), getOid(), PATH_CREDENTIALS_PASSWORD_VALUE, task, result, newValue);

        // THEN
        PrismObject<F> focusAfter = getFocus();

        F focusBeanAfter = focusAfter.asObjectable();
        CredentialsType credentialsAfter = focusBeanAfter.getCredentials();
        assertNotNull("Focus has no credentials", credentialsAfter);

        PasswordType passwordAfter = credentialsAfter.getPassword();
        assertNotNull("Focus has no password", passwordAfter);

        List<PasswordHistoryEntryType> historyEntries = passwordAfter.getHistoryEntry();
        assertEquals("Wrong # of history entries", 0, historyEntries.size());
    }

    private void assertPasswords(String password, ProtectedStringType passwordAfterChange) throws SchemaException, EncryptionException {
        ProtectedStringType protectedStringType = new ProtectedStringType();
        protectedStringType.setClearValue(password);
        AssertJUnit.assertTrue("Password doesn't match",
                protector.compareCleartext(protectedStringType, passwordAfterChange));
    }

    private void setPasswordHistoryLength(int historyLength) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        modifyObjectReplaceProperty(SecurityPolicyType.class, SECURITY_POLICY_OID,
                SchemaConstants.PATH_CREDENTIALS_PASSWORD_HISTORY_LENGTH, task, result, historyLength);
    }
}
