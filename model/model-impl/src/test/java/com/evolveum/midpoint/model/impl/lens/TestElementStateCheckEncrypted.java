/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.fail;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests backward-compatible encryption validation of legacy clear-text password hints in {@link ElementState}.
 *
 * Verifies that an unchanged legacy hint is tolerated during object-state validation, while newly introduced
 * or changed clear-text hints are still rejected by the normal encryption checks.
 */
public class TestElementStateCheckEncrypted extends AbstractUnitTest {

    private static final String USER_OID = "00000000-0000-0000-0000-000000001211";

    private static final ItemPath PASSWORD_HINT_PATH = ItemPath.create(
            FocusType.F_CREDENTIALS,
            CredentialsType.F_PASSWORD,
            PasswordType.F_HINT);

    @BeforeSuite
    public void setup() throws Exception {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testLegacyClearHintWithUnrelatedDescriptionDeltaIsTolerated() throws Exception {
        ElementState<UserType> state = elementStateWithInitialUser("legacy hint");
        state.setPrimaryDelta(
                deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION)
                        .replace("unrelated edit")
                        .asObjectDelta(USER_OID));
        state.getNewObject();

        state.checkEncrypted();
    }

    @Test
    public void testLegacyClearHintWithDirectHintDeltaIsRejected() throws Exception {
        ElementState<UserType> state = elementStateWithInitialUser("legacy hint");
        state.setPrimaryDelta(
                deltaFor(UserType.class)
                        .item(PASSWORD_HINT_PATH)
                        .replace(ProtectedStringType.fromClearValue("changed hint"))
                        .asObjectDelta(USER_OID));

        assertCheckEncryptedFails(state);
    }

    @Test
    public void testDifferentOldAndCurrentLegacyClearHintsAreRejected() {
        ElementState<UserType> state = elementState(UserType.class);
        state.setInitialObject(userWithHint("old hint"));
        state.setCurrentObject(userWithHint("current hint"));

        assertCheckEncryptedFails(state);
    }

    @Test
    public void testEncryptedHintWithUnrelatedDescriptionDeltaIsAccepted() throws Exception {
        ElementState<UserType> state = elementState(UserType.class);
        state.setInitialObject(userWithHint(encryptedProtectedString()));
        state.setPrimaryDelta(
                deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION)
                        .replace("unrelated edit")
                        .asObjectDelta(USER_OID));
        state.getNewObject();

        state.checkEncrypted();
    }

    private ElementState<UserType> elementStateWithInitialUser(String clearHint) {
        ElementState<UserType> state = elementState(UserType.class);
        state.setInitialObject(userWithHint(clearHint));
        return state;
    }

    private <O extends ObjectType> ElementState<O> elementState(Class<O> objectTypeClass) {
        return new ElementState<>(objectTypeClass, object -> object, definition -> definition);
    }

    private PrismObject<UserType> userWithHint(String clearHint) {
        return userWithHint(ProtectedStringType.fromClearValue(clearHint));
    }

    private PrismObject<UserType> userWithHint(ProtectedStringType hint) {
        return new UserType()
                .name("test")
                .oid(USER_OID)
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .hint(hint)))
                .asPrismObject();
    }

    private ProtectedStringType encryptedProtectedString() {
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setEncryptedData(new EncryptedDataType());
        return protectedString;
    }

    private <O extends ObjectType> S_ItemEntry deltaFor(Class<O> objectTypeClass) throws SchemaException {
        return PrismContext.get().deltaFor(objectTypeClass);
    }

    private void assertCheckEncryptedFails(ElementState<?> state) {
        try {
            state.checkEncrypted();
            fail("Unexpected encrypted-value check success");
        } catch (IllegalStateException e) {
            displayExpectedException(e);
        }
    }
}
