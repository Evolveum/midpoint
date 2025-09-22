/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.ButtonBar;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectContainerActionTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.MultiSelectTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassBasicConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.BasicPrimItemDefinitionPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.DefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-documentation")
@PanelInstance(identifier = "cdw-documentation",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.documentation", icon = "fa fa-wrench"),
        containerPath = "empty")
public class DocumentationConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(DocumentationConnectorStepPanel.class);

    private static final String PANEL_TYPE = "cdw-documentation";
    static final String TASK_DOCUMENTATION_KEY = "taskDocumentationKey";

    private static final String CLASS_DOT = DocumentationConnectorStepPanel.class.getName() + ".";
    private static final String OP_LOAD_DOCS = CLASS_DOT + "loadDocumentations";

    private static final String ID_PANEL = "panel";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevDocumentationSourceType>>> valuesModel;

    public DocumentationConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        valuesModel = new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ConnDevDocumentationSourceType>> load() {
                try {
                    PrismContainerWrapper<ConnDevDocumentationSourceType> container = getDetailsModel().getObjectWrapper().findContainer(ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE);
                    return container.getValues();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Task task = getPageBase().createSimpleTask(OP_LOAD_DOCS);
        OperationResult result = task.getResult();
        String token = getHelper().getVariable(TASK_DOCUMENTATION_KEY);

        StatusInfo<ConnDevDiscoverDocumentationResultType> statusInfo;
        try {
            statusInfo =
                    getDetailsModel().getServiceLocator().getConnectorService().getDiscoverDocumentationStatus(token, task, result);
        } catch (SchemaException | ObjectNotFoundException e) {
            throw new RuntimeException(e);
        }
        ConnDevDiscoverDocumentationResultType documentationResultBean = statusInfo.getResult();

        PrismContainer<ConnDevDocumentationSourceType> suggestionsParent = documentationResultBean.asPrismContainerValue()
                .findContainer(ConnDevDiscoverDocumentationResultType.F_DOCUMENTATION);


        if (suggestionsParent == null) {
            return;
        }

        try {
            PrismContainerWrapper<ConnDevDocumentationSourceType> parentWrapper =
                    getDetailsModel().getObjectWrapper().findContainer(ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE);

            suggestionsParent.getValues().stream()
                    .filter(suggestedValue ->
                                    StringUtils.isNotEmpty(((ConnDevDocumentationSourceType)suggestedValue.getRealValue()).getName())
                                    && parentWrapper.getValues().stream().noneMatch(value ->
                                            StringUtils.equals(((ConnDevDocumentationSourceType)suggestedValue.getRealValue()).getName(), value.getRealValue().getName())))
                    .map(suggestedValue -> {
                        try {
                            PrismContainerValue<ConnDevDocumentationSourceType> clone = suggestedValue.clone();
                            AiUtil.markContainerValueAsAiProvided(clone);
                            //noinspection unchecked
                            return (PrismContainerValueWrapper<ConnDevDocumentationSourceType>) getPageBase().createValueWrapper(
                                    parentWrapper, clone, ValueStatus.ADDED, getDetailsModel().createWrapperContext());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .forEach(value -> {
                        try {
                            //noinspection unchecked
                            parentWrapper.getItem().add(value.getRealValue().asPrismContainerValue());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                        parentWrapper.getValues().add(value);
                    });


        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private void initLayout() {
        setOutputMarkupId(true);
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        MultiSelectContainerActionTileTablePanel<PrismContainerValueWrapper<ConnDevDocumentationSourceType>, ConnDevDocumentationSourceType, DocumentationTile> panel = new MultiSelectContainerActionTileTablePanel<>(
                ID_PANEL, UserProfileStorage.TableId.PANEL_CONNECTOR_GENERATION_DOCUMENTATION, () -> ViewToggle.TILE) {
            @Override
            protected MultivalueContainerListDataProvider createDataProvider() {
                return new MultivalueContainerListDataProvider(this, getSearchModel(), valuesModel);
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<ConnDevDocumentationSourceType>, String>> createDomainColumns() {
                return List.of();
            }

            @Override
            protected void deselectItem(PrismContainerValueWrapper<ConnDevDocumentationSourceType> entry) {
            }

            @Override
            protected IModel<String> getItemLabelModel(PrismContainerValueWrapper<ConnDevDocumentationSourceType> entry) {
                return null;
            }

            @Override
            protected IModel<List<PrismContainerValueWrapper<ConnDevDocumentationSourceType>>> getSelectedItemsModel() {
                return getSelectedContainerItemsModel();
            }

            @Override
            protected Class<? extends Containerable> getType() {
                return ConnDevDocumentationSourceType.class;
            }

            @Override
            protected Component createTile(String id, IModel<DocumentationTile> model) {
                return new DocumentationTilePanel(id, model) {
                    @Override
                    protected void onDelete(DocumentationTile modelObject, AjaxRequestTarget target) {
                        try {
                            PrismContainerWrapper<ConnDevDocumentationSourceType> container = getDetailsModel().getObjectWrapper().findContainer(ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE);
                            container.remove(modelObject.getValue(), getPageBase());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                        valuesModel.detach();
                        refreshAndDetach(target);
                    }
                };
            }

            @Override
            protected DocumentationTile createTileObject(PrismContainerValueWrapper<ConnDevDocumentationSourceType> object) {
                return new DocumentationTile(object);
            }

            @Override
            protected List<Component> createToolbarButtonsList(String idButton) {
                List<Component> buttonsList = new ArrayList<>();
                buttonsList.add(createTableActionToolbar(idButton));
                return buttonsList;
            }

            @Override
            protected boolean isTogglePanelVisible() {
                return false;
            }

            @Override
            protected Component createHeader(String id) {
                RepeatingView buttons = new RepeatingView(id);
                AjaxIconButton addUrl = new AjaxIconButton(buttons.newChildId(),
                        Model.of(GuiStyleConstants.CLASS_ADD_NEW_OBJECT),
                        createStringResource("DocumentationConnectorStepPanel.addUrl")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onAddUrlPerformed(target);
                    }
                };

                addUrl.showTitleAsLabel(true);
                addUrl.add(AttributeAppender.replace("class", "btn btn-default rounded-0 ml-auto"));
                addUrl.add(AttributeAppender.replace("style", "border-right: 0 !important;"));
                buttons.add(addUrl);

                AjaxIconButton uploadFile = new AjaxIconButton(buttons.newChildId(),
                        Model.of(GuiStyleConstants.CLASS_UPLOAD),
                        createStringResource("DocumentationConnectorStepPanel.uploadFile")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                    }
                };

                uploadFile.showTitleAsLabel(true);
                uploadFile.add(AttributeAppender.replace("class", "btn btn-default rounded-0"));
                buttons.add(uploadFile);
                return buttons;
            }

            @Override
            public void updateRowCssBasedValueStatus(@NotNull Component component, @NotNull PrismContainerValueWrapper<ConnDevDocumentationSourceType> value, boolean isTile) {
            }

            @Override
            protected String getTileCssClasses() {
                return "col-12";
            }

            @Override
            protected String getTileCssStyle() {
                return "";
            }
        };

//        ListView<PrismContainerValueWrapper<ConnDevDocumentationSourceType>> panel = new ListView<>(ID_PANEL, valuesModel) {
//            @Override
//            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevDocumentationSourceType>> listItem) {
//                if (listItem.getIndex() == valuesModel.getObject().size() - 1) {
//                    listItem.add(AttributeAppender.append("class", "card-body py-2"));
//                } else {
//                    listItem.add(AttributeAppender.append("class", "card-header py-2"));
//                }
//
//                CheckPanel check = new CheckPanel(ID_CHECK, new PropertyModel<>(listItem.getModel(), "selected"));
//                check.setOutputMarkupId(true);
//                listItem.add(check);
//
//                IModel<String> iconModel =
//                        () -> !StringUtils.isEmpty(listItem.getModelObject().getRealValue().getUri()) ? "fa fa-globe" : "fa fa-file";
//                WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
//                icon.add(AttributeAppender.append("class", iconModel));
//                listItem.add(icon);
//
//                Label name = new Label(ID_NAME, () -> listItem.getModelObject().getRealValue().getName());
//                name.setOutputMarkupId(true);
//                listItem.add(name);
//
//                Label description = new Label(ID_DESCRIPTION, () -> listItem.getModelObject().getRealValue().getDescription());
//                description.setOutputMarkupId(true);
//                listItem.add(description);
//            }
//        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void onAddUrlPerformed(AjaxRequestTarget target) {
        OnePanelPopupPanel popup = new OnePanelPopupPanel(
                getPageBase().getMainPopupBodyId(),
                createStringResource("DocumentationConnectorStepPanel.addNewDocumentation")) {
            @Override
            protected WebMarkupContainer createPanel(String id) {
                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .headerVisibility(false)
                        .build();
                settings.setConfig(getContainerConfiguration(PANEL_TYPE));

                IModel<PrismContainerValueWrapper<ConnDevDocumentationSourceType>> model = new LoadableModel<>(false) {

                    @Override
                    protected PrismContainerValueWrapper<ConnDevDocumentationSourceType> load() {
                        PrismContainerWrapperModel<ConnectorDevelopmentType, ConnDevDocumentationSourceType> model
                                = PrismContainerWrapperModel.fromContainerWrapper(
                                getDetailsModel().getObjectWrapperModel(),
                                ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE);
                        PrismContainerValueWrapper<ConnDevDocumentationSourceType> newItemWrapper;
                            try {
                                PrismContainerValue<ConnDevDocumentationSourceType> newItem = model.getObject().getItem().createNewValue();
                                newItemWrapper = WebPrismUtil.createNewValueWrapper(
                                        model.getObject(), newItem, getPageBase());
                                model.getObject().getValues().add(newItemWrapper);
                            } catch (SchemaException e) {
                                LOGGER.error("Couldn't create new value for limitation container", e);
                                return null;
                            }
                        newItemWrapper.setExpanded(true);
                        newItemWrapper.setShowEmpty(true);
                        return newItemWrapper;
                    }
                };

                return new VerticalFormPrismContainerValuePanel<>(id, model, settings){
                    @Override
                    protected void onInitialize() {
                        super.onInitialize();
                        ((VerticalFormDefaultContainerablePanel)getValuePanel()).getFormContainer().add(AttributeAppender.remove("class"));
                        get(ID_MAIN_CONTAINER).add(AttributeAppender.remove("class"));
                    }

                    @Override
                    protected boolean isShowEmptyButtonVisible() {
                        return false;
                    }
                };
            }

            @Override
            protected void processHide(AjaxRequestTarget target) {
                valuesModel.detach();
                ((MultiSelectContainerActionTileTablePanel)DocumentationConnectorStepPanel.this.get(ID_PANEL)).refreshAndDetach(target);
                super.processHide(target);
            }
        };
        popup.setOutputMarkupId(true);
        getPageBase().showMainPopup(popup, target);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.documentation");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.documentation.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.documentation.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public String appendCssToWizard() {
        return "col-10";
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
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }
}
