/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardParentStep;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-relation")
@PanelInstance(identifier = "cdw-relation",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.relation", icon = "fa fa-wrench"),
        containerPath = "empty")
public class RelationConnectorStepPanel extends AbstractFormWizardStepPanel<ConnectorDevelopmentDetailsModel> implements WizardParentStep {

    private static final Trace LOGGER = TraceManager.getTrace(RelationConnectorStepPanel.class);

    private static final String PANEL_TYPE = "cdw-relation";

    private String relation;
    private IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel;

    public RelationConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
        createValueModel(helper.getDetailsModel().getServiceLocator());
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ConnectorDevelopmentType.F_APPLICATION);
    }

    @Override
    protected void onInitialize() {
        getDetailsModel().getObjectWrapper().setShowEmpty(false, false);
        getDetailsModel().getObjectWrapper().getValues().forEach(valueWrapper -> valueWrapper.setShowEmpty(false));
        super.onInitialize();
    }

    private void createValueModel(ModelServiceLocator modelServiceLocator) {
        valueModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerValueWrapper<ConnDevRelationInfoType> load() {
                if (relation == null) {
                    relation = getHelper().getVariable(RelationSelectConnectorStepPanel.RELATION_NAME);
                    getHelper().removeVariable(RelationSelectConnectorStepPanel.RELATION_NAME);
                }

                PrismContainerWrapperModel<ConnectorDevelopmentType, ConnDevRelationInfoType> model
                        = PrismContainerWrapperModel.fromContainerWrapper(
                        getDetailsModel().getObjectWrapperModel(),
                        ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_RELATION));

                if (model.getObject().getValues().isEmpty()
                        || (relation == null && model.getObject().getValues().stream().noneMatch(
                        value -> value.getStatus() ==  ValueStatus.ADDED && StringUtils.isEmpty(value.getRealValue().getName())))) {
                    try {
                        PrismContainerValue<ConnDevRelationInfoType> newItem = model.getObject().getItem().createNewValue();
                        PrismContainerValueWrapper<ConnDevRelationInfoType> newItemWrapper = WebPrismUtil.createNewValueWrapper(
                                model.getObject(), newItem, modelServiceLocator);
                        model.getObject().getValues().add(newItemWrapper);
                        newItemWrapper.setExpanded(true);
                        newItemWrapper.setShowEmpty(true);
                        return newItemWrapper;
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create new value for limitation container", e);
                        return null;
                    }
                }

                PrismContainerValueWrapper<ConnDevRelationInfoType> newItemWrapper = model.getObject().getValues().stream()
                        .filter(value ->
                                (!StringUtils.isEmpty(relation)
                                        && StringUtils.equals(relation, value.getRealValue().getName()))
                                        || (relation == null && value.getStatus() == ValueStatus.ADDED && StringUtils.isEmpty(value.getRealValue().getName())))
                        .findFirst()
                        .orElse(model.getObject().getValues().get(0));
                newItemWrapper.setExpanded(true);
                newItemWrapper.setShowEmpty(true);
                return newItemWrapper;
            }
        };
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ((VerticalFormPrismContainerPanel)getVerticalForm().getSingleContainerPanel().getContainer().get("1"))
                .getContainer().add(AttributeAppender.remove("class"));
    }

    @Override
    protected void initLayout() {
//        getTopLevelContainer().add(AttributeAppender.replace("class", "d-flex flex-column col-9 mt-2"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(this::checkMandatory)
                .build();
        VerticalFormPanel panel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration()) {
            @Override
            protected String getIcon() {
                return RelationConnectorStepPanel.this.getIcon();
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getFormTitle();
            }

            @Override
            protected WrapperContext createWrapperContext() {
                return getDetailsModel().createWrapperContext();
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return false;
            }

            @Override
            protected boolean isHeaderVisible(IModel model) {
                return false;
            }

            @Override
            protected String getCssClassForFormContainerOfValuePanel() {
                return "";
            }
        };
        panel.setOutputMarkupId(true);
        panel.add(AttributeAppender.replace("class", "col-12"));
        add(panel);
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
        return () -> {
            String title = createStringResource("PageConnectorDevelopment.wizard.step.relation").getString();
            if (StringUtils.isNotEmpty(valueModel.getObject().getRealValue().getName())) {
                title += ": " + valueModel.getObject().getRealValue().getName();
            }
            return title;
        };
    }
    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relation.subText");
    }

//    protected boolean checkMandatory(ItemWrapper itemWrapper) {
//        if (itemWrapper.getItemName().equals(ConnDevApplicationInfoType.F_APPLICATION_NAME)) {
//            return true;
//        }
//        return itemWrapper.isMandatory();
//    }

//    @Override
//    protected ItemVisibilityHandler getVisibilityHandler() {
//        return wrapper -> {
//            if (wrapper.getItemName().equals(ResourceType.F_CONNECTOR_REF)
//                    || wrapper.getItemName().equals(ResourceType.F_TEMPLATE)
//                    || wrapper.getItemName().equals(ResourceType.F_ABSTRACT)
//                    || wrapper.getItemName().equals(ResourceType.F_SCRIPTS)){
//                return ItemVisibility.HIDDEN;
//            }
//            return ItemVisibility.AUTO;
//        };
//    }

    @Override
    public String getStepId() {
        if (StringUtils.isNotEmpty(valueModel.getObject().getRealValue().getName())) {
            return getPanelType() + "-" + StringUtils.normalizeSpace(valueModel.getObject().getRealValue().getName());
        }
        return getDefaultStepId();
    }

    @Override
    public String getDefaultStepId() {
        return getPanelType();
    }

    @Override
    public String appendCssToWizard() {
        return "";
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public List<WizardStep> createChildrenSteps() {
        return List.of(
                new RelationSelectConnectorStepPanel(getHelper(), valueModel),
                new WaitingRelationScriptConnectorStepPanel(getHelper(), valueModel),
                new RelationScriptConnectorStepPanel(getHelper()));
    }
}
