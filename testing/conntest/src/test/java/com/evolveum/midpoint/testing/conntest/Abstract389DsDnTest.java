/**
 * Copyright (c) 2014-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.testing.conntest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.namespace.QName;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
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
    public void test025SchemaDn() throws Exception {
		final String TEST_NAME = "test025SchemaDn";
        TestUtil.displayTestTitle(this, TEST_NAME);
        
        ResourceAttributeDefinition<String> dnDef = accountObjectClassDefinition.findAttributeDefinition("dn");
        display("DN defintion", dnDef);
        PrismAsserts.assertDefinition(dnDef, new QName(MidPointConstants.NS_RI, "dn"), DOMUtil.XSD_STRING, 1, 1);
        assertTrue("dn read", dnDef.canRead());
        assertTrue("dn modify", dnDef.canModify());
        assertTrue("dn add", dnDef.canAdd());
        
        assertTrue("Dn is not an identifier", accountObjectClassDefinition.getPrimaryIdentifiers().contains(dnDef));
        assertTrue("Secodary identifiers are not empty: "+accountObjectClassDefinition.getSecondaryIdentifiers(), 
        		accountObjectClassDefinition.getSecondaryIdentifiers().isEmpty());
	}
	
}
