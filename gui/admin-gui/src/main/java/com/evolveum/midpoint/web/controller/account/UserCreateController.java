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
 * Portions Copyrighted 2010 Forgerock
 */
package com.evolveum.midpoint.web.controller.account;

import java.io.Serializable;

import javax.faces.event.ActionEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 */
@Controller("userCreate")
@Scope("session")
public class UserCreateController implements Serializable {

	private static final long serialVersionUID = -405498021481348879L;
	private static final Trace TRACE = TraceManager.getTrace(UserCreateController.class);

	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private transient UserListController userListController;
	private GuiUserDto user;

	public UserCreateController() {
		reinit();
	}

	public GuiUserDto getUser() {
		return user;
	}

	public String create() {
		String oid = null;
		try {
			UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);
			oid = userManager.add(user);
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Couldn't create user", ex);
			FacesUtils.addErrorMessage("Failed to create user:" + ex.getMessage());

			return null;
		}
		if (oid == null) {
			FacesUtils.addErrorMessage("Failed to create user");
			TRACE.debug("Failed to create user {}, oid returned null.", user.getName());
			return null;
		}

		reinit();

		TRACE.info("Created user with oid {}", oid);
		FacesUtils.addSuccessMessage("User created successfully");

		userListController.fillTableList();

		return UserListController.PAGE_NAVIGATION;
	}

	public void cancel(ActionEvent evt) {
		reinit();
	}

	private void reinit() {
		UserManager manager = ControllerUtil.getUserManager(objectTypeCatalog);
		user = (GuiUserDto) manager.create();
		user.setXmlObject(new UserType());
		user.setVersion("1.0");
		user.setEnabled(true);
	}
}
