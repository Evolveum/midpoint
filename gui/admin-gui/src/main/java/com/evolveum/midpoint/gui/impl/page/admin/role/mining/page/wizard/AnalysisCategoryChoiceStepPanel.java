/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import java.util.List;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AnalysisCategory;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

@PanelType(name = "rm-category")
@PanelInstance(identifier = "rm-category",
        applicableForType = RoleAnalysisSessionType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRoleAnalysisSession.wizard.step.choice", icon = "fa fa-wrench"),
        containerPath = "empty")
public class AnalysisCategoryChoiceStepPanel extends EnumWizardChoicePanel<AnalysisCategory, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final String PANEL_TYPE = "rm-category";

    public AnalysisCategoryChoiceStepPanel(String id, AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(id, model, AnalysisCategory.class);
    }

    @Override
    protected void addDefaultTile(List<Tile<AnalysisCategory>> list) {

    }

    @Override
    protected QName getObjectType() {
        return null;
    }

    @Override
    protected void onTileClickPerformed(AnalysisCategory value, AjaxRequestTarget target) {
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper = getAssignmentHolderDetailsModel().getObjectWrapperModel();
        RoleAnalysisCategoryType roleAnalysisCategoryType = value.resolveCategoryMode();
        RoleAnalysisSessionType realValue = objectWrapper.getObject().getValue().getRealValue();
        RoleAnalysisOptionType analysisOption = realValue.getAnalysisOption();
        analysisOption.setAnalysisCategory(roleAnalysisCategoryType);

        Task task = getPageBase().createSimpleTask("prepare options");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        value.generateConfiguration(
                roleAnalysisService, objectWrapper, task, result);

        onSubmitPerformed(target);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<AnalysisCategory>> tileModel) {

        RoleAnalysisSessionType session = getAssignmentHolderDetailsModel().getObjectType();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        boolean isVisible = true;
        AnalysisCategory category = tileModel.getObject().getValue();

        //TEMPORARY DISABLE OUTLIER ANALYSIS UNTIL IT IS IMPLEMENTED TODO
//        if (category.equals(AnalysisCategory.OUTLIER)) {
//            isVisible = false;
//        }

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            if (category.equals(AnalysisCategory.DEPARTMENT)) {
                isVisible = false;
            }
            if(category.equals(AnalysisCategory.OUTLIER)){
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
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.subText");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

}
