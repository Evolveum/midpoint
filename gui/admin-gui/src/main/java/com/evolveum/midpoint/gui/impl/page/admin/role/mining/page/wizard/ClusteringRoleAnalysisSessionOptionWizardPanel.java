/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils;
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

public class ClusteringRoleAnalysisSessionOptionWizardPanel extends AbstractFormWizardStepPanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final String WORK_PANEL_TYPE = "rm-option-clustering";

    public ClusteringRoleAnalysisSessionOptionWizardPanel(AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(model);

    }

    @Override
    protected void onInitialize() {
//        try {
//
//            Task task = getPageBase().createSimpleTask("countObjects");
//            OperationResult result = task.getResult();
//            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
//            RoleAnalysisOptionType processModeObject = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
//            RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();
//
//            PrismContainerValueWrapper<AbstractAnalysisSessionOptionType> sessionType = getContainerFormModel().getObject()
//                    .getValue();
//
//            Class<? extends ObjectType> propertiesClass = UserType.class;
//            Class<? extends ObjectType> membersClass = RoleType.class;
//            if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
//                propertiesClass = RoleType.class;
//                membersClass = UserType.class;
//            }
//
//            Integer maxPropertiesObjects;
//            Integer maxMembersObjects;
//
//            ModelService modelService = getPageBase().getModelService();
//
//            maxPropertiesObjects = modelService.countObjects(propertiesClass, null, null, task, result);
//            maxMembersObjects = modelService.countObjects(membersClass, null, null, task, result);
//
//            if (maxPropertiesObjects == null) {
//                maxPropertiesObjects = 1000000;
//            }
//
//            if (maxMembersObjects == null) {
//                maxMembersObjects = 1000000;
//            }
//
//            double minMembersObject = maxMembersObjects < 10 ? 2.0 : 10;
//            double minObject = maxPropertiesObjects < 10 ? 1.0 : 10;
//
//            if (sessionType.getNewValue().getValue().getSimilarityThreshold() == null) {
//                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD, 80.0);
//            }
//
//            if (sessionType.getNewValue().getValue().getMinMembersCount() == null) {
//                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT, minMembersObject);
//            }
//
//            if (sessionType.getNewValue().getValue().getMinPropertiesOverlap() == null) {
//                setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP, minObject);
//            }
//
//            AnalysisAttributeSettingType value = new AnalysisAttributeSettingType();
//            List<AnalysisAttributeRuleType> analysisAttributeRule = new ArrayList<>();
//            RoleAnalysisAttributeDef title = RoleAnalysisAttributeDefUtils.getTitle();
//            RoleAnalysisAttributeDef archetypeRef = RoleAnalysisAttributeDefUtils.getArchetypeRef();
//            RoleAnalysisAttributeDef locality = RoleAnalysisAttributeDefUtils.getLocality();
//            RoleAnalysisAttributeDef orgAssignment = RoleAnalysisAttributeDefUtils.getOrgAssignment();
//
//            analysisAttributeRule
//                    .add(new AnalysisAttributeRuleType()
//                            .attributeIdentifier(title.getDisplayValue())
//                            .propertyType(UserType.COMPLEX_TYPE));
//            analysisAttributeRule
//                    .add(new AnalysisAttributeRuleType()
//                            .attributeIdentifier(archetypeRef.getDisplayValue())
//                            .propertyType(UserType.COMPLEX_TYPE));
//            analysisAttributeRule
//                    .add(new AnalysisAttributeRuleType()
//                            .attributeIdentifier(locality.getDisplayValue())
//                            .propertyType(UserType.COMPLEX_TYPE));
//            analysisAttributeRule
//                    .add(new AnalysisAttributeRuleType()
//                            .attributeIdentifier(orgAssignment.getDisplayValue())
//                            .propertyType(UserType.COMPLEX_TYPE));
//            analysisAttributeRule
//                    .add(new AnalysisAttributeRuleType()
//                            .attributeIdentifier(archetypeRef.getDisplayValue())
//                            .propertyType(RoleType.COMPLEX_TYPE));
//
//            value.getAnalysisAttributeRule().addAll(analysisAttributeRule);
//            setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING, value);
//
//            setNewValue(sessionType, AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP, minObject);
//
//        } catch (SchemaException e) {
//            throw new RuntimeException("Failed to update values session clustering options values", e);
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
        return itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING)
                || itemName.equivalent(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            ItemName itemName = wrapper.getItemName();

            if (itemName.equals(AbstractAnalysisSessionOptionType.F_QUERY)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE)) {
                return ItemVisibility.HIDDEN;
            }

            if (itemName.equals(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING)
                    || itemName.equals(AbstractAnalysisSessionOptionType.F_ANALYSIS_ATTRIBUTE_SETTING)) {
                LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getDetailsModel().getObjectWrapperModel();
                RoleAnalysisOptionType processModeObject = objectWrapperModel.getObject().getObject().asObjectable().getAnalysisOption();
                RoleAnalysisCategoryType analysisCategory = processModeObject.getAnalysisCategory();
                if (analysisCategory.equals(RoleAnalysisCategoryType.STANDARD)) {
                    return ItemVisibility.HIDDEN;
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
