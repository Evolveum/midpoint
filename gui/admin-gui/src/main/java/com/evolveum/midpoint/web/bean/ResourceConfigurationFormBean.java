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
import com.evolveum.midpoint.web.model.dto.ResourceDto;

/**
 * 
 * @author mserbak
 */

public class ResourceConfigurationFormBean implements Serializable {

	private static final long serialVersionUID = -1098405621403612160L;
	private int id;
	private ResourceDto resourceConfig;
	private QName defaultresourceConfigType;
	private FormObject bean;
	private boolean expanded = true;
	private boolean isNew;
	private boolean enabled;

	public ResourceConfigurationFormBean(int id, ResourceDto resourceConfig, QName defaultResourceConfigType, FormObject bean,
			boolean isNew) {
		this.id = id;
		this.resourceConfig = resourceConfig;
		this.defaultresourceConfigType = defaultResourceConfigType;
		this.bean = bean;
		this.isNew = isNew;
	}

	public QName getDefaultConnectorType() {
		return defaultresourceConfigType;
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

	public ResourceDto getConnector() {
		return resourceConfig;
	}
}
