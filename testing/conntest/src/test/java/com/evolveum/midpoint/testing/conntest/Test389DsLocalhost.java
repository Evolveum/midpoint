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
public class Test389DsLocalhost extends AbstractLdapConnTest {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.conntest.AbstractLdapConnTest#getResourceOid()
	 */
	@Override
	protected String getResourceOid() {
		return "eaee8a88-ce54-11e4-a311-001e8c717e5b";
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.conntest.AbstractLdapConnTest#getResourceFile()
	 */
	@Override
	protected File getResourceFile() {
		return new File(COMMON_DIR, "resource-389ds-localhost.xml");
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
	protected String getLdapServerHost() {
		return "localhost";
	}

	@Override
	protected int getLdapServerPort() {
		return 2389;
	}

	@Override
	protected String getLdapBindDn() {
		return "cn=directory manager";
	}

	@Override
	protected String getLdapBindPassword() {
		return "secret123";
	}
	
	@Override
	protected String getAccount0Cn() {
		return "Warlaz Kunjegjul (00000000)";
	}

}
