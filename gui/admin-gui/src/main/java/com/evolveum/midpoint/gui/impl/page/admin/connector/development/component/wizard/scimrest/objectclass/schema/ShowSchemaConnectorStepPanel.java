/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.ComplexTypeDefinitionPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.DefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismContainerDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismItemDefinitionType;

import org.apache.commons.lang3.Strings;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-show-schema")
@PanelInstance(identifier = "cdw-show-schema",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.showSchema", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ShowSchemaConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-show-schema";

    private static final String ID_PANEL = "panel";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;

    private LoadableModel<String> resourceOidModel;

    public ShowSchemaConnectorStepPanel(WizardPanelHelper<? extends Containerable,
            ConnectorDevelopmentDetailsModel> helper, IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        ResourceDetailsModel resourceDetailsModel;

        try {
            PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));

            ObjectDetailsModels objectDetailsModel = resource.getValue().getNewObjectModel(
                    getContainerConfiguration(PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));
            resourceDetailsModel = (ResourceDetailsModel) objectDetailsModel;

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        ComplexTypeDefinitionPanel<ResourceType, ResourceDetailsModel> view = new ComplexTypeDefinitionPanel<>(
                ID_PANEL,
                resourceDetailsModel,
                getContainerConfiguration(PANEL_TYPE),
                ResourceType.F_SCHEMA){

            @Override
            protected boolean isVisibleNewItemDefinitionOrEnumValueButton() {
                return false;
            }

            @Override
            protected boolean isNewDefinitionButtonVisible() {
                return false;
            }

            @Override
            protected List<PrismContainerValueWrapper<? extends DefinitionType>> createListOfItem(IModel<String> searchItemModel) {
                List<PrismContainerValueWrapper<? extends DefinitionType>> list = new ArrayList<>();
                List<PrismContainerValueWrapper<? extends DefinitionType>> values = super.createListOfItem(searchItemModel);
                Optional<PrismContainerValueWrapper<? extends DefinitionType>> configuredObjectClass = values.stream()
                        .filter(value -> Strings.CS.equals(
                                value.getRealValue().getName().getLocalPart(), valueModel.getObject().getRealValue().getName()))
                        .findFirst();

                if (configuredObjectClass.isEmpty()) {
                    return list;
                }
                list.add(configuredObjectClass.get());

                List<String> embeddedObjectClasses = new ArrayList<>(getEmbeddedClasses(
                        ((ComplexTypeDefinitionType) configuredObjectClass.get().getRealValue()).getItemDefinitions()));

                while (!embeddedObjectClasses.isEmpty()) {
                    List<String> localEmbeddedClasses = new ArrayList<>();
                    values.stream()
                            .filter(value -> embeddedObjectClasses.contains(
                                    value.getRealValue().getName().getLocalPart()))
                            .forEach(value -> {
                                list.add(value);
                                localEmbeddedClasses.addAll(getEmbeddedClasses(
                                        ((ComplexTypeDefinitionType)value.getRealValue()).getItemDefinitions()));
                            });
                    embeddedObjectClasses.clear();
                    embeddedObjectClasses.addAll(localEmbeddedClasses);
                }

                return list;
            }
        };
        view.setOutputMarkupId(true);
        add(view);
    }

    private List<String> getEmbeddedClasses(List<PrismItemDefinitionType> itemDefinitions) {
        return itemDefinitions.stream()
                .filter(itemDefinition -> itemDefinition instanceof PrismContainerDefinitionType)
                .map(itemDefinition -> itemDefinition.getType().getLocalPart())
                .toList();
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSchema");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSchema.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSchema.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("ShowSchemaConnectorStepPanel.submit");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        onNextPerformed(target);
    }
}
