/**
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.text.ParseException;

import org.apache.directory.api.util.GeneralizedTime;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * OpenLDAP, but without permissive modify, shortcut attributes, etc.
 *
 * @author semancik
 */
public class TestOpenLdapDumber extends TestOpenLdap {

	@Override
	protected File getBaseDir() {
		return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "openldap-dumber");
	}

	@Override
	protected boolean hasAssociationShortcut() {
		return false;
	}

	@Override
	protected boolean isUsingGroupShortcutAttribute() {
		return false;
	}
	
	// This is a dumb resource. It cannot count.
	@Override
	protected void assertCountAllAccounts(Integer count) {
		assertEquals("Wrong account count", (Integer)null, count);
	}

}
