/*
 * Copyright (c) 2014-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
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
        ResourceAttributeDefinition<String> dnDef = accountObjectClassDefinition.findAttributeDefinition("dn");
        displayDumpable("DN defintion", dnDef);
        PrismAsserts.assertDefinition(dnDef, new QName(MidPointConstants.NS_RI, "dn"), DOMUtil.XSD_STRING, 1, 1);
        assertTrue("dn read", dnDef.canRead());
        assertTrue("dn modify", dnDef.canModify());
        assertTrue("dn add", dnDef.canAdd());

        assertTrue("Dn is not an identifier", accountObjectClassDefinition.getPrimaryIdentifiers().contains(dnDef));
        assertTrue("Secodary identifiers are not empty: "+accountObjectClassDefinition.getSecondaryIdentifiers(),
                accountObjectClassDefinition.getSecondaryIdentifiers().isEmpty());
    }
}
