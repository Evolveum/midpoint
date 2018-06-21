/**
 * Copyright (c) 2015-2016 Evolveum
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
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestAdLdapMedusa extends AbstractAdLdapCookedTest {

	@Override
	protected String getResourceOid() {
		return "eced6d24-73e3-11e5-8457-93eff15a6b85";
	}

	@Override
	protected File getResourceFile() {
		return new File(getBaseDir(), "resource-medusa.xml");
	}

	@Override
	protected String getLdapServerHost() {
		return "medusa.lab.evolveum.com";
	}

	@Override
	protected int getLdapServerPort() {
		return 636;
	}

}
