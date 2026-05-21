/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-supported-auth")
@PanelInstance(identifier = "cdw-supported-auth",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.supportedAuthMethod", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SupportedAuthMethodConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-supported-auth";

    private static final String ID_PANEL = "panel";
    private static final String ID_CHECK = "check";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_RECOMMENDED_BADGE = "recommendedBadge";
    private static final String ID_SHOW_ALL = "showAll";
    private static final String ID_HIDE_ALL = "hideAll";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevAuthInfoType>>> valuesModel;
    private IModel<Boolean> showAllModel = Model.of(false);

    public SupportedAuthMethodConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        valuesModel = new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ConnDevAuthInfoType>> load() {
                try {
                    PrismContainerWrapper<ConnDevAuthInfoType> container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_AUTH));
                    List<PrismContainerValueWrapper<ConnDevAuthInfoType>> values = new ArrayList<>(container.getValues());

                    if (showAllModel.getObject()) {
                        for (SupportedAuthorization auth : SupportedAuthorization.values()) {
                            if (auth == SupportedAuthorization.NONE) {
                                continue;
                            }
                            boolean alreadyPresent = values.stream()
                                    .anyMatch(v -> auth.crateBasicInformation().getType().equals(v.getRealValue().getType()));
                            if (!alreadyPresent) {
                                ConnDevAuthInfoType newVal = auth.crateBasicInformation();
                                values.add(getPageBase().createValueWrapper(container, newVal.asPrismContainerValue(), ValueStatus.NOT_CHANGED, getDetailsModel().createWrapperContext()));
                            }
                        }
                    } else {
                        values = values.stream()
                                .filter(v -> Boolean.TRUE.equals(v.getRealValue().isRecommended()))
                                .collect(Collectors.toCollection(ArrayList::new));
                    }

                    values.sort(Comparator.comparing((PrismContainerValueWrapper<ConnDevAuthInfoType> v) ->
                                    !Boolean.TRUE.equals(v.getRealValue().isRecommended()))
                            .thenComparing(v -> v.getRealValue().getName()));

                    if (values.size() == 1) {
                        values.get(0).setSelected(true);
                    }
                    return values;
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        ListView<PrismContainerValueWrapper<ConnDevAuthInfoType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevAuthInfoType>> listItem) {
                listItem.setOutputMarkupId(true);
                if (listItem.getIndex() == valuesModel.getObject().size() - 1) {
                    listItem.add(AttributeAppender.append("class", "card-body py-3"));
                } else {
                    listItem.add(AttributeAppender.append("class", "card-header py-3"));
                }
                listItem.add(AttributeAppender.append("style", "cursor: pointer;"));

                listItem.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<ConnDevAuthInfoType> wrapper = listItem.getModelObject();
                        wrapper.setSelected(!wrapper.isSelected());
                        target.add(listItem);
                    }
                });

                CheckPanel check = new CheckPanel(ID_CHECK, new PropertyModel<>(listItem.getModel(), "selected"));
                check.setOutputMarkupId(true);
                listItem.add(check);

                Label name = new Label(ID_NAME, () -> listItem.getModelObject().getRealValue().getName());
                name.setOutputMarkupId(true);
                listItem.add(name);

                WebMarkupContainer recommendedBadge = new WebMarkupContainer(ID_RECOMMENDED_BADGE);
                recommendedBadge.setVisible(Boolean.TRUE.equals(listItem.getModelObject().getRealValue().isRecommended()));
                listItem.add(recommendedBadge);

                Label description = new Label(ID_DESCRIPTION, () -> listItem.getModelObject().getRealValue().getDescription());
                description.setOutputMarkupId(true);
                listItem.add(description);
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);

        AjaxLink<Void> showAllLink = new AjaxLink<>(ID_SHOW_ALL) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showAllModel.setObject(true);
                target.add(SupportedAuthMethodConnectorStepPanel.this);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(!showAllModel.getObject());
            }
        };
        add(showAllLink);

        AjaxLink<Void> hideAllLink = new AjaxLink<>(ID_HIDE_ALL) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showAllModel.setObject(false);
                target.add(SupportedAuthMethodConnectorStepPanel.this);
            }

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(showAllModel.getObject());
            }
        };
        add(hideAllLink);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.supportedAuthMethod");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.supportedAuthMethod.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.supportedAuthMethod.subText");
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ConnDevApplicationInfoType.F_APPLICATION_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public String appendCssToWizard() {
        return "col-12 col-xl-10 col-xxl-8";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ConnDevAuthInfoType> container =
                    getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_AUTH));

            List<PrismContainerValueWrapper<ConnDevAuthInfoType>> valuesForAdd = valuesModel.getObject()
                    .stream().filter(PrismContainerValueWrapper::isSelected)
                    .map(value -> {
                        try {
                            //noinspection unchecked
                            return (PrismContainerValueWrapper<ConnDevAuthInfoType>) getPageBase().createValueWrapper(
                                    container, value.getRealValue().asPrismContainerValue().clone(), ValueStatus.ADDED, getDetailsModel().createWrapperContext());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();

            valuesForAdd.forEach(value -> {
                try {
                    container.getItem().add(value.getRealValue().asPrismContainerValue());
                    container.getValues().add(value);
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    @Override
    public boolean isCompleted() {
        return ConnectorDevelopmentWizardUtil.existContainerValue(
                getDetailsModel().getObjectWrapper(),
                ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_AUTH));
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
