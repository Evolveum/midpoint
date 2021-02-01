/*
 * Copyright (c) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.opendj;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * OpenDJ, but without permissive modify, shortcut attributes, with manual matching rules, etc.
 * Uses SPR instead of VLV for paging.
 * Also has additional search filter.
 * Uses unbind operation on disconnect.
 *
 * @author semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOpenDjDumber extends AbstractOpenDjNoiseTest {

    private static final int INITIAL_SYNC_TOKEN = 21;

    @Override
    protected File getBaseDir() {
        return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "opendj-dumber");
    }

    @Override
    protected boolean hasAssociationShortcut() {
        return false;
    }

    @Override
    protected int getInitialSyncToken() {
        return INITIAL_SYNC_TOKEN;
    }

    @Override
    protected boolean isUsingGroupShortcutAttribute() {
        return false;
    }

    @Override
    protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd) throws ObjectNotFoundException, SchemaException {
        // TODO: assert timistamp
    }

    @Override
    protected void assertApproxNumberOfAllResults(SearchResultMetadata searchMetadata, Integer expectedNumber) {
        // We are using SPR, it never returns this. Nothing to assert.
    }

    @Test
    public void test818DeleteAccountHtm() throws Exception {
        // Nothing to do. This won't work with modifytimestamp sync.
    }

    @Test
    public void test837RenameAccount() throws Exception {
        // Nothing to do. This won't work with modifytimestamp sync.
    }

    @Test
    public void test838DeleteAccountHtm() throws Exception {
        // Nothing to do. This won't work with modifytimestamp sync.
    }
}
