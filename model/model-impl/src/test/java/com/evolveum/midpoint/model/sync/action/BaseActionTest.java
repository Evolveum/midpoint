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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.model.controller.ModelControllerImpl;
import com.evolveum.midpoint.model.controller.SchemaHandler;
import com.evolveum.midpoint.model.sync.Action;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class BaseActionTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/sync");
	protected Action action;
	@Autowired(required = true)
	protected ModelControllerImpl controller;
	@Autowired(required = true)
	protected ProvisioningService provisioning;
	@Autowired(required = true)
	protected RepositoryService repository;
	@Autowired(required = true)
	protected SchemaHandler schemaHandler;

	protected void before(Action action) {
		this.action = action;
		BaseAction base = (BaseAction) this.action;
		base.setModel(controller);
		base.setSchemaHandler(schemaHandler);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullChange() throws Exception {
		action.executeChanges("1", null, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullSituation() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = null;

		action.executeChanges("1", change, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void nullShadowAfterChange() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "change-correct.xml"))).getValue();
		SynchronizationSituationType situation = SynchronizationSituationType.UNMATCHED;

		action.executeChanges("1", change, situation, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(TEST_FOLDER, "change-correct.xml"))).getValue();
		SynchronizationSituationType situation = SynchronizationSituationType.UNMATCHED;

		ObjectChangeAdditionType objectAddition = (ObjectChangeAdditionType) change.getObjectChange();
		ObjectType object = objectAddition.getObject();

		action.executeChanges("1", change, situation, (ResourceObjectShadowType) object, null);
	}
}
