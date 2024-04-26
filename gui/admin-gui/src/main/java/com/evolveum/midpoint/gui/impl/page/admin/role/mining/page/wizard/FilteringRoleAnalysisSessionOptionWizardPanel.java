/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FilteringRoleAnalysisSessionOptionWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-option-filtering";

    public FilteringRoleAnalysisSessionOptionWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected void onInitialize() {
//        try {
//            Task task = getPageBase().createSimpleTask("countObjects");
//            OperationResult result = task.getResult();
//            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
//            RoleAnalysisOptionType processModeObject = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
//            RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();
//            RoleAnalysisCategoryType analysisCategory = processModeObject.getAnalysisCategory();
//
//            PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> primaryOptions = getContainerFormModel().getObject()
//                    .getValue();
//
//            Class<? extends ObjectType> propertiesClass = UserType.class;
//            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
//                propertiesClass = RoleType.class;
//            }
//
//            Integer maxPropertiesObjects;
//
//            ModelService modelService = getPageBase().getModelService();
//
//            maxPropertiesObjects = modelService.countObjects(propertiesClass, null, null, task, result);
//
//            if (maxPropertiesObjects == null) {
//                maxPropertiesObjects = 1000000;
//            }
//
//            double minObject = maxPropertiesObjects < 10 ? 1.0 : 10;
//            boolean isIndirect = analysisCategory.equals(RoleAnalysisCategoryType.OUTLIERS);
//
//            if (primaryOptions.getNewValue().getValue().isIsIndirect() == null) {
//                setNewValue(primaryOptions, AbstractAnalysisSessionOptionType.F_IS_INDIRECT, isIndirect);
//            }
//
//            if (primaryOptions.getNewValue().getValue().getPropertiesRange() == null
//                    || primaryOptions.getNewValue().getValue().getPropertiesRange().getMin() == null
//                    || primaryOptions.getNewValue().getValue().getPropertiesRange().getMax() == null) {
//                setNewValue(primaryOptions, AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE, new RangeType()
//                        .min(minObject)
//                        .max(maxPropertiesObjects.doubleValue()));
//            }
//
//        } catch (SchemaException e) {
//            throw new RuntimeException("Failed to update values session filtering options values", e);
//        } catch (ObjectNotFoundException | SecurityViolationException | ConfigurationException |
//                CommunicationException | ExpressionEvaluationException e) {
//            throw new RuntimeException("Cloud not count objects", e);
//        }

        super.onInitialize();
    }

    private void setNewValue(PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType,
            ItemName itemName, Object realValue) throws SchemaException {

        sessionType.findProperty(itemName).getValue().setRealValue(realValue);

    }

    @Override
    protected IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getContainerFormModel() {
        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
        RoleAnalysisOptionType processModeObject = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
        RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();

        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
            return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
        }
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(),
                ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        ItemName itemName = itemWrapper.getItemName();
        return itemName.equivalent(AbstractAnalysisSessionOptionType.F_QUERY)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            ItemName itemName = wrapper.getItemName();
            if (itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING)) {
                return ItemVisibility.HIDDEN;
            }

            if (itemName.equals(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)) {
                LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
                RoleAnalysisOptionType option = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
                RoleAnalysisProcessModeType processMode = option.getProcessMode();
                if (!processMode.equals(RoleAnalysisProcessModeType.USER)) {
                    return ItemVisibility.HIDDEN;
                } else {
                    return ItemVisibility.AUTO;

                }
            }

            return ItemVisibility.AUTO;
        };
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
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filtering.option");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filtering.option.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageRoleAnalysisSession.wizard.step.work.filtering.option.subText");
    }

    @Override
    public String getStepId() {
        return WORK_PANEL_TYPE;
    }
}
