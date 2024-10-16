/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisOutlierOperationButtonPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisOutlierSummaryPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;

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

    public static final String PARAM_IS_WIZARD = "isWizard";
    boolean isWizardPanel = false;

    public PageRoleAnalysisOutlier() {
        super();
    }

    public PageRoleAnalysisOutlier(PageParameters params) {
        super(params);
    }

    public PageRoleAnalysisOutlier(PrismObject<RoleAnalysisOutlierType> outlier) {
        super(outlier);

    }

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

    @Override
    protected String getMainPanelCssClass() {
        return "col p-0 rounded";
    }

    @Override
    protected String getMainPanelCssStyle() {
        return "align-items: stretch;";
    }

    @Override
    public void addAdditionalButtons(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_RECYCLE, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton recertifyButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(), iconBuilder.build(),
                createStringResource("PageRoleAnalysisOutlier.button.mark.outlier")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                //TODO
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        recertifyButton.titleAsLabel(true);
        recertifyButton.setOutputMarkupId(true);
        recertifyButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-primary"));
        repeatingView.add(recertifyButton);

        Form<?> form = recertifyButton.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(recertifyButton);
        }
    }

    @Override
    public Class<RoleAnalysisOutlierType> getType() {
        return RoleAnalysisOutlierType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisOutlierType> summaryModel) {
        return null;
    }

    @Override
    protected Panel createVerticalSummaryPanel(String id, IModel<RoleAnalysisOutlierType> summaryModel) {
        return new RoleAnalysisOutlierSummaryPanel(id, summaryModel);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("RoleMining.page.outlier.title");
    }

    @Override
    protected boolean canShowWizard() {
        return isWizardPanel();
    }

    @Override
    protected void onBackPerform(AjaxRequestTarget target) {
        ((PageBase) getPage()).navigateToNext(PageRoleAnalysis.class);
    }

    @Override
    protected boolean supportGenericRepository() {
        return false;
    }

    @Override
    protected boolean supportNewDetailsLook() {
        return true;
    }

    @Override
    protected InlineOperationalButtonsPanel<RoleAnalysisOutlierType> createInlineButtonsPanel(String idButtons, LoadableModel<PrismObjectWrapper<RoleAnalysisOutlierType>> objectWrapperModel) {
        return new RoleAnalysisOutlierOperationButtonPanel(idButtons, objectWrapperModel) {

            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageRoleAnalysisOutlier.this.savePerformed(target);
            }

            @Override
            protected void backPerformed(AjaxRequestTarget target) {
                super.backPerformed(target);
                onBackPerform(target);
            }

            @Override
            protected void addRightButtons(@NotNull RepeatingView rightButtonsView) {
                addAdditionalButtons(rightButtonsView);
            }

            @Override
            protected void deleteConfirmPerformed(AjaxRequestTarget target) {
                super.deleteConfirmPerformed(target);
                afterDeletePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageRoleAnalysisOutlier.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected VisibleEnableBehaviour getPageTitleBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

}

