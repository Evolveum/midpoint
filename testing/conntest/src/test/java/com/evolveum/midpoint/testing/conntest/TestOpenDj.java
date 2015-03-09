/**
 * Copyright (c) 2014-2015 Evolveum
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

import java.io.File;

import org.testng.annotations.AfterClass;

/**
 * @author semancik
 *
 */
public class TestOpenDj extends AbstractLdapConnTest {

	private static final String OPENDJ_TEMPLATE_NAME = "opendj-4000.template";

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.conntest.AbstractLdapConnTest#getResourceOid()
	 */
	@Override
	protected String getResourceOid() {
		return "371ffc38-c424-11e4-8467-001e8c717e5b";
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.conntest.AbstractLdapConnTest#getResourceFile()
	 */
	@Override
	protected File getResourceFile() {
		return new File(COMMON_DIR, "resource-opendj.xml");
	}

	@Override
	public String getStartSystemCommand() {
		return null;
	}

	@Override
	public String getStopSystemCommand() {
		return null;
	}
	
	@Override
	protected void startResources() throws Exception {
		super.startResources();
		openDJController.startCleanServer(OPENDJ_TEMPLATE_NAME);
	}
	
	@AfterClass
    public static void stopResources() throws Exception {
		AbstractLdapConnTest.stopResources();
		openDJController.stop();
	}

	@Override
	protected String getLdapServerHost() {
		return "localhost";
	}

	@Override
	protected int getLdapServerPort() {
		return 10389;
	}

	@Override
	protected String getLdapBindDn() {
		return "cn=directory manager";
	}

	@Override
	protected String getLdapBindPassword() {
		return "secret";
	}
	
	@Override
	protected String getAccount0Cn() {
		return "Warlaz Kunjegjul (00000000)";
	}

}
