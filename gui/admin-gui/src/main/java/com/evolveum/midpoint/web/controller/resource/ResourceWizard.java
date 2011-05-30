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
package com.evolveum.midpoint.web.controller.resource;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.controller.wizard.Wizard;
import com.evolveum.midpoint.web.controller.wizard.WizardPage;

/**
 * 
 * @author lazyman
 * 
 */
@Controller
@Scope("session")
public class ResourceWizard extends Wizard {

	static final String PAGE_NAVIGATION_BASE = "/resource/create";
	private static final long serialVersionUID = -8327099988202610912L;
	@Autowired(required = true)
	private ResourceCreateController create;
	@Autowired(required = true)
	private ResourceConfigurationController configuration;
	@Autowired(required = true)
	private SchemaReviewController schemaReview;
	@Autowired(required = true)
	private SchemaHandlingController schemaHandling;
	@Autowired(required = true)
	private SynchronizationController synchronization;

	public ResourceWizard() {
		super("/resource/index");
	}

	@Override
	public List<WizardPage> getPages() {
		if (super.getPages().isEmpty()) {
			super.getPages().add(create);
			super.getPages().add(configuration);
			super.getPages().add(schemaReview);
			super.getPages().add(schemaHandling);
			super.getPages().add(synchronization);
		}
		return super.getPages();
	}

	@Override
	public String finish() {

		return super.finish();
	}
}
