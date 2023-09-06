/*
 * Copyright (c) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.opendj;

import java.io.File;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DOMUtil;

import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_DN;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Almost same sa TestOpenDj, but using DN as the only identifier (NOT using entryUUID).
 *
 * MID-6884
 *
 * @author semancik
 */
public class TestOpenDjDn extends AbstractOpenDjNoiseTest {

    private static final int INITIAL_SYNC_TOKEN = 25;

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-dn.xml");
    }

    @Override
    protected int getInitialSyncToken() { return INITIAL_SYNC_TOKEN; }

    @Override
    public String getPrimaryIdentifierAttributeName() {
        return "dn";
    }

    @Override
    protected File getShadowBilboFile() {
        return new File(getBaseDir(),  "shadow-bilbo-dn.xml");
    }

    @Test
    public void test025SchemaDn() {
        ResourceAttributeDefinition<?> dnDef = accountDefinition.findAttributeDefinition("dn");
        displayDumpable("DN defintion", dnDef);
        PrismAsserts.assertDefinition(dnDef, QNAME_DN, DOMUtil.XSD_STRING, 1, 1);
        assertTrue("dn read", dnDef.canRead());
        assertTrue("dn modify", dnDef.canModify());
        assertTrue("dn add", dnDef.canAdd());

        assertTrue("Dn is not an identifier", accountDefinition.getPrimaryIdentifiers().contains(dnDef));
        assertTrue("Secodary identifiers are not empty: "+ accountDefinition.getSecondaryIdentifiers(),
                accountDefinition.getSecondaryIdentifiers().isEmpty());
    }

    // MID-6515
    /**
     * This addition should be filtered out by additionalSearchFilter.
     */
    @Test
    public void test840SyncAddAccountChaos() throws Exception {
        // See MID-6515
    }
}
