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

import com.evolveum.midpoint.web.bean.AppenderListItem;
import com.evolveum.midpoint.web.util.FacesUtils;

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
	public static final String PARAM_APPENDER_ID = "appenderName";
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
		String appenderName = FacesUtils.getRequestParameter(PARAM_APPENDER_ID);
		if (StringUtils.isEmpty(appenderName)) {
			FacesUtils.addErrorMessage("Appender id not defined.");
			return PAGE_NAVIGATION_LIST;
		}

		for (AppenderListItem item : loggingController.getAppenders()) {
			if (item.getName().equals(appenderName)) {
				this.item = item.cloneItem();
				break;
			}
		}

		if (item == null) {
			FacesUtils.addErrorMessage("Appender configuration not found.");
			return PAGE_NAVIGATION_LIST;
		}
		
		return PAGE_NAVIGATION_EDIT;
	}

	public String backPerformed() {
		clearController();
		return PAGE_NAVIGATION_LIST;
	}

	public String savePerformed() {
		AppenderListItem oldItem = null;
		for (AppenderListItem item : loggingController.getAppenders()) {
			if (item.getName().equals(getItem().getName())) {
				oldItem = item;
				break;
			}
		}
		
		if (oldItem != null) {
			loggingController.getAppenders().remove(oldItem);		
		}
		
		loggingController.getAppenders().add(item);
		
		//TODO: update configuration
		
		clearController();
		return PAGE_NAVIGATION_LIST;
	}
}
