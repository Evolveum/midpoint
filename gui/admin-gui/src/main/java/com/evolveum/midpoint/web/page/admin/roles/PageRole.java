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
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.hibernate.cfg.PropertyData;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.roles.component.MultiplicityPolicyDialog;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author shood
 */
@PageDescriptor(url = "/admin/role", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminRoles.AUTH_ROLE_ALL, label = PageAdminRoles.AUTH_ROLE_ALL_LABEL, description = PageAdminRoles.AUTH_ROLE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageAdminAbstractRole<RoleType>implements ProgressReportingAwarePage {

	public static final String AUTH_ROLE_ALL = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL;
	public static final String AUTH_ROLE_ALL_LABEL = "PageAdminRoles.auth.roleAll.label";
	public static final String AUTH_ROLE_ALL_DESCRIPTION = "PageAdminRoles.auth.roleAll.description";

	private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

	public PageRole() {
		initialize(null);
	}

	public PageRole(PageParameters parameters, PageTemplate previousPage) {
		getPageParameters().overwriteWith(parameters);
		setPreviousPage(previousPage);
		initialize(null);
	}

	@Override
	protected void performCustomInitialization() {
		super.performCustomInitialization();

	}

	protected void initCustomLayout(Form mainForm) {
		super.initCustomLayout(mainForm);
	};

	/**
	 * Removes empty policy constraints from role. It was created when loading
	 * model (not very good model implementation). MID-2366
	 *
	 * TODO improve
	 *
	 * @param prism
	 */
	private void removeEmptyPolicyConstraints(PrismObject<RoleType> prism) {
		RoleType role = prism.asObjectable();
		PolicyConstraintsType pc = role.getPolicyConstraints();
		if (pc == null) {
			return;
		}

		if (pc.getExclusion().isEmpty() && pc.getMinAssignees().isEmpty() && pc.getMaxAssignees().isEmpty()) {
			role.setPolicyConstraints(null);
		}
	}

	@Override
	protected void prepareFocusDeltaForModify(ObjectDelta<RoleType> focusDelta) throws SchemaException {
		super.prepareFocusDeltaForModify(focusDelta);

		ObjectDelta delta = getFocusWrapper().getObjectOld().diff(getFocusWrapper().getObject());

		ContainerDelta<PolicyConstraintsType> policyConstraintsDelta = delta
				.findContainerDelta(new ItemPath(RoleType.F_POLICY_CONSTRAINTS));
		if (policyConstraintsDelta != null) {
			focusDelta.addModification(policyConstraintsDelta);
			return;
		}

		ContainerDelta maxAssignes = delta.findContainerDelta(
				new ItemPath(RoleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES));
		if (maxAssignes != null) {
			focusDelta.addModification(maxAssignes);
		}

		ContainerDelta minAssignes = delta.findContainerDelta(
				new ItemPath(RoleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MIN_ASSIGNEES));
		if (minAssignes != null) {
			focusDelta.addModification(minAssignes);
		}

	}

	@Override
	protected void prepareFocusForAdd(PrismObject<RoleType> focus) throws SchemaException {
		// TODO policyConstraints
		super.prepareFocusForAdd(focus);

		getFocusWrapper().getObjectOld().findOrCreateContainer(RoleType.F_POLICY_CONSTRAINTS);
		ObjectDelta delta = getFocusWrapper().getObjectOld().diff(getFocusWrapper().getObject());

		ContainerDelta<PolicyConstraintsType> policyConstraintsDelta = delta
				.findContainerDelta(new ItemPath(RoleType.F_POLICY_CONSTRAINTS));
		if (policyConstraintsDelta != null) {
			policyConstraintsDelta.applyTo(focus);
			return;
		}

		ContainerDelta maxAssignes = delta.findContainerDelta(
				new ItemPath(RoleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MAX_ASSIGNEES));
		if (maxAssignes != null) {
			maxAssignes.applyTo(focus);
		}

		ContainerDelta minAssignes = delta.findContainerDelta(
				new ItemPath(RoleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_MIN_ASSIGNEES));
		if (minAssignes != null) {
			minAssignes.applyTo(focus);
		}

	}

	@Override
	protected void setSpecificResponsePage() {
		if (getPreviousPage() != null) {
			goBack(PageDashboard.class); // parameter is not used
		} else {
			setResponsePage(new PageRoles(false, ""));
		}
	}

	@Override
	protected RoleType createNewFocus() {
		return new RoleType();
	}

	@Override
	protected void reviveCustomModels() throws SchemaException {
		// TODO revivie max min assignments?
	}

	@Override
	protected Class<RoleType> getCompileTimeClass() {
		return RoleType.class;
	}

	@Override
	protected Class getRestartResponsePage() {
		return PageRoles.class;
	}

	@Override
	protected void initTabs(List<ITab> tabs) {
		super.initTabs(tabs);

		tabs.add(new AbstractTab(createStringResource("AbstractRoleType.policyConstraints")) {
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new RolePolicyPanel(panelId, getFocusWrapper().getObject());
			}
		});

		
			tabs.add(new AbstractTab(createStringResource("pageRole.members")) {
				@Override
				public WebMarkupContainer getPanel(String panelId) {
					return new RoleMemberPanel<UserType>(panelId, getFocusWrapper().getObject().getOid(),
							PageRole.this);
				}
			});
		
	}
}
