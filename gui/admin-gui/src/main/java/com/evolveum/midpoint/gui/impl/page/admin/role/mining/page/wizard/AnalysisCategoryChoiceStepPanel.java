/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractAnalysisOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AnalysisCategory;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

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
        PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper = getAssignmentHolderDetailsModel().getObjectWrapper();
        RoleAnalysisCategoryType roleAnalysisCategoryType = value.resolveCategoryMode();
        RoleAnalysisSessionType realValue = objectWrapper.getObject().getRealValue();
        RoleAnalysisOptionType analysisOption = realValue.getAnalysisOption();
        analysisOption.setAnalysisCategory(roleAnalysisCategoryType);
        PrismContainer<Containerable> property = objectWrapper.getObject()
                .findContainer(RoleAnalysisSessionType.F_ANALYSIS_OPTION);
        property.findProperty(RoleAnalysisOptionType.F_ANALYSIS_CATEGORY)
                .setRealValue(analysisOption);

        Task task = getPageBase().createSimpleTask("prepare options");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        AbstractAnalysisOption analysisConfiguration = value.generateConfiguration(
                roleAnalysisService, objectWrapper, task, result);
        analysisConfiguration.applySettings();

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
