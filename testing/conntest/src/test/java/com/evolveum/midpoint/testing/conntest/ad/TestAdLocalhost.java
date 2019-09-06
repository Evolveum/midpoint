/**
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import java.io.File;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * AD test to run manually. Assumess connector server on localhost (or a tunneled connection).
 *
 * @author semancik
 *
 */
public class TestAdLocalhost extends AbstractAdTest {

	@Override
	protected File getConnectorHostFile() {
		return new File(getBaseDir(), "connector-host-localhost.xml");
	}

	@Override
	protected String getLdapServerHost() {
		return "localhost";
	}

	@Override
	protected int getLdapServerPort() {
		return 44389;
	}

}
