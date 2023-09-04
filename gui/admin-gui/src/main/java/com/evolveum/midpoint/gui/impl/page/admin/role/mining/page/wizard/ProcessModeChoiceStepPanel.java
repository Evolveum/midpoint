/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.wizard.EnumWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

import static com.evolveum.midpoint.gui.api.GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
import static com.evolveum.midpoint.gui.api.GuiStyleConstants.CLASS_OBJECT_USER_ICON;

@PanelType(name = "rm-process")
@PanelInstance(identifier = "rm-process",
        applicableForType = RoleAnalysisSessionType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRoleAnalysisSession.wizard.step.choice", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ProcessModeChoiceStepPanel extends EnumWizardChoicePanel<ProcessModeChoiceStepPanel.ProcessMode, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final String PANEL_TYPE = "rm-process";

    /**
     * @param id
     * @param resourceModel
     **/
    public ProcessModeChoiceStepPanel(String id, AssignmentHolderDetailsModel<RoleAnalysisSessionType> resourceModel) {
        super(id, resourceModel, ProcessMode.class);
    }

    @Override
    protected void addDefaultTile(List<Tile<ProcessMode>> list) {

    }



    @Override
    protected QName getObjectType() {
        return null;
    }

    @Override
    protected void onTileClickPerformed(ProcessMode value, AjaxRequestTarget target) {

        try {
            RoleAnalysisProcessModeType mode;
            if (value.equals(ProcessMode.ROLE)) {
                mode = RoleAnalysisProcessModeType.ROLE;
            } else {
                mode = RoleAnalysisProcessModeType.USER;
            }

            getAssignmentHolderDetailsModel().getObjectWrapper().findProperty(RoleAnalysisSessionType.F_PROCESS_MODE).getValue().setRealValue(mode);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        onSubmitPerformed(target);
    }


    public enum ProcessMode implements TileEnum {
        ROLE(CLASS_OBJECT_ROLE_ICON),
        USER(CLASS_OBJECT_USER_ICON);

        private final String icon;

        ProcessMode(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
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
