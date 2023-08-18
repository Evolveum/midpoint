/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.ClassVisitFilter;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

/**
 * @author lskublik
 */
@PanelType(name = "arw-basic")
@PanelInstance(identifier = "arw-basic",
        applicableForType = RoleAnalysisSessionType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageRoleAnalysisSession.wizard.step.basicInformation", icon = "fa fa-wrench"),
        containerPath = "empty")
public class BasicSessionInformationStepPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    public static final String PANEL_TYPE = "arw-basic";

    public BasicSessionInformationStepPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        getDetailsModel().getObjectWrapper().setShowEmpty(false, false);
        getDetailsModel().getObjectWrapper().getValues().forEach(valueWrapper -> valueWrapper.setShowEmpty(true));
        super.onInitialize();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(RoleAnalysisSessionType.F_LIFECYCLE_STATE)
                    || wrapper.getItemName().equals(RoleAnalysisSessionType.F_INDESTRUCTIBLE)
                    || wrapper.getItemName().equivalent(RoleAnalysisSessionType.F_PROCESS_MODE)) {
                return ItemVisibility.HIDDEN;
            }

            return ItemVisibility.AUTO;
        };
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
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.basicInformation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.basicInformation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.basicInformation.subText");
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(RoleAnalysisSessionType.F_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }
}
