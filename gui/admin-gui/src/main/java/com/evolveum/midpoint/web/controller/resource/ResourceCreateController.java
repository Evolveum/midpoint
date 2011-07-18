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
package com.evolveum.midpoint.web.controller.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.WizardPage;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("resourceCreate")
@Scope("session")
public class ResourceCreateController extends WizardPage {

	private static final long serialVersionUID = 8679302869048479599L;
	@Autowired(required = true)
	private ObjectTypeCatalog catalog;
	private Collection<ConnectorDto> connectors;
	private String name;
	private String connectorType;
	private String connectorVersion;

	public ResourceCreateController() {
		super(ResourceWizard.PAGE_NAVIGATION_BASE + "/resourceCreate.xhtml");
	}

	public String getName() {
		return name;
	}

	private Collection<ConnectorDto> getConnectors() {
		if (connectors != null) {
			return connectors;
		}

		ObjectManager<ConnectorDto> manager = ControllerUtil.getConnectorManager(catalog);
		connectors = manager.list();

		return connectors;
	}

	public List<String> getTypes() {
		List<String> types = new ArrayList<String>();
		for (ConnectorDto connector : getConnectors()) {
			if (types.contains(connector.getConnectorType())) {
				continue;
			}
			types.add(connector.getConnectorType());
		}
		return types;
	}

	public List<String> getVersions() {
		List<String> versions = new ArrayList<String>();
		if (StringUtils.isEmpty(getConnectorType())) {
			return versions;
		}
		for (ConnectorDto connector : getConnectors()) {
			if (!getConnectorType().equals(connector.getConnectorType())) {
				continue;
			}
			versions.add(connector.getConnectorVersion());
		}
		return versions;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getConnectorType() {
		return connectorType;
	}

	public String getConnectorVersion() {
		return connectorVersion;
	}

	public void setConnectorType(String connectorType) {
		this.connectorType = connectorType;
	}

	public void setConnectorVersion(String connectorVersion) {
		this.connectorVersion = connectorVersion;
	}

	@Override
	public void cleanController() {
		name = null;
		connectorType = null;
		connectorVersion = null;
		connectors = null;
	}
}
