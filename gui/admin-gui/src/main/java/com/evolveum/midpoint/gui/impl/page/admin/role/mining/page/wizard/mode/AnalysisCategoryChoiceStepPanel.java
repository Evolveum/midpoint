/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.mode;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    protected void onTileClickPerformed(AnalysisCategoryMode value, AjaxRequestTarget target) {
        analysisCategoryModel.setObject(value);
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getAssignmentHolderDetailsModel()
                .getObjectWrapperModel();

        PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper = objectWrapperModel
                .getObject();

        PrismContainer<Containerable> property = objectWrapper.getObject()
                .findContainer(RoleAnalysisSessionType.F_ANALYSIS_OPTION);

        property.findProperty(RoleAnalysisOptionType.F_ANALYSIS_CATEGORY)
                .setRealValue(value.resolveCategoryMode());
        if (!value.requiresProcessModeConfiguration()) {
            property.findProperty(RoleAnalysisOptionType.F_PROCESS_MODE)
                    .setRealValue(value.getRequiredProcessModeConfiguration());
            Task task = getPageBase().createSimpleTask("prepare options");
            OperationResult result = task.getResult();
            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
            value.generateConfiguration(
                    roleAnalysisService, objectWrapperModel, task, result);
        }
        onSubmitPerformed(target);
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
