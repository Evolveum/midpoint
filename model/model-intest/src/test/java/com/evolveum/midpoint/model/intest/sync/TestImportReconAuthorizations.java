/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
}
