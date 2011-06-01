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

import com.evolveum.midpoint.web.controller.wizard.WizardPage;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("resourceSynchronization")
@Scope("session")
public class ResourceSynchronizationController extends WizardPage {

	private static final long serialVersionUID = 8679302869048479592L;
	private boolean enabled;
	private int poolingInterval;
	private String correlation;
	private String confirmation;

	public ResourceSynchronizationController() {
		super(ResourceWizard.PAGE_NAVIGATION_BASE + "/synchronization.xhtml");
	}

	@Override
	public String getFinishPage() {
		return "/resource/index";
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPoolingInterval() {
		return poolingInterval;
	}

	public void setPoolingInterval(int poolingInterval) {
		this.poolingInterval = poolingInterval;
	}

	public String getCorrelation() {
		return correlation;
	}

	public void setCorrelation(String correlation) {
		this.correlation = correlation;
	}

	public String getConfirmation() {
		return confirmation;
	}

	public void setConfirmation(String confirmation) {
		this.confirmation = confirmation;
	}

	@Override
	public void cleanController() {
		// TODO Auto-generated method stub
	}

	@Override
	public String finish() {
		// TODO: stuff...

		return getFinishPage();
	}
}
