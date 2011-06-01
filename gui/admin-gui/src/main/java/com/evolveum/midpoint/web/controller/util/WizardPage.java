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
package com.evolveum.midpoint.web.controller.util;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class WizardPage implements Serializable {

	private static final long serialVersionUID = 6794156473197099300L;
	private String pageUrl;

	public WizardPage(String pageUrl) {
		if (pageUrl == null || pageUrl.isEmpty()) {
			throw new IllegalArgumentException("Actual page cant' be null or empty");
		}
		this.pageUrl = pageUrl;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public boolean isShowCancel() {
		return !StringUtils.isEmpty(getCancelPage());
	}

	public boolean isShowFinish() {
		return !StringUtils.isEmpty(getFinishPage());
	}

	public String getCancelPage() {
		return null;
	}

	public String getFinishPage() {
		return null;
	}

	public String finish() {
		return getFinishPage();
	}

	public String cancel() {
		return getCancelPage();
	}

	public abstract void cleanController();
}
