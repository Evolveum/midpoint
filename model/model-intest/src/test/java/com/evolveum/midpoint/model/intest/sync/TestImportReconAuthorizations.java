/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.sync;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.testng.SkipException;

/**
 * Same test as TestImportRecon, but this is using logged-in user with limited
 * authorizations.
 * <p>
 * MID-4822
 *
 * @author Radovan semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImportReconAuthorizations extends TestImportRecon {

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userImporter;
    }

    @Override
    protected void loginImportUser() throws CommonException {
        login(userImporter);
    }

    /**
     * TODO Allow `importer` user to add a task explicitly.
     */
    @Override
    public void test161aImportFromResourceDummyLimeLimitedLegacy() {
        throw new SkipException("TODO");
    }

    /**
     * TODO Allow `importer` user to add a task explicitly.
     */
    @Override
    public void test161bImportFromResourceDummyLimeLimited() {
        throw new SkipException("TODO");
    }
}
