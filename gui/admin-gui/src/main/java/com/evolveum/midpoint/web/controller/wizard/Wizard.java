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
package com.evolveum.midpoint.web.controller.wizard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class Wizard implements Serializable {

	private static final long serialVersionUID = -1906021840798686508L;
	private String cancelPage;
	private List<WizardPage> pages;
	private int index = 0;

	public Wizard(String cancelPage) {
		if (StringUtils.isEmpty(cancelPage)) {
			throw new IllegalArgumentException("Cancel page must not be null.");
		}

		this.cancelPage = cancelPage;
	}

	public String getActualPageUrl() {
		return getPages().get(index).getPageUrl();
	}

	public String getActualCancelPage() {
		String cancelPage = getActualWizardPage().getCancelPage();
		if (StringUtils.isEmpty(cancelPage)) {
			return this.cancelPage;
		}
		return cancelPage;
	}

	public String getActualFinishPage() {
		return getActualWizardPage().getFinishPage();
	}

	public boolean isActualShowCancel() {
		return true;
	}

	public boolean isActualShowFinish() {
		return getActualWizardPage().isShowFinish();
	}

	public boolean isActualShowBack() {
		if (index == 0) {
			return false;
		}
		return true;
	}

	public boolean isActualShowNext() {
		if (index + 1 >= getPages().size()) {
			return false;
		}
		return true;
	}

	public WizardPage getActualWizardPage() {
		return getPages().get(index);
	}

	public List<WizardPage> getPages() {
		if (pages == null) {
			pages = new ArrayList<WizardPage>();
		}
		return pages;
	}

	public String next() {
		if (index + 1 <= getPages().size()) {
			index++;
		}
		return getActualWizardPage().getPageUrl();
	}

	public String back() {
		if (index >= 1) {
			index--;
		}
		return getActualWizardPage().getPageUrl();
	}

	public String finish() {
		String returnPage = getActualWizardPage().finish();
		if (!StringUtils.isEmpty(returnPage)) {
			for (WizardPage page : getPages()) {
				page.cleanController();
			}
		}

		return returnPage;
	}

	public String cancel() {
		for (WizardPage page : getPages()) {
			page.cleanController();
		}

		return getActualCancelPage();
	}
}
