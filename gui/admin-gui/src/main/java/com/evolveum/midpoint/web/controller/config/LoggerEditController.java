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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.bean.LoggerListItem;
import com.evolveum.midpoint.web.util.FacesUtils;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("loggerEdit")
@Scope("session")
public class LoggerEditController implements Serializable {

	public static final String PAGE_NAVIGATION_EDIT = "/config/loggerEdit?faces-redirect=true";
	public static final String PARAM_LOGGER_ID = "loggerId";
	private static final long serialVersionUID = 4491151780064705480L;
	@Autowired(required = true)
	private LoggingController loggingController;
	private LoggerListItem item;

	public LoggerListItem getItem() {
		if (item == null) {
			int id = 0;
			for (LoggerListItem item : loggingController.getLoggers()) {
				if (item.getId() >= id) {
					id = item.getId() + 1;
				}
			}
			item = new LoggerListItem(id);
		}
		return item;
	}

	private void clearController() {
		item = null;
	}

	public String addLogger() {
		clearController();

		return PAGE_NAVIGATION_EDIT;
	}

	public String editLogger() {
		clearController();
		String argument = FacesUtils.getRequestParameter(PARAM_LOGGER_ID);
		if (StringUtils.isEmpty(argument) || !argument.matches("[0-9]*")) {
			FacesUtils.addErrorMessage("Logger id not defined.");
			return LoggingController.PAGE_NAVIGATION;
		}

		int loggerId = Integer.parseInt(argument);
		for (LoggerListItem item : loggingController.getLoggers()) {
			if (item.getId() == loggerId) {
				this.item = item.cloneItem();
				break;
			}
		}

		if (item == null) {
			FacesUtils.addErrorMessage("Logger configuration not found.");
			return LoggingController.PAGE_NAVIGATION;
		}

		return PAGE_NAVIGATION_EDIT;
	}

	public String backPerformed() {
		clearController();
		return LoggingController.PAGE_NAVIGATION;
	}

	public String savePerformed() {
		LoggerListItem oldItem = null;
		for (LoggerListItem item : loggingController.getLoggers()) {
			if (item.getId() == getItem().getId()) {
				oldItem = item;
				break;
			}
		}

		if (oldItem != null) {
			loggingController.getLoggers().remove(oldItem);
		}

		loggingController.getLoggers().add(item);
		loggingController.saveConfiguration();

		clearController();
		return LoggingController.PAGE_NAVIGATION;
	}
}
