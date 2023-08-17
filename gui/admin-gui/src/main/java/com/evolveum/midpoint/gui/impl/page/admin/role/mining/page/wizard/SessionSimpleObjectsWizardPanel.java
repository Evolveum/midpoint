/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAnalysisSessionOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class SessionSimpleObjectsWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "tw-work";

    public SessionSimpleObjectsWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected void onInitialize() {
        try {
            PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType = getContainerFormModel().getObject()
                    .getValue();

            setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD, 80.0);
            setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT, 10);
            setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE, new RangeType()
                    .min(10.0)
                    .max(100.0));
            setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP, 10);

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        super.onInitialize();
    }

    private void setNewValue(PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType,
            ItemName itemName, Object realValue) throws SchemaException {

        sessionType.findProperty(itemName).getValue().setRealValue(realValue);

    }

    @Override
    protected IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getContainerFormModel() {
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
        RoleAnalysisProcessModeType processMode = objectWrapperModel.getObject().getObject().asObjectable().getProcessMode();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
        }
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
    }

    @Override
    protected String getPanelType() {
        return WORK_PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.detection.option");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.detection.option.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.detection.option.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }
}
