/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.OutlierSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.RoleAnalysisSessionWizardPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisOutlier", matchUrlForSecurity = "/admin/roleAnalysisOutlier")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                description = "PageRoleAnalysis.auth.roleAnalysisAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_SESSION_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisSession.label",
                description = "PageRoleAnalysis.auth.roleAnalysisSession.description")
})

public class PageRoleAnalysisOutlier extends PageAssignmentHolderDetails<RoleAnalysisOutlierType, AssignmentHolderDetailsModel<RoleAnalysisOutlierType>> {

    private static final String DOT_CLASS = PageRoleAnalysisOutlier.class.getName() + ".";
    private static final String OP_DELETE_CLEANUP = DOT_CLASS + "deleteCleanup";
    private static final String OP_PERFORM_CLUSTERING = DOT_CLASS + "performClustering";
    public static final String PARAM_IS_WIZARD = "isWizard";
    boolean isWizardPanel = false;
    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisOutlierType.class);

    public boolean isWizardPanel() {
        StringValue stringValue = getPageParameters().get(PARAM_IS_WIZARD);
        if (stringValue != null) {
            if ("true".equalsIgnoreCase(stringValue.toString())
                    || "false".equalsIgnoreCase(stringValue.toString())) {
                this.isWizardPanel = getPageParameters().get(PARAM_IS_WIZARD).toBoolean();
            } else {
                getPageParameters().remove(PARAM_IS_WIZARD);
            }
        }
        return isWizardPanel;
    }

    public PageRoleAnalysisOutlier() {
        super();
    }



    @Override
    public void savePerformed(AjaxRequestTarget target) {
        super.savePerformed(target);
    }



    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Class<RoleAnalysisOutlierType> getType() {
        return RoleAnalysisOutlierType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisOutlierType> summaryModel) {
        return new OutlierSummaryPanel(id, summaryModel, null);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("RoleMining.page.cluster.title");
    }

    private boolean canShowWizard() {
        return isWizardPanel();
    }

    protected DetailsFragment createDetailsFragment() {
        if (!isNativeRepo()) {
            return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisOutlier.this) {
                @Override
                protected void initFragmentLayout() {
                    add(new ErrorPanel(ID_TEMPLATE,
                            createStringResource("RoleAnalysis.menu.nonNativeRepositoryWarning")));
                }
            };
        }

        if (canShowWizard()) {
            setShowedByWizard(true);
            getObjectDetailsModels().reset();
            return createRoleWizardFragment(RoleAnalysisSessionWizardPanel.class);
        }

        return super.createDetailsFragment();
    }

    @Override
    protected AssignmentHolderDetailsModel<RoleAnalysisOutlierType> createObjectDetailsModels(PrismObject<RoleAnalysisOutlierType> object) {
        return super.createObjectDetailsModels(object);
    }

    @Override
    protected void onBackPerform(AjaxRequestTarget target) {
        ((PageBase) getPage()).navigateToNext(PageRoleAnalysis.class);
    }

    private DetailsFragment createRoleWizardFragment(Class<? extends AbstractWizardPanel> clazz) {

        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRoleAnalysisOutlier.this) {
            @Override
            protected void initFragmentLayout() {
                try {
                    Constructor<? extends AbstractWizardPanel> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);
                    AbstractWizardPanel wizard = constructor.newInstance(ID_TEMPLATE, createObjectWizardPanelHelper());
                    add(wizard);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                        InvocationTargetException ignored) {

                }
            }
        };

    }
}

