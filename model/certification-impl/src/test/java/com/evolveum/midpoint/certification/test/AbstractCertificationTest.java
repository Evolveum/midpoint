/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * @author mederly
 *
 */
public class AbstractCertificationTest extends AbstractModelIntegrationTest {
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_NAME = "administrator";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	
	protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";
	
	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    protected static final File CERT_TYPE_USER_ROLE_BASIC_FILE = new File(COMMON_DIR, "user-role-basic-certification.xml");
    protected static final String CERT_TYPE_USER_ROLE_BASIC_OID = "33333333-0000-0000-0000-000000000001";
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

    @Autowired
    protected CertificationManagerImpl certificationManager;

	protected PrismObject<UserType> userAdministrator;
	
	protected UserType userTypeJack;

    protected PrismObject<AccessCertificationTypeType> userRoleBasicCertificationType;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);

		modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
				
		// Administrator
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult);
		login(userAdministrator);
		
		// Users
		userTypeJack = repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult).asObjectable();

        // Certifications
        userRoleBasicCertificationType = repoAddObjectFromFile(CERT_TYPE_USER_ROLE_BASIC_FILE, AccessCertificationTypeType.class, initResult);
	}
	

}
