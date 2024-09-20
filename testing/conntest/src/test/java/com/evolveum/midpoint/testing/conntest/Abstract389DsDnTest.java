/*
 * Copyright (c) 2014-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_DN;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.util.DOMUtil;

/**
 * @author semancik
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class Abstract389DsDnTest extends Abstract389DsTest {

    @Override
    public String getPrimaryIdentifierAttributeName() {
        return "dn";
    }

    @Override
    protected boolean syncCanDetectDelete() {
        return true;
    }

    @Override
    protected boolean isUsingGroupShortcutAttribute() {
        return false;
    }

    @Test
    public void test025SchemaDn() {
        ShadowSimpleAttributeDefinition<?> dnDef = accountDefinition.findSimpleAttributeDefinition("dn");
        displayDumpable("DN definition", dnDef);
        PrismAsserts.assertDefinition(dnDef, QNAME_DN, DOMUtil.XSD_STRING, 1, 1);
        assertTrue("dn read", dnDef.canRead());
        assertTrue("dn modify", dnDef.canModify());
        assertTrue("dn add", dnDef.canAdd());

        assertTrue("Dn is not an identifier", accountDefinition.getPrimaryIdentifiers().contains(dnDef));
        assertTrue("Secondary identifiers are not empty: "+ accountDefinition.getSecondaryIdentifiers(),
                accountDefinition.getSecondaryIdentifiers().isEmpty());
    }
}
