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

/**
 * @author semancik
 *
 */
public class TestOpenLdap extends AbstractLdapConnTest {

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.conntest.AbstractLdapConnTest#getResourceOid()
	 */
	@Override
	protected String getResourceOid() {
		return "2a7c7130-7a34-11e4-bdf6-001e8c717e5b";
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.conntest.AbstractLdapConnTest#getResourceFile()
	 */
	@Override
	protected File getResourceFile() {
		return new File(COMMON_DIR, "resource-openldap.xml");
	}

	@Override
	public String getStartSystemCommand() {
		return "sudo "+getScriptDirectoryName()+"/openldap-start";
	}

	@Override
	public String getStopSystemCommand() {
		return "sudo "+getScriptDirectoryName()+"/openldap-stop";
	}

	@Override
	protected String getLdapServerHost() {
		return "localhost";
	}

	@Override
	protected int getLdapServerPort() {
		return 11389;
	}

	@Override
	protected String getLdapBindDn() {
		return "cn=admin,dc=example,dc=com";
	}

	@Override
	protected String getLdapBindPassword() {
		return "secret";
	}

	@Override
	protected String getAccount0Cn() {
		return "Riwibmix Juvotut (00000000)";
	}
	
}
