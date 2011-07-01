/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.sync.action;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-provisioning.xml" })
public class AddUserActionTest extends BaseActionTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/sync/action/addUser");
	private static final Trace LOGGER = TraceManager.getTrace(AddUserActionTest.class);

	@Before
	public void before() {
		before(new AddUserAction());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testUserExists() throws SynchronizationException, JAXBException {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "existing-user.xml"))).getValue();
		OperationResult result = new OperationResult("Add User Action Test");

		//TODO: finish mocking repository
		try {
			ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
			action.executeChanges("1", change, SynchronizationSituationType.UNMATCHED,
					(ResourceObjectShadowType) addition.getObject(), result);
		} finally {
			LOGGER.debug(result.debugDump());
		}
	}
}
