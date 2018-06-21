/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.schema;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public abstract class AbstractSchemaTest {

	protected static final File COMMON_DIR = new File("src/test/resources/common");

	public static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
	public static final String USER_JACK_OID = "2f9b9299-6f45-498f-bc8e-8d17c6b93b20";
	public static final String USER_JACK_NAME = "jack";
	public static final long USER_JACK_ASSIGNMENT_ID = 111L;

	public static final File USER_BILL_FILE = new File(COMMON_DIR, "user-bill.xml");

	public static final File ROLE_CONSTRUCTION_FILE = new File(COMMON_DIR, "role-construction.xml");
	public static final String ROLE_CONSTRUCTION_OID = "cc7dd820-b653-11e3-936d-001e8c717e5b";
	public static final long ROLE_CONSTRUCTION_INDUCEMENT_ID = 1001L;
	public static final String ROLE_CONSTRUCTION_RESOURCE_OID = "10000000-0000-0000-0000-000000000004";

	@BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

	protected PrismObjectDefinition<UserType> getUserDefinition() {
		return PrismTestUtil.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}

	protected void displayTestTile(String TEST_NAME) {
		System.out.println("===[ "+TEST_NAME+" ]====");
	}

}
