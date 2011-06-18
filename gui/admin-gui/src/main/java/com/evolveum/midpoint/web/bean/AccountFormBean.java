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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.jsf.form.FormObject;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;

public class AccountFormBean implements Serializable {

	private static final long serialVersionUID = 7023783107817686439L;
	private int id;
	private AccountShadowDto account;
	private QName defaultAccountType;
	private FormObject bean;
	private boolean expanded = true;
	private boolean isNew;
	private boolean enabled;

	public AccountFormBean(int id, AccountShadowDto account, QName defaultAccountType, FormObject bean,
			boolean isNew) {
		this.id = id;
		this.account = account;
		this.defaultAccountType = defaultAccountType;
		this.bean = bean;
		this.isNew = isNew;
	}

	public QName getDefaultAccountType() {
		return defaultAccountType;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isNew() {
		return isNew;
	}

	public int getId() {
		return id;
	}

	public FormObject getBean() {
		return bean;
	}

	public void setBean(FormObject bean) {
		this.bean = bean;
	}

	public String getName() {
		return bean.getDisplayName();
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	public AccountShadowDto getAccount() {
		return account;
	}
}
