/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

public class RoleAnalysisSessionSimpleObjectsWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-options";

    public RoleAnalysisSessionSimpleObjectsWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected void onInitialize() {
        try {
            Task task = getPageBase().createSimpleTask("countObjects");
            OperationResult result = task.getResult();
            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
            RoleAnalysisProcessModeType processMode = objectWrapperModel.getObject().getObject().asObjectable().getProcessMode();

            PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType = getContainerFormModel().getObject()
                    .getValue();

            Class<? extends ObjectType> propertiesClass = UserType.class;
            Class<? extends ObjectType> membersClass = RoleType.class;
            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                propertiesClass = RoleType.class;
                membersClass = UserType.class;
            }

            Integer maxPropertiesObjects;
            Integer maxMembersObjects;

            ModelService modelService = getPageBase().getModelService();

            maxPropertiesObjects = modelService.countObjects(propertiesClass, null, null, task, result);
            maxMembersObjects = modelService.countObjects(membersClass, null, null, task, result);

            if (maxPropertiesObjects == null) {
                maxPropertiesObjects = 1000000;
            }

            if (maxMembersObjects == null) {
                maxMembersObjects = 1000000;
            }

            double minMembersObject = maxMembersObjects < 10 ? 2.0 : 10;
            double minObject = maxPropertiesObjects < 10 ? 1.0 : 10;

            if (sessionType.getNewValue().getValue().getSimilarityThreshold() == null) {
                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD, 80.0);
            }

            if (sessionType.getNewValue().getValue().getMinMembersCount() == null) {
                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT, minMembersObject);
            }

            if (sessionType.getNewValue().getValue().getPropertiesRange() == null
                    || sessionType.getNewValue().getValue().getPropertiesRange().getMin() == null
                    || sessionType.getNewValue().getValue().getPropertiesRange().getMax() == null) {
                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE, new RangeType()
                        .min(minObject)
                        .max(maxPropertiesObjects.doubleValue()));
            }
            if (sessionType.getNewValue().getValue().getMinPropertiesOverlap() == null) {
                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP, minObject);
            }
        } catch (SchemaException e) {
            throw new RuntimeException("Failed to update values session clustering options values", e);
        } catch (ObjectNotFoundException | SecurityViolationException | ConfigurationException |
                CommunicationException | ExpressionEvaluationException e) {
            throw new RuntimeException("Cloud not count objects", e);
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

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        ItemName itemName = itemWrapper.getItemName();
        if (itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE)) {
            return true;
        }
        return itemWrapper.isMandatory();
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
