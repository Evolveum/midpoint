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
package com.evolveum.midpoint.web.controller.config;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.bean.AppenderListItem;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("appenderEdit")
@Scope("session")
public class AppenderEditController implements Serializable {

	public static final String PAGE_NAVIGATION_LIST = "/config/logging?faces-redirect=true";
	public static final String PAGE_NAVIGATION_EDIT = "/config/appenderEdit?faces-redirect=true";
	public static final String PARAM_LOGGER_ID = "appenderId";
	private static final long serialVersionUID = 636982713825573383L;
	@Autowired(required = true)
	private LoggingController loggingController;
	private AppenderListItem item;

	public AppenderListItem getItem() {
		if (item == null) {
			item = new AppenderListItem();
		}
		return item;
	}

	private void clearController() {
		item = null;
	}

	public String addAppender() {
		clearController();
		return PAGE_NAVIGATION_EDIT;
	}

	public String editAppender() {
		clearController();
		// TODO: finish
		return PAGE_NAVIGATION_EDIT;
	}

	public String backPerformed() {
		clearController();
		return PAGE_NAVIGATION_LIST;
	}

	public String savePerformed() {

		clearController();
		return PAGE_NAVIGATION_LIST;
	}
}
