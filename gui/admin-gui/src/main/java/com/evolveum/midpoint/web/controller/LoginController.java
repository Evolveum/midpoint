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

package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.FacesUtils;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author lazyman
 */
@Controller
@Scope("request")
public class LoginController implements Serializable {

	private static final long serialVersionUID = 4748884787908138803L;
	@Autowired(required = true)
	private transient AuthenticationManager authenticationManager;
	private String userName;
	private String userPassword;
	private String adminName;
	private String adminPassword;
	private SecurityUtils secUtils = new SecurityUtils();

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getUserPassword() {
		return userPassword;
	}
	
	public String getAdminName() {
		return adminName;
	}

	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String loginUser() {
		try {
			Authentication request = new UsernamePasswordAuthenticationToken(this.getUserName(),
					getUserPassword());
			Authentication result = authenticationManager.authenticate(request);
			SecurityContextHolder.getContext().setAuthentication(result);
		} catch (AuthenticationException ex) {
			Object extra = ex.getExtraInformation();
			if (extra instanceof Object[]) {
				FacesUtils.addErrorMessage(FacesUtils.translateKey(ex.getMessage(), (Object[]) extra));
			} else {
				FacesUtils.addErrorMessage(FacesUtils.translateKey(ex.getMessage()));
			}
			return null;
		}
		return "/user-gui/index.xhml?faces-redirect=true";
	}

	public String loginAdmin() {
		try {
			Authentication request = new UsernamePasswordAuthenticationToken(this.getAdminName(),
					getAdminPassword());
			Authentication result = authenticationManager.authenticate(request);
			SecurityContextHolder.getContext().setAuthentication(result);
		} catch (AuthenticationException ex) {
			Object extra = ex.getExtraInformation();
			if (extra instanceof Object[]) {
				FacesUtils.addErrorMessage(FacesUtils.translateKey(ex.getMessage(), (Object[]) extra));
			} else {
				FacesUtils.addErrorMessage(FacesUtils.translateKey(ex.getMessage()));
			}
			return null;
		}
		if (secUtils.getIsAdminLoggedIn()) {
			return "/index.xhml?faces-redirect=true";
		} else {
			FacesUtils.addErrorMessage("You haven't permission to login as administrator.");
			return null;
		}
	}
}
