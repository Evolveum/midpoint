/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import java.util.Collections;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests encryption of legacy password hints copied into delta estimated old values.
 *
 * Verifies that password hints are encrypted before delta validation, while {@code noCrypt}
 * is respected and unrelated protected values remain unchanged.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPasswordHintEstimatedOldValueEncryption extends AbstractLensTest {

    private static final String USER_OID = "00000000-0000-0000-0000-000000001211";

    private static final ItemPath PASSWORD_HINT_PATH = ItemPath.create(
            FocusType.F_CREDENTIALS,
            CredentialsType.F_PASSWORD,
            PasswordType.F_HINT);

    private static final ItemPath PASSWORD_VALUE_PATH = ItemPath.create(
            FocusType.F_CREDENTIALS,
            CredentialsType.F_PASSWORD,
            PasswordType.F_VALUE);

    @Test
    public void test100EncryptsPasswordHintEstimatedOldValue() throws Exception {
        LensFocusContext<UserType> focusContext = focusContextWithPassword(
                new PasswordType().hint(ProtectedStringType.fromClearValue("old hint")));

        PropertyDelta<ProtectedStringType> delta = createHintDeltaWithEncryptedReplacement();

        LensUtil.setDeltaOldValue(focusContext, delta);

        CryptoUtil.checkEncrypted(Collections.singleton(delta));

        ProtectedStringType estimatedOld = delta.getEstimatedOldValues().iterator().next().getValue();

        assertNull("Estimated old clear value remains", estimatedOld.getClearValue());
        assertTrue("Estimated old value is not encrypted", estimatedOld.isEncrypted());
        assertEquals(
                "Wrong estimated old password hint",
                "old hint",
                protector.decryptString(estimatedOld));
    }

    @Test
    public void test110RespectsNoCryptForPasswordHintEstimatedOldValue() throws Exception {
        LensContext<UserType> context = createUserLensContext();
        context.setOptions(ModelExecuteOptions.create().noCrypt(true));

        LensFocusContext<UserType> focusContext = focusContextWithPassword(
                context,
                new PasswordType().hint(ProtectedStringType.fromClearValue("old hint")));

        PropertyDelta<ProtectedStringType> delta = createHintDeltaWithEncryptedReplacement();

        LensUtil.setDeltaOldValue(focusContext, delta);

        ProtectedStringType estimatedOld = delta.getEstimatedOldValues().iterator().next().getValue();

        assertEquals(
                "Estimated old value was encrypted despite noCrypt",
                "old hint",
                estimatedOld.getClearValue());
        assertFalse(
                "Estimated old value is encrypted despite noCrypt",
                estimatedOld.isEncrypted());
    }

    @Test
    public void test120DoesNotEncryptOtherProtectedEstimatedOldValues() throws Exception {
        LensFocusContext<UserType> focusContext = focusContextWithPassword(
                new PasswordType().value(ProtectedStringType.fromClearValue("old password")));

        PropertyDelta<ProtectedStringType> delta = createPasswordValueDeltaWithEncryptedReplacement();

        LensUtil.setDeltaOldValue(focusContext, delta);

        ProtectedStringType estimatedOld = delta.getEstimatedOldValues().iterator().next().getValue();

        assertEquals(
                "Non-hint estimated old value was encrypted",
                "old password",
                estimatedOld.getClearValue());
        assertFalse(
                "Non-hint estimated old value is encrypted",
                estimatedOld.isEncrypted());
    }

    private PropertyDelta<ProtectedStringType> createHintDeltaWithEncryptedReplacement()
            throws Exception {
        return prismContext.deltaFactory().property()
                .createModificationReplaceProperty(
                        PASSWORD_HINT_PATH,
                        getUserDefinition(),
                        protector.encryptString("new hint"));
    }

    private PropertyDelta<ProtectedStringType> createPasswordValueDeltaWithEncryptedReplacement()
            throws Exception {
        return prismContext.deltaFactory().property()
                .createModificationReplaceProperty(
                        PASSWORD_VALUE_PATH,
                        getUserDefinition(),
                        protector.encryptString("new password"));
    }

    private LensFocusContext<UserType> focusContextWithPassword(PasswordType password) {
        return focusContextWithPassword(createUserLensContext(), password);
    }

    private LensFocusContext<UserType> focusContextWithPassword(LensContext<UserType> context, PasswordType password) {

        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();

        focusContext.setInitialObject(
                new UserType()
                        .oid(USER_OID)
                        .name("test")
                        .credentials(
                                new CredentialsType()
                                        .password(password))
                        .asPrismObject());

        return focusContext;
    }

    @Override
    public PrismObjectDefinition<UserType> getUserDefinition() {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    }
}
