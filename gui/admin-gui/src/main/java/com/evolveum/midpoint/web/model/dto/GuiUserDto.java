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

package com.evolveum.midpoint.web.model.dto;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author katuska
 */
public class GuiUserDto extends UserDto implements Selectable {

	private static final long serialVersionUID = -8265669830268114388L;
	private static final Trace TRACE = TraceManager.getTrace(GuiUserDto.class);
	private boolean selected;
	private boolean enabled = false;
	private String password1;
	private String password2;

	public GuiUserDto(UserType object) {
		super(object);
	}

	public GuiUserDto(GuiUserDto user) {
		this.setFamilyName(user.getFamilyName());
		this.setFullName(user.getFullName());
		this.setGivenName(user.getGivenName());
		this.setHonorificPrefix(user.getHonorificPrefix());
		this.setHonorificSuffix(user.getHonorificSuffix());
		this.setName(user.getName());
		this.setOid(user.getOid());
		this.setVersion(user.getVersion());
		this.setEmail(user.getEmail());
		this.getAccount().addAll(user.getAccount());
		this.getAccountRef().addAll(user.getAccountRef());
	}

	public GuiUserDto() {
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getPassword1() {
		return password1;
	}

	public void setPassword1(String password1) {
		this.password1 = password1;

		if (password1 != null) {
			UserType user = (UserType) this.getXmlObject();
			CredentialsType credentials = user.getCredentials();
			if (credentials == null) {
				credentials = new CredentialsType();
				user.setCredentials(credentials);
			}
			CredentialsType.Password password = credentials.getPassword();
			if (password == null) {
				password = new CredentialsType.Password();
				credentials.setPassword(password);
			}

			Document document = DOMUtil.getDocument();
			Element hash = document.createElementNS(SchemaConstants.NS_C, "c:base64");
			hash.setTextContent(Base64.encodeBase64String(password1.getBytes()));
			password.setAny(hash);
		}
	}

	public String getPassword2() {
		return password2;
	}

	public void setPassword2(String password2) {
		this.password2 = password2;
	}

	public boolean isEnabled() {
		UserType user = (UserType) this.getXmlObject();
		ActivationType activation = user.getActivation();
		if (activation != null) {
			enabled = activation.isEnabled();
		}

		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;

		UserType user = (UserType) this.getXmlObject();
		ActivationType activation = user.getActivation();
		if (activation == null) {
			activation = new ActivationType();
			user.setActivation(activation);
		}
		activation.setEnabled(enabled);
	}

	public void setWebAccessEnabled(boolean webAccessEnabled) {
		UserType user = (UserType) this.getXmlObject();
		CredentialsType credentials = user.getCredentials();
		if (credentials == null) {
			credentials = new CredentialsType();
			user.setCredentials(credentials);
		}
		credentials.setAllowedIdmGuiAccess(webAccessEnabled);
	}

	public boolean isWebAccessEnabled() {
		UserType user = (UserType) this.getXmlObject();
		CredentialsType credentials = user.getCredentials();

		if (credentials == null || credentials.isAllowedIdmGuiAccess() == null) {
			return false;
		}

		return credentials.isAllowedIdmGuiAccess();
	}
}
