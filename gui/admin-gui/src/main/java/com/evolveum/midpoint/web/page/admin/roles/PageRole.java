/*
 * Copyright (c) 2010-2017 Evolveum
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

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.RoleMainPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.page.admin.PageAdminAbstractRole;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author shood
 * @author semancik
 */
@PageDescriptor(url = "/admin/role", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminRoles.AUTH_ROLE_ALL, label = PageAdminRoles.AUTH_ROLE_ALL_LABEL, description = PageAdminRoles.AUTH_ROLE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageAdminAbstractRole<RoleType> implements ProgressReportingAwarePage {
	private static final long serialVersionUID = 1L;

	public static final String AUTH_ROLE_ALL = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL;
	public static final String AUTH_ROLE_ALL_LABEL = "PageAdminRoles.auth.roleAll.label";
	public static final String AUTH_ROLE_ALL_DESCRIPTION = "PageAdminRoles.auth.roleAll.description";

	private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

	public PageRole() {
		initialize(null);
	}

	public PageRole(PrismObject<RoleType> roleToEdit) {
		initialize(roleToEdit);
	}

	public PageRole(PageParameters parameters) {
		getPageParameters().overwriteWith(parameters);
		initialize(null);
	}


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
	protected void prepareObjectDeltaForModify(ObjectDelta<RoleType> focusDelta) throws SchemaException {
		super.prepareObjectDeltaForModify(focusDelta);

		ObjectDelta<RoleType> delta = getObjectWrapper().getObjectOld().diff(getObjectWrapper().getObject());

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
	protected void prepareObjectForAdd(PrismObject<RoleType> focus) throws SchemaException {
		// TODO policyConstraints
		super.prepareObjectForAdd(focus);

		getObjectWrapper().getObjectOld().findOrCreateContainer(RoleType.F_POLICY_CONSTRAINTS);
		ObjectDelta<RoleType> delta = getObjectWrapper().getObjectOld().diff(getObjectWrapper().getObject());

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
	protected RoleType createNewObject() {
		return new RoleType();
	}

	@Override
    public Class<RoleType> getCompileTimeClass() {
		return RoleType.class;
	}

	@Override
	protected Class getRestartResponsePage() {
		return PageRoles.class;
	}

	@Override
	protected FocusSummaryPanel<RoleType> createSummaryPanel() {
    	return new RoleSummaryPanel(ID_SUMMARY_PANEL, getObjectModel(), this);
    }

	@Override
	protected AbstractObjectMainPanel<RoleType> createMainPanel(String id) {
		return new RoleMainPanel(id, getObjectModel(), getProjectionModel(), getInducementsModel(), this);
	}
}
