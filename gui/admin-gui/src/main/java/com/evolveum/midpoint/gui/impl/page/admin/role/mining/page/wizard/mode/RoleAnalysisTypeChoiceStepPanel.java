/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.mode;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.RoleAnalysisProcedureMode;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

@PanelType(name = "ra-type")
@PanelInstance(identifier = "ra-type",
        applicableForType = RoleAnalysisSessionType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRoleAnalysisSession.wizard.step.choice", icon = "fa fa-wrench"),
        containerPath = "empty")
public class RoleAnalysisTypeChoiceStepPanel extends EnumWizardChoicePanel<RoleAnalysisProcedureMode, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final String PANEL_TYPE = "ra-type";

    LoadableModel<RoleAnalysisProcedureMode> analysisCategoryModel;

    public RoleAnalysisTypeChoiceStepPanel(String id, AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(id, model, RoleAnalysisProcedureMode.class);
        this.analysisCategoryModel = new LoadableModel<>(false) {
            @Override
            protected RoleAnalysisProcedureMode load() {
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
    protected void onTileClickPerformed(RoleAnalysisProcedureMode value, AjaxRequestTarget target) {
        analysisCategoryModel.setObject(value);
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getAssignmentHolderDetailsModel()
                .getObjectWrapperModel();

        PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper = objectWrapperModel
                .getObject();

        PrismContainer<Containerable> property = objectWrapper.getObject()
                .findContainer(RoleAnalysisSessionType.F_ANALYSIS_OPTION);

        property.findProperty(RoleAnalysisOptionType.F_ANALYSIS_PROCEDURE_TYPE)
                .setRealValue(value.resolveCategoryMode());
        onSubmitPerformed(target);
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<RoleAnalysisProcedureMode>> tileModel) {
        boolean isVisible = true;
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
    protected String getDescriptionForTile(@NotNull RoleAnalysisProcedureMode type) {
        return createStringResource(type.getDescriptionKey()).getString();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.process.type.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.subText");
    }
}
