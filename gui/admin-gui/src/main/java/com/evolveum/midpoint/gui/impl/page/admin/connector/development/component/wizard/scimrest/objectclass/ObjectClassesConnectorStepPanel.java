/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTile;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-object-classes")
@PanelInstance(identifier = "cdw-object-classes",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.objectClasses", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ObjectClassesConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> implements WizardParentStep {

    public static final String PANEL_TYPE = "cdw-object-classes";

    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_TABLE = "table";

    private IModel<List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>> objectClassesModel;

    public ObjectClassesConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        objectClassesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> load() {
                PrismContainerWrapperModel<ConnectorDevelopmentType, ConnDevObjectClassInfoType> model = PrismContainerWrapperModel.fromContainerWrapper(
                        getDetailsModel().getObjectWrapperModel(),
                        ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS));

                if (model.getObject() == null) {
                    return List.of();
                }
                return model.getObject().getValues().stream()
                        .filter(value -> value.getStatus() != ValueStatus.ADDED
                                && value.getStatus() != ValueStatus.DELETED)
                        .toList();
            }
        };
    }

    private void initLayout() {
        add(AttributeAppender.replace("class", "col-12"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        AjaxLink<?> addObjectClassButton = new AjaxLink<>(ID_ADD_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getController().initNewObjectClass(target);
            }
        };
        addObjectClassButton.setOutputMarkupId(true);
        add(addObjectClassButton);

        TileTablePanel<TemplateTile<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>, PrismContainerValueWrapper<ConnDevObjectClassInfoType>> table
                = new TileTablePanel<>(ID_TABLE) {
            @Override
            protected ISortableDataProvider createProvider() {
                return new MultivalueContainerListDataProvider<>(
                        this,
                        Model.of(),
                        objectClassesModel) {

                    @Override
                    protected List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> searchThroughList() {
                        return super.searchThroughList();
                    }
                };
            }

            @Override
            protected TemplateTile<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> createTileObject(
                    PrismContainerValueWrapper<ConnDevObjectClassInfoType> object) {
                ConnDevObjectClassInfoType value = object.getRealValue();
                return new TemplateTile<>(null, value.getName(), object)
                        .description(value.getDescription())
                        .addTags(createTags(object));
            }

            @Override
            protected Component createTile(String id, IModel<TemplateTile<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>> model) {
                return new ConnectorObjectClassTilePanel(id, model) {
                    @Override
                    protected void deleteObjectClassPerformed(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<ConnDevObjectClassInfoType> value = model.getObject().getValue();
                        try {
                            value.getParent().remove(value, getDetailsModel().getPageAssignmentHolder());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }

                        OperationResult result = getHelper().onSaveObjectPerformed(target);
                        getDetailsModel().getConnectorDevelopmentOperation();
                        if (result == null || result.isError()) {
                            target.add(getFeedback());
                        }
                        target.add(ObjectClassesConnectorStepPanel.this);
                    }

                    @Override
                    protected void editSearchAllPerformed(String objectClassName, AjaxRequestTarget target) {
                        getController().editSearchAll(objectClassName, target);
                    }

                    @Override
                    protected void editSchemaPerformed(String objectClassName, AjaxRequestTarget target) {
                        getController().editSchema(objectClassName, target);
                    }

                    @Override
                    protected void editCreatePerformed(String objectClassName, AjaxRequestTarget target) {
                        getController().editCreateOp(objectClassName, target);
                    }

                    @Override
                    protected void editUpdatePerformed(String objectClassName, AjaxRequestTarget target) {
                        getController().editUpdateOp(objectClassName, target);
                    }

                    @Override
                    protected void editDeletePerformed(String objectClassName, AjaxRequestTarget target) {
                        getController().editDeleteOp(objectClassName, target);
                    }
                };
            }

            @Override
            protected String getTileCssClasses() {
                return "col-6";
            }

            @Override
            protected boolean showFooter() {
                return objectClassesModel.getObject().size() > 6;
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    private ConnectorDevelopmentController getController() {
        return (ConnectorDevelopmentController) getWizard();
    }

    private List<DisplayType> createTags(PrismContainerValueWrapper<ConnDevObjectClassInfoType> value) {
        Map<String, List<ItemName>> itemNames = new HashedMap<>();
        itemNames.put("ObjectClassesConnectorStepPanel.schema",
                List.of(
                        ConnDevObjectClassInfoType.F_NATIVE_SCHEMA_SCRIPT, ConnDevObjectClassInfoType.F_CONNID_SCHEMA_SCRIPT));
        itemNames.put("ObjectClassesConnectorStepPanel.search",
                List.of(
                        ConnDevObjectClassInfoType.F_SEARCH_ALL_OPERATION,
                        ConnDevObjectClassInfoType.F_GET_OPERATION,
                        ConnDevObjectClassInfoType.F_SEARCH_FILTER_OPERATION));
        itemNames.put("ObjectClassesConnectorStepPanel.create", List.of(ConnDevObjectClassInfoType.F_CREATE_SCRIPT));
//        itemNames.put("ObjectClassesConnectorStepPanel.update", List.of(ConnDevObjectClassInfoType.F_SEARCH_UPDATE_OPERATION));
//        itemNames.put("ObjectClassesConnectorStepPanel.delete", List.of(ConnDevObjectClassInfoType.F_SEARCH_DELETE_OPERATION));
        return itemNames.entrySet().stream()
                .filter(itemNameEntry -> itemNameEntry.getValue().stream().anyMatch(
                        itemName -> ConnectorDevelopmentWizardUtil.existContainerValue(value, itemName)))
                .map(itemNameEntry -> {
                    PolyStringType label = PolyStringType.fromOrig(itemNameEntry.getKey());
                    label.setTranslation(new PolyStringTranslationType().key(itemNameEntry.getKey()));
                    return new DisplayType()
                            .label(label)
                            .cssClass("bg-default");
                }).toList();
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClasses");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClasses.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClasses.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public boolean isStatusStep() {
        return true;
    }

}
