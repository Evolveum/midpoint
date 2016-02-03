/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.users;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.dialog.ChooseResourceDefinitionDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.users.component.OrgSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminUsers.AUTH_ORG_ALL, label = PageAdminUsers.AUTH_ORG_ALL_LABEL, description = PageAdminUsers.AUTH_ORG_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL, label = "PageOrgUnit.auth.orgUnit.label", description = "PageOrgUnit.auth.orgUnit.description") })
public class PageOrgUnit extends PageAdminAbstractRole<OrgType> implements ProgressReportingAwarePage {

	private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);
	
	private static final String MODAL_ID_SELECT_RESOURCE_DEFINITION = "selectResourceDefinition";
	
	public PageOrgUnit() {
		initialize(null);
	}

	// todo improve [erik]
	public PageOrgUnit(final PrismObject<OrgType> unitToEdit) {
		initialize(unitToEdit);
	}

	public PageOrgUnit(PageParameters parameters, PageTemplate previousPage) {
		getPageParameters().overwriteWith(parameters);
		setPreviousPage(previousPage);
		initialize(null);
	}

	
	class ResourceChooseModel implements Serializable {
		boolean unix;
		boolean ldap;
		
		public boolean isUnix() {
			return unix;
		}
		
		public boolean isLdap() {
			return ldap;
		}
		
		public void setLdap(boolean ldap) {
			this.ldap = ldap;
		}
		
		public void setUnix(boolean unix) {
			this.unix = unix;
		}
	}
	
	private void showModalWindow(String id, AjaxRequestTarget target) {
		ModalWindow window = (ModalWindow) get(id);
		window.show(target);
		target.add(getFeedbackPanel());
	}

	@Override
	protected void initCustomLayout(Form mainForm) {

		IModel<ResourceChooseModel> model = new Model(new ResourceChooseModel());
		ChooseResourceDefinitionDialog chooseResourceDialog = new ChooseResourceDefinitionDialog(MODAL_ID_SELECT_RESOURCE_DEFINITION,
				createStringResource("pageAdminFocus.title.selectResourceDefinition"),
				 (Model) model) {

			@Override
			public void yesPerformed(AjaxRequestTarget target, IModel model) {
				close(target);
				generateResourceDefinition(target,((ResourceChooseModel)model.getObject()).isUnix(), ((ResourceChooseModel) model.getObject()).isLdap());
			}
			
		};
		add(chooseResourceDialog);

		
		AjaxButton generateResourceDefinition = new AjaxButton("generateResourceDefinition", createStringResource("pageAdminFocus.button.generateResourceDefinition")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				showModalWindow(MODAL_ID_SELECT_RESOURCE_DEFINITION, target);
			}
			
		};
		generateResourceDefinition.add(new VisibleEnableBehaviour(){
			
			@Override
			public boolean isVisible() {
				PrismObject focus = getFocusWrapper().getObject();
				
				if (focus.asObjectable() instanceof UserType || focus.asObjectable() instanceof RoleType){
					return false;
				}
				
//				OrgType orgType = (OrgType) focus.asObjectable();
				PrismProperty propertyOrgType = focus.findProperty(OrgType.F_ORG_TYPE);
				if (propertyOrgType == null || propertyOrgType.isEmpty()){
					return false;
				}
				
				Collection<String> orgTypes = propertyOrgType.getRealValues();
				if (orgTypes.contains("resource")){
					return true;
				}
				
				return false;
			}
			
		});
		
		mainForm.add(generateResourceDefinition);

	}

	
private void generateResourceDefinition(AjaxRequestTarget target, boolean unix, boolean ldap) {
		
		
		if (unix){
			String CONNECTOR_PROPERTIES = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.connid.bundles.unix/org.connid.bundles.unix.UnixConnector";
			File f = new File(getMidpointConfiguration().getMidpointHome() + "/templates/unix-resource.xml");
			
			createResourceDefinition(target, f, "Unix", new QName(CONNECTOR_PROPERTIES, "hostname"));
		}
		
		if (ldap){
			String CONNECTOR_PROPERTIES = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-ldap/com.evolveum.polygon.connector.ldap.LdapConnector";
			
			File f = new File(getMidpointConfiguration().getMidpointHome() + "/templates/ldap-resource.xml");
			createResourceDefinition(target, f, "LDAP", new QName(CONNECTOR_PROPERTIES, "host"));
		}
		
		
		
	}


private void createResourceDefinition(AjaxRequestTarget target, File tempalteFile, String resourceType, QName connectorProperty){
	
	
	try {
		String CONNECTOR_NS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-3";
		String opStExtensionNs = "http://midpoint.evolveum.com/xml/ns/custom/openstack-1";
		PrismObject<ResourceType> resource =  getPrismContext().parseObject(tempalteFile);
		
		PrismProperty<String> hostName = resource.findOrCreateProperty(new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, new QName(CONNECTOR_NS, "configurationProperties"), connectorProperty));
		PrismProperty<String> pp = getFocusWrapper().getObject().findProperty(new ItemPath(FocusType.F_EXTENSION, new QName(opStExtensionNs, "host")));
		if (pp == null){
			pp = getFocusWrapper().getObject().findOrCreateProperty(new ItemPath(FocusType.F_EXTENSION, new QName(opStExtensionNs, "host")));
		}
		hostName.setRealValue(pp.getAnyRealValue());
		
		String resourceName = "Resource " + resourceType + ": " + getFocusWrapper().getObject().getName().getOrig();
		PrismProperty name = resource.findOrCreateProperty(ResourceType.F_NAME);
		name.setRealValue(new PolyString(resourceName));
		
		PrismReference parentOrgRef = resource.findOrCreateReference(ResourceType.F_PARENT_ORG_REF);
		ObjectReferenceType orgRef = ObjectTypeUtil.createObjectRef(getFocusWrapper().getObject());
		parentOrgRef.add(orgRef.asReferenceValue());
		
		ObjectDelta addReosurceDelta = ObjectDelta.createAddDelta(resource);
		
		Task task = createSimpleTask("generate resource definition");
		OperationResult result = new OperationResult("Generate resource configuration for: " + resourceName);
		
		getModelService().executeChanges(WebMiscUtil.createDeltaCollection(addReosurceDelta), null, task, result);
		result.computeStatus();
		showResult(result);
		target.add(getFeedbackPanel());
		
	} catch (SchemaException | IOException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
		LOGGER.error("ERROR generating resource definition: " + e.getMessage(), e);
		throw new IllegalArgumentException(e);
	}
}
	
	


	private IModel<String> createStyleClassModel(final IModel<PropertyWrapper> wrapper) {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				PropertyWrapper property = wrapper.getObject();
				return property.isVisible() ? "visible" : null;
			}
		};
	}


	
	protected void setSpecificResponsePage() {
		if (getSessionStorage().getPreviousPage() != null){
			goBack(getSessionStorage().getPreviousPage());
		} else {
			goBack(PageOrgTree.class);
		}
	}


	protected void reviveCustomModels() throws SchemaException {

	}

	@Override
	protected OrgType createNewFocus() {
		return new OrgType();
	}

	

	@Override
	protected Class getCompileTimeClass() {
		return OrgType.class;
	}

	@Override
	protected Class getRestartResponsePage() {
		return PageOrgTree.class;
	}

	@Override
	protected void initTabs(List<ITab> tabs) {
		super.initTabs(tabs);
		
	}
	
	@Override
	protected FocusSummaryPanel<OrgType> createSummaryPanel() {
		
    	return new OrgSummaryPanel(ID_SUMMARY_PANEL, getFocusModel());
    	
    }

}
