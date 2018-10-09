/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.testing.story.security;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests for privacy-enhancing setup. E.g. broad get authorizations, but limited search.
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPrivacy extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "security/privacy");
	
	protected static final File USERS_FILE = new File(TEST_DIR, "users.xml");
	
	protected static final String USER_GUYBRUSH_OID = "0cf84e54-b815-11e8-9862-a7c904bd4e94";
	protected static final String USER_ELAINE_OID = "30444e02-b816-11e8-a26d-0380f27eebe6";
	protected static final String USER_RAPP_OID = "353265f2-b816-11e8-91c7-333c643c8719";

	protected static final File ROLE_PRIVACY_END_USER_FILE = new File(TEST_DIR, "role-privacy-end-user.xml");
	protected static final String ROLE_PRIVACY_END_USER_OID = "d6f2c30a-b816-11e8-88c5-4f735c761a81";
	
	protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	protected static final String RESOURCE_DUMMY_OID = "dfc012e2-b813-11e8-82af-679b6f0a6ad4";
	private static final String RESOURCE_DUMMY_NS = MidPointConstants.NS_RI;

	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
		getDummyResource().setSyncStyle(DummySyncStyle.SMART);
		
		repoAddObjectFromFile(ROLE_PRIVACY_END_USER_FILE, initResult);
		
		importObjectsFromFileNotRaw(USERS_FILE, initTask, initResult);
	}

	/**
	 * MID-4892
	 */
	@Test
	public void test100AutzJackReadSearch() throws Exception {
		final String TEST_NAME = "test100AutzJackReadSearch";
		displayTestTitle(TEST_NAME);
		
		assignRole(USER_JACK_OID, ROLE_PRIVACY_END_USER_OID);
		
		login(USER_JACK_USERNAME);

		// WHEN
        displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_ELAINE_OID);
        assertGetAllow(UserType.class, USER_RAPP_OID);
		
        assertSearch(UserType.class, null, 
        		USER_ADMINISTRATOR_OID, USER_GUYBRUSH_OID, USER_ELAINE_OID, USER_JACK_OID);
		
		// THEN
		displayThen(TEST_NAME);
		
	}

}
