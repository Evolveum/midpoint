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
package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.crypto.AESProtector;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;

/**
 * REFACTOR THIS CLASS!!! In cooperation with schema form parser refactor and
 * Account auto from, resource schema from refactoring.
 * 
 * @author lazyman
 * 
 */
public class ResourceCapability implements Serializable {

	private static final long serialVersionUID = -3013453566132560637L;
	// flags for available capabilities
	private boolean credentials;
	private boolean activation = true;
	// activation
	private boolean enabled = true;
	private boolean activationUsed = false;
	private Date activeFrom;
	private Date activeTo;
	// credentials
	private String password1;
	private String password2;
	private boolean allowedIdmAdminGuiAccess;

	// @Autowired(required = true)
	// private transient Protector protector;

	@SuppressWarnings("rawtypes")
	public void setAccount(AccountShadowDto account, CapabilitiesType capabilities) {
		for (Object capability : capabilities.getAny()) {
			if (!(capability instanceof JAXBElement)) {
				continue;
			}
			JAXBElement element = (JAXBElement) capability;
			if (element.getDeclaredType().equals(CredentialsCapabilityType.class)) {
				credentials = true;
			}
			if (element.getDeclaredType().equals(ActivationCapabilityType.class)) {
				activation = true;
			}
		}
		ActivationType activation = account.getActivation();
		if (activation != null) {
			enabled = activation.isEnabled() == null ? true : activation.isEnabled();
			if (activation.getValidFrom() != null) {
				XMLGregorianCalendar calendar = activation.getValidFrom();
				activeFrom = calendar.toGregorianCalendar().getTime();
			}
			if (activation.getValidTo() != null) {
				XMLGregorianCalendar calendar = activation.getValidTo();
				activeTo = calendar.toGregorianCalendar().getTime();
			}
		}
		CredentialsType credentials = account.getCredentials();
		if (credentials != null) {
			allowedIdmAdminGuiAccess = credentials.isAllowedIdmAdminGuiAccess() == null ? false : credentials
					.isAllowedIdmAdminGuiAccess();
		}
	}

	public CredentialsType getCredentialsType() {
		if (StringUtils.isEmpty(password1)) {
			return null;
		}

		// TODO: refactor this: the same mathod is in the GuiUserDto class ->
		// encryptCredentials
		CredentialsType credentials = new CredentialsType();
		credentials.setAllowedIdmAdminGuiAccess(allowedIdmAdminGuiAccess);

		PasswordType password = new PasswordType();

		try {
			Protector protector = new AESProtector();
			password.setProtectedString(protector.encryptString(password1));
			credentials.setPassword(password);
			// protectedString.setClearValue(password1);
		} catch (Exception ex) {
			// LoggingUtils.logException(LOGGER, "Couldn't encrypt credentials",
			// ex);
			throw new SystemException(ex.getMessage(), ex);
		}
		return credentials;
	}

	public ActivationType getActivationType() {
		// TODO: now wse do not support activation date...only simple
		// enable/disable account is supported
		// if (enabled && !activationUsed) {
		// return null;
		// }

		ActivationType activation = new ActivationType();
		activation.setEnabled(enabled);
		// if (!enabled) {
		// activation.setEnabled(false);
		// }

		if (activationUsed) {
			try {
				Calendar calendar = GregorianCalendar.getInstance();
				calendar.setTimeInMillis(getActiveFrom().getTime());
				activation.setValidFrom(DatatypeFactory.newInstance().newXMLGregorianCalendar(
						(GregorianCalendar) calendar));

				calendar = GregorianCalendar.getInstance();
				calendar.setTimeInMillis(getActiveTo().getTime());
				activation.setValidTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(
						(GregorianCalendar) calendar));
			} catch (DatatypeConfigurationException ex) {
				throw new SystemException(ex.getMessage(), ex);
			}
		}

		return activation;
	}

	public boolean isCredentials() {
		return credentials;
	}

	public void setCredentials(boolean credentials) {
		this.credentials = credentials;
	}

	public boolean isActivation() {
		return activation;
	}

	public void setActivation(boolean activation) {
		this.activation = activation;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isActivationUsed() {
		return activationUsed;
	}

	public void setActivationUsed(boolean activationUsed) {
		this.activationUsed = activationUsed;
	}

	public Date getActiveFrom() {
		return activeFrom;
	}

	public void setActiveFrom(Date activeFrom) {
		this.activeFrom = activeFrom;
	}

	public Date getActiveTo() {
		return activeTo;
	}

	public void setActiveTo(Date activeTo) {
		this.activeTo = activeTo;
	}

	public String getPassword1() {
		return password1;
	}

	public void setPassword1(String password1) {
		this.password1 = password1;
	}

	public String getPassword2() {
		return password2;
	}

	public void setPassword2(String password2) {
		this.password2 = password2;
	}

	public boolean isAllowedIdmAdminGuiAccess() {
		return allowedIdmAdminGuiAccess;
	}

	public void setAllowedIdmAdminGuiAccess(boolean allowedIdmAdminGuiAccess) {
		this.allowedIdmAdminGuiAccess = allowedIdmAdminGuiAccess;
	}
}
