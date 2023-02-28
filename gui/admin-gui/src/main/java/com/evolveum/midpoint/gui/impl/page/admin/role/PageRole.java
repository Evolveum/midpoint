/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/role", matchUrlForSecurity = "/admin/role")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, label = "PageAdminRoles.auth.roleAll.label", description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageAbstractRole<RoleType, FocusDetailsModels<RoleType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

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

        if (canShowWizard(SystemObjectsType.ARCHETYPE_APPLICATION_ROLE)) {
            setShowedByWizard(true);
            getObjectDetailsModels().reset();
            return createRoleWizardFragment(ApplicationRoleWizardPanel.class);
        }

        if (canShowWizard(SystemObjectsType.ARCHETYPE_BUSINESS_ROLE)) {
            setShowedByWizard(true);
            getObjectDetailsModels().reset();
            return createRoleWizardFragment(BusinessRoleWizardPanel.class);
        }

        return super.createDetailsFragment();
    }

    private boolean canShowWizard(SystemObjectsType archetype) {
        return !isEditObject() && WebComponentUtil.hasArchetypeAssignment(
                getObjectDetailsModels().getObjectType(),
                archetype.value());
    }

    private DetailsFragment createRoleWizardFragment(Class<? extends AbstractWizardPanel> clazz) {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRole.this) {
            @Override
            protected void initFragmentLayout() {
                try {
                    Constructor<? extends AbstractWizardPanel> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);
                    AbstractWizardPanel wizard = constructor.newInstance(ID_TEMPLATE, createObjectWizardPanelHelper());
                    add(wizard);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error("Couldn't create panel by constructor for class " + clazz.getSimpleName()
                            + " with parameters type: String, WizardPanelHelper");
                }
            }
        };
    }
}
