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
package com.evolveum.midpoint.model.sync;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class SynchronizationServiceTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/sync");
	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceTest.class);
	@Autowired(required = true)
	private transient ResourceObjectChangeListener synchronizationService;

	@Test(expected = IllegalArgumentException.class)
	public void nullChange() {
		synchronizationService.notifyChange(null, new OperationResult("Test Operation"));
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void nullChangeResource() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "change-without-resource.xml"))).getValue();
		synchronizationService.notifyChange(change, new OperationResult("Test Operation"));
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void nullChangeObject() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "change-without-object.xml"))).getValue();
		synchronizationService.notifyChange(change, new OperationResult("Test Operation"));
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "change-correct.xml"))).getValue();
		synchronizationService.notifyChange(change, null);
	}
}
