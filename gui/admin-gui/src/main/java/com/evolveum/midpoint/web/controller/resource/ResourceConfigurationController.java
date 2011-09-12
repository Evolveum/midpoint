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
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.processor.ItemDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.web.bean.ResourceConfigFormBean;
import com.evolveum.midpoint.web.controller.util.WizardPage;
import com.evolveum.midpoint.web.jsf.form.FormObject;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SchemaFormParser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Configuration;

@Controller("resourceConfiguration")
@Scope("session")
public class ResourceConfigurationController extends WizardPage {

	private static final long serialVersionUID = 3516650461724866075L;
	private List<ResourceConfigFormBean> configurationList;

	public ResourceConfigurationController() {
		super(ResourceWizard.PAGE_NAVIGATION_BASE + "/resourceConfiguration.xhtml");
	}

	public List<ResourceConfigFormBean> getConfigurationList() {
		if (configurationList == null) {
			configurationList = new ArrayList<ResourceConfigFormBean>();
		}
		return configurationList;
	}

	public List<FormObject> getFormObjects() {
		List<FormObject> list = new ArrayList<FormObject>();
		for (ResourceConfigFormBean bean : getConfigurationList()) {
			list.add(bean.getBean());
		}

		return list;
	}

	public int getSize() {
		return getConfigurationList().size();
	}

	public void init(ConnectorDto connector, Configuration configuration) {
		if (connector == null) {
			FacesUtils.addErrorMessage("Connector object must not be null.");
			return;
		}

		SchemaFormParser parser = new SchemaFormParser();
		List<ResourceObjectDefinition> definitions = parser.parseSchemaForConnector(connector, configuration);
		if (definitions == null) {
			return;
		}

		int id = 0;
		for (ResourceObjectDefinition objectDefinition : definitions) {
			FormObject formObject = new FormObject();
			// TODO: prepare form object here like in UserDetailsController line
			// 547 to 556 (as an example), prepare also values in there (create
			// FormAttributes based on ResourceObjectAttributeDefinition and
			// values).
			for (ItemDefinition attributeDefinition : objectDefinition.getDefinitions()) {
				if (!(attributeDefinition instanceof ResourceObjectAttributeDefinition)) {
					continue;
				}

				ResourceObjectAttributeDefinition attribute = (ResourceObjectAttributeDefinition) attributeDefinition;
			}
			getConfigurationList().add(
					new ResourceConfigFormBean(id, connector, objectDefinition.getTypeName(), formObject));
			id++;
		}
	}

	@Override
	public void cleanController() {
		getConfigurationList().clear();
	}
}