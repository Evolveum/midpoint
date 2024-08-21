package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@PanelType(name = "rw-association-object")
@PanelInstance(identifier = "rw-association-object",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.associationType.object", icon = "fa fa-list"),
        containerPath = "empty")
public class ObjectAssociationStepPanel extends ParticipantAssociationStepPanel {

    protected static final Trace LOGGER = TraceManager.getTrace(ObjectAssociationStepPanel.class);

    public static final String PANEL_TYPE = "rw-association-object";

    public ObjectAssociationStepPanel(
            ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel) {
        super(model, valueModel);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String userFriendlyNameOfSelectedObject(String key) {
        return createStringResource("PageResource.wizard.step.associationType.object").getString();
    }

    @Override
    protected String getNameOfParticipant() {
        return "object";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.associationType.object");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.object.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.object.subText");
    }

    @Override
    protected List<ResourceObjectTypeDefinition> getListOfSupportedObjectTypeDef() throws SchemaException, ConfigurationException {
        List<ResourceObjectTypeDefinition> values = new ArrayList<>();
        PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeOfSubject =
                getValueModel().getObject().findContainer(
                        ItemPath.create(
                                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                ShadowAssociationTypeSubjectDefinitionType.F_OBJECT_TYPE));

        if (objectTypeOfSubject == null || objectTypeOfSubject.getValues().isEmpty()) {
            return values;
        }

        ResourceObjectTypeIdentificationType bean = objectTypeOfSubject.getValues().iterator().next().getRealValue();
        CompleteResourceSchema schema = getDetailsModel().getRefinedSchema();
        @Nullable ResourceObjectTypeDefinition def = schema.getObjectTypeDefinition(ResourceObjectTypeIdentification.of(bean));
        if (def == null) {
            return values;
        }

        List<? extends ShadowReferenceAttributeDefinition> referenceAttributes = def.getReferenceAttributeDefinitions();
        if (referenceAttributes.isEmpty()) {
            return values;
        }

        referenceAttributes.forEach(referenceAttribute -> {
            List<ShadowRelationParticipantType> objectsDefs = ProvisioningObjectsUtil.getObjectsOfSubject(referenceAttribute);

            objectsDefs.forEach(associationParticipantType -> {
                ResourceObjectDefinition targetDef = associationParticipantType.getObjectDefinition();

                if (associationParticipantType.getTypeIdentification() != null) {
                    addObjectTypeDef(values, (ResourceObjectTypeDefinition) targetDef);
                } else {
                    schema.getObjectTypeDefinitions().stream()
                            .filter(objectTypeDef -> QNameUtil.match(objectTypeDef.getObjectClassName(), targetDef.getObjectClassName()))
                            .forEach(objectTypeDef -> addObjectTypeDef(values, objectTypeDef));
                }
            });
        });
        return values;
    }

    private void addObjectTypeDef(List<ResourceObjectTypeDefinition> values, ResourceObjectTypeDefinition objectTypeDef) {
        boolean match = values.stream()
                .anyMatch(value -> objectTypeDef.getKind() == value.getKind()
                        && Objects.equals(objectTypeDef.getIntent(), value.getIntent()));
        if (!match) {
            values.add(objectTypeDef);
        }
    }

    protected void performSelectedObjects() {
        List<ParticipantObjectTypeWrapper> selectedNewItems = new ArrayList<>(getSelectedItemsModel().getObject());

        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationContainer = getValueModel().getObject();
        PrismContainerWrapper<ShadowAssociationTypeObjectDefinitionType> objectParticipantContainer;
        try {
            objectParticipantContainer = associationContainer.findContainer(ShadowAssociationTypeDefinitionType.F_OBJECT);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find object container in " + associationContainer);
            return;
        }
        List<PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType>> objectValueForRemove = new ArrayList<>();
        for (PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType> objectValue : objectParticipantContainer.getValues()) {
            try {
                PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeContainer =
                        objectValue.findContainer(ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE);
                if (objectTypeContainer == null || objectTypeContainer.getValues().isEmpty()) {
                    continue;
                }
                List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> valueForRemove = new ArrayList<>();
                objectTypeContainer.getValues().forEach(value -> {
                    boolean match = getSelectedItemsModel().getObject().stream()
                            .anyMatch(wrapper -> equalValueAndObjectTypeWrapper(value, wrapper));

                    if (!match) {
                        if (ValueStatus.ADDED == value.getStatus()) {
                            valueForRemove.add(value);
                        } else {
                            value.setStatus(ValueStatus.DELETED);
                        }
                    } else {
                        selectedNewItems.removeIf(wrapper -> equalValueAndObjectTypeWrapper(value, wrapper));
                    }
                });

                valueForRemove.forEach(value -> {
                    try {
                        objectTypeContainer.remove(value, getPageBase());
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't remove deselected value " + value);
                    }
                });

                if (objectTypeContainer.getValues().isEmpty() || !objectTypeContainer.getValues().stream().anyMatch(value -> ValueStatus.DELETED != value.getStatus())) {
                    if (ValueStatus.ADDED == objectValue.getStatus()) {
                        objectValueForRemove.add(objectValue);
                    } else {
                        objectValue.setStatus(ValueStatus.DELETED);
                    }
                }

            } catch (SchemaException e) {
                LOGGER.error("Couldn't find object type subcontainer of " + getNameOfParticipant() + " objectParticipantContainer in " + objectValue);
            }
        }

        objectValueForRemove.forEach(objectValue -> {
            try {
                objectParticipantContainer.remove(objectValue, getPageBase());
            } catch (SchemaException e) {
                LOGGER.error("Couldn't remove deselected value " + objectValue);
            }
        });

        if (objectParticipantContainer.getValues().isEmpty()) {
            try {
                PrismContainerValue<ShadowAssociationTypeObjectDefinitionType> newValue = objectParticipantContainer.getItem().createNewValue();
                selectedNewItems.forEach(wrapper -> {
                    newValue.asContainerable().beginObjectType().kind(wrapper.getKind()).intent(wrapper.getIntent());
                });
                PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                        objectParticipantContainer, newValue, getPageBase(), getDetailsModel().createWrapperContext());
                objectParticipantContainer.getValues().add(valueWrapper);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create new value for ShadowAssociationTypeObjectDefinitionType in " + objectParticipantContainer);
            }
        } else {
            try {
                PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType> value = objectParticipantContainer.getValues().get(0);
                PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeContainer = value.findContainer(ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE);
                selectedNewItems.forEach(wrapper -> {
                    try {
                        PrismContainerValue<ResourceObjectTypeIdentificationType> newValue = objectTypeContainer.getItem().createNewValue();
                        newValue.asContainerable().kind(wrapper.getKind()).intent(wrapper.getIntent());
                        PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                                objectTypeContainer, newValue, getPageBase(), getDetailsModel().createWrapperContext());
                        objectTypeContainer.getValues().add(valueWrapper);
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create new value for ResourceObjectTypeIdentificationType in " + objectParticipantContainer, e);
                    }
                });
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find " + ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE.getLocalPart() + " container in " + objectParticipantContainer, e);
            }
        }
    }

    @Override
    protected ItemPath getPathForValueContainer() {
        return ShadowAssociationTypeDefinitionType.F_OBJECT;
    }
}
