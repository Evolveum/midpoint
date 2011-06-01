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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.bean.LoggerListItem;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("loggerEdit")
@Scope("session")
public class LoggerEditController implements Serializable {

	public static final String PAGE_NAVIGATION_LIST = "/config/logging?faces-redirect=true";
	public static final String PAGE_NAVIGATION_EDIT = "/config/loggerEdit?faces-redirect=true";
	public static final String PARAM_LOGGER_ID = "loggerId";
	private static final long serialVersionUID = 4491151780064705480L;
	@Autowired(required = true)
	private LoggingController loggingController;
	private LoggerListItem item;
	private String packageName;
	private List<String> selectedPackages;

	public List<String> getSelectedPackages() {
		return selectedPackages;
	}

	public void setSelectedPackages(List<String> selectedPackages) {
		this.selectedPackages = selectedPackages;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public LoggerListItem getItem() {
		if (item == null) {
			int id = 0;
			for (LoggerListItem item : loggingController.getLoggers()) {
				if (item.getId() > id) {
					id = item.getId() + 1;
				}
			}
			item = new LoggerListItem(id);
		}
		return item;
	}

	public void setItem(LoggerListItem item) {
		this.item = item;
	}

	private void clearController() {
		item = null;
		packageName = null;
		selectedPackages = null;
	}

	public List<SelectItem> getPackages() {
		List<SelectItem> packages = new ArrayList<SelectItem>();
		for (String packageName : getItem().getPackages()) {
			packages.add(new SelectItem(packageName));
		}

		Collections.sort(packages, new SelectItemComparator());

		return packages;
	}

	public void addPackage() {
		item.getPackages().add(packageName);
		packageName = null;
	}

	public void deletePackages() {
		if (selectedPackages == null) {
			return;
		}

		item.getPackages().removeAll(selectedPackages);

		selectedPackages = null;
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
			return PAGE_NAVIGATION_LIST;
		}

		int loggerId = Integer.parseInt(argument);
		for (LoggerListItem item : loggingController.getLoggers()) {
			if (item.getId() == loggerId) {
				this.item = item.cloneItem();
			}
		}

		if (item == null) {
			FacesUtils.addErrorMessage("Logger configuration not found.");
			return PAGE_NAVIGATION_LIST;
		}

		return PAGE_NAVIGATION_EDIT;
	}

	public String backPerformed() {
		clearController();
		return PAGE_NAVIGATION_LIST;
	}

	public String savePerformed() {
		LoggerListItem oldItem = null;
		for (LoggerListItem item : loggingController.getLoggers()) {
			if (item.getId() == this.item.getId()) {
				oldItem = item;
				break;
			}
		}
		
		if (oldItem != null) {
			loggingController.getLoggers().remove(oldItem);		
		}
		
		loggingController.getLoggers().add(item);
		
		//TODO: update configuration
		
		clearController();
		return PAGE_NAVIGATION_LIST;
	}
}
