/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.mode;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.CommonException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AnalysisCategoryMode;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcedureType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

@PanelType(name = "ra-category")
@PanelInstance(identifier = "ra-category",
        applicableForType = RoleAnalysisSessionType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRoleAnalysisSession.wizard.step.choice", icon = "fa fa-wrench"),
        containerPath = "empty")
public class AnalysisCategoryChoiceStepPanel extends EnumWizardChoicePanel<AnalysisCategoryMode, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final String PANEL_TYPE = "ra-category";

    LoadableModel<AnalysisCategoryMode> analysisCategoryModel;

    public AnalysisCategoryChoiceStepPanel(String id, AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(id, model, AnalysisCategoryMode.class);
        this.analysisCategoryModel = new LoadableModel<>(false) {
            @Override
            protected AnalysisCategoryMode load() {
                return null;
            }
        };
    }

    @Override
    protected boolean addDefaultTile() {
        return false;
    }

    @Override
    protected QName getObjectType() {
        return null;
    }

    @Override
    protected void onTileClickPerformed(AnalysisCategoryMode category, AjaxRequestTarget target) {
        analysisCategoryModel.setObject(category);

        Task task = getPageBase().createSimpleTask("prepare options");
        OperationResult result = task.getResult();

        PrismObjectWrapper<RoleAnalysisSessionType> sessionWrapper = getAssignmentHolderDetailsModel().getObjectWrapper();
        RoleAnalysisSessionType session;
        try {
            session = sessionWrapper.getObjectApplyDelta().asObjectable();
        } catch (CommonException e) {
            session = sessionWrapper.getObjectOld().asObjectable();
        }

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        if (analysisOption == null) {
            analysisOption = new RoleAnalysisOptionType();
            session.setAnalysisOption(analysisOption);
        }
        analysisOption.setAnalysisCategory(category.resolveCategoryMode());

        if (!category.requiresProcessModeConfiguration()) {
            session.getAnalysisOption().setProcessMode(category.getRequiredProcessModeConfiguration());
        }


        category.generateConfiguration(getPageBase().getRoleAnalysisService(), session, task, result);

        reloadDefaultConfiguration(session);

        onSubmitPerformed(target);
    }

    protected void reloadDefaultConfiguration(RoleAnalysisSessionType session) {

    }


    protected boolean isRequiresProcessModeConfiguration() {
        if (analysisCategoryModel.getObject() == null) {
            return true;
        }
        return analysisCategoryModel.getObject().requiresProcessModeConfiguration();
    }

    @Override
    protected Component createTilePanel(String id, @NotNull IModel<Tile<AnalysisCategoryMode>> tileModel) {

        RoleAnalysisSessionType session = getAssignmentHolderDetailsModel().getObjectType();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcedureType procedureType = analysisOption.getAnalysisProcedureType();

        boolean isVisible = true;
        AnalysisCategoryMode category = tileModel.getObject().getValue();

        if (procedureType == RoleAnalysisProcedureType.ROLE_MINING) {
            if (category == AnalysisCategoryMode.OUTLIERS_DEPARTMENT
                    || category == AnalysisCategoryMode.OUTLIER_DETECTION_ADVANCED) {
                isVisible = false;
            }
        } else {
            if (category != AnalysisCategoryMode.OUTLIERS_DEPARTMENT
                    && category != AnalysisCategoryMode.OUTLIER_DETECTION_ADVANCED) {
                isVisible = false;
            }
        }

        Component tilePanel = super.createTilePanel(id, tileModel);
        tilePanel.setVisible(isVisible);
        return tilePanel;
    }

    @Override
    protected void onBeforeRender() {
        visitParents(Form.class, (form, visit) -> {
            form.setMultiPart(true);
            visit.stop();
        }, new ClassVisitFilter(Form.class) {
            @Override
            public boolean visitObject(Object object) {
                return super.visitObject(object) && "mainForm".equals(((Form) object).getId());
            }
        });
        super.onBeforeRender();
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getDescriptionForTile(@NotNull AnalysisCategoryMode type) {
        return createStringResource(type.getDescriptionKey()).getString();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.category.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.category.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.category.subText");
    }

}
