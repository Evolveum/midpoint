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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.component.form.AutoForm;
import com.evolveum.midpoint.web.controller.util.WizardPage;

@Controller("resourceConfiguration")
@Scope("session")
public class ResourceConfigurationController extends WizardPage {

	private static final long serialVersionUID = 3516650461724866075L;
	private AutoForm configuration = new AutoForm();

	public ResourceConfigurationController() {
		super(ResourceWizard.PAGE_NAVIGATION_BASE + "/resourceConfiguration.xhtml");
	}

	public AutoForm getConfiguration() {
		return configuration;
	}
	
	@Override
	public void cleanController() {
		// TODO Auto-generated method stub
		
	}
}