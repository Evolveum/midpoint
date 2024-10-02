/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard.mode;

import static com.evolveum.midpoint.gui.api.GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
import static com.evolveum.midpoint.gui.api.GuiStyleConstants.CLASS_OBJECT_USER_ICON;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AnalysisCategoryMode;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

@PanelType(name = "ra-process")
@PanelInstance(identifier = "ra-process",
        applicableForType = RoleAnalysisSessionType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRoleAnalysisSession.wizard.step.choice", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ProcessModeChoiceStepPanel extends EnumWizardChoicePanel<ProcessModeChoiceStepPanel.ProcessMode, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final String PANEL_TYPE = "ra-process";

    /**
     * @param id Panel ID
     * @param resourceModel AssignmentHolderDetailsModel
     **/
    public ProcessModeChoiceStepPanel(String id, AssignmentHolderDetailsModel<RoleAnalysisSessionType> resourceModel) {
        super(id, resourceModel, ProcessMode.class);
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
    protected void onTileClickPerformed(@NotNull ProcessMode value, AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask("prepare options");
        OperationResult result = task.getResult();

        PrismObjectWrapper<RoleAnalysisSessionType> objectWrapper = getAssignmentHolderDetailsModel().getObjectWrapper();
        RoleAnalysisSessionType session;
        try {
            session = objectWrapper.getObjectApplyDelta().asObjectable();
        } catch (CommonException e) {
            session = objectWrapper.getObjectOld().asObjectable();
        }
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        analysisOption.setProcessMode(ProcessMode.ROLE == value ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER);
        AnalysisCategoryMode categoryMode = AnalysisCategoryMode.resolveCategoryMode(session);
        categoryMode.generateConfiguration(getPageBase().getRoleAnalysisService(), session, task, result);

        reloadDefaultConfiguration(session);
        onSubmitPerformed(target);
    }

    protected void reloadDefaultConfiguration(RoleAnalysisSessionType sessionType) {

    }

    public enum ProcessMode implements TileEnum {
        USER(CLASS_OBJECT_USER_ICON, "ProcessMode.USER_MODE.description"),
        ROLE(CLASS_OBJECT_ROLE_ICON, "ProcessMode.ROLE_MODE.description");

        private final String icon;
        private final String descriptionKey;

        ProcessMode(String icon, String descriptionKey) {
            this.icon = icon;
            this.descriptionKey = descriptionKey;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        public String getDescriptionKey() {
            return descriptionKey;
        }
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
    protected String getDescriptionForTile(@NotNull ProcessMode type) {
        return createStringResource(type.getDescriptionKey()).getString();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.process.mode.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.process.mode.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.choice.analysis.process.mode.subText");
    }

}
