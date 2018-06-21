/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.certification.impl.AccCertQueryHelper;
import com.evolveum.midpoint.certification.impl.AccCertResponseComputationHelper;
import com.evolveum.midpoint.certification.impl.AccCertUpdateHelper;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * @author mederly
 *
 */
public class AbstractUninitializedCertificationTest extends AbstractModelIntegrationTest {

	@Autowired
	private AccCertResponseComputationHelper computationHelper;

	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	public static final File USER_ADMINISTRATOR_PLAIN_FILE = new File(COMMON_DIR, "user-administrator-plain.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_NAME = "administrator";

	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

    @Autowired protected CertificationManagerImpl certificationManager;
	@Autowired protected AccessCertificationService certificationService;
	@Autowired protected AccCertUpdateHelper updateHelper;
	@Autowired protected AccCertQueryHelper queryHelper;

	protected RoleType roleSuperuser;
	protected UserType userAdministrator;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);

		modelService.postInit(initResult);

		// System Configuration
		try {
			repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

		// Administrator
		roleSuperuser = repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult).asObjectable();
		userAdministrator = repoAddObjectFromFile(getUserAdministratorFile(), UserType.class, initResult).asObjectable();
		login(userAdministrator.asPrismObject());
	}

	@NotNull
	protected File getUserAdministratorFile() {
		return USER_ADMINISTRATOR_PLAIN_FILE;
	}

	@NotNull
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

}
