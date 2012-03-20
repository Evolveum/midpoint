/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class AbstractModelIntegrationTest extends AbstractIntegrationTest {

	protected static final QName ICFS_NAME = new QName("http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-1.xsd","name");
	
	protected static final String COMMON_DIR_NAME = "src/test/resources/common";
	
	protected static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_NAME + "/system-configuration.xml";
	protected static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";
	
	protected static final String USER_TEMPLATE_FILENAME = COMMON_DIR_NAME + "/user-template.xml";
	protected static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	protected static final String CONNECTOR_LDAP_FILENAME = COMMON_DIR_NAME + "/connector-ldap.xml";
	protected static final String CONNECTOR_DBTABLE_FILENAME = COMMON_DIR_NAME + "/connector-dbtable.xml";
	
	protected static final String RESOURCE_OPENDJ_FILENAME = COMMON_DIR_NAME + "/resource-opendj.xml";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	
	protected static final String ROLE_ALPHA_FILENAME = COMMON_DIR_NAME + "/role-alpha.xml";
	protected static final String ROLE_ALPHA_OID = "12345678-d34d-b33f-f00d-55555555aaaa";

	protected static final String ROLE_BETA_FILENAME = COMMON_DIR_NAME + "/role-beta.xml";
	protected static final String ROLE_BETA_OID = "12345678-d34d-b33f-f00d-55555555bbbb";

	protected static final String USER_JACK_FILENAME = COMMON_DIR_NAME + "/user-jack.xml";
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	protected static final String USER_BARBOSSA_FILENAME = COMMON_DIR_NAME + "/user-barbossa.xml";
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";

	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-hbarbossa-opendj.xml";
	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_OID = "c0c010c0-d34d-b33f-f00d-222211111112";

	@Autowired(required = true)
	protected ModelService modelService;
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected ResourceType resourceType;
	
	public AbstractModelIntegrationTest() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		
		addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, initResult);
		addObjectFromFile(USER_TEMPLATE_FILENAME, initResult);

		// Connectors
		addObjectFromFile(CONNECTOR_LDAP_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DBTABLE_FILENAME, ConnectorType.class, initResult);
		
		// Resources
		resourceType = addObjectFromFile(RESOURCE_OPENDJ_FILENAME, ResourceType.class, initResult).asObjectable();

		// Users
		userTypeJack = addObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = addObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		
		// Accounts
		addObjectFromFile(ACCOUNT_HBARBOSSA_OPENDJ_FILENAME, initResult);
		

	}
	
	private void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
	}

}
