/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.ApplicationRoleWizardPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/role", matchUrlForSecurity = "/admin/role")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, label = "PageAdminRoles.auth.roleAll.label", description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageFocusDetails<RoleType, FocusDetailsModels<RoleType>> {

    public PageRole() {
        super();
    }

    public PageRole(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageRole(PrismObject<RoleType> role) {
        super(role);
    }

    @Override
    public Class<RoleType> getType() {
        return RoleType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleType> summaryModel) {
        return new RoleSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    protected DetailsFragment createDetailsFragment() {

        if (!isEditObject() && WebComponentUtil.hasArchetypeAssignment(
                getObjectDetailsModels().getObjectType(),
                SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value())) {
            setUseWizardForCreating();
            return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRole.this) {

                @Override
                protected void initFragmentLayout() {
                    add(new ApplicationRoleWizardPanel(ID_TEMPLATE, createRoleWizardPanelHelper()) {

                        @Override
                        protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                            OperationResult result = new OperationResult(OPERATION_SAVE);
                            saveOrPreviewPerformed(target, result, false);
                            return result;
                        }
                    });
                }
            };
        }
        return super.createDetailsFragment();
    }

    private WizardPanelHelper<RoleType, FocusDetailsModels<RoleType>> createRoleWizardPanelHelper() {
        return new WizardPanelHelper<>(getObjectDetailsModels()) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                navigateToNext(PageRoles.class);
            }

            @Override
            public IModel<PrismContainerValueWrapper<RoleType>> getValueModel() {
                return new ContainerValueWrapperFromObjectWrapperModel<>(
                        getDetailsModel().getObjectWrapperModel(), ItemPath.EMPTY_PATH);
            }
        };
    }
}
