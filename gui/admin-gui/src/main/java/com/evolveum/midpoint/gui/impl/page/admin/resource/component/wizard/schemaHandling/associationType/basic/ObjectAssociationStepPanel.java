package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
    protected ItemPath getPathForValueContainer() {
        return ItemPath.create(ShadowAssociationTypeDefinitionType.F_OBJECT, ShadowAssociationTypeSubjectDefinitionType.F_OBJECT_TYPE);
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

    protected List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> getInitValues() throws SchemaException {
        List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> values = new ArrayList<>();

        PrismContainerWrapper<ShadowAssociationTypeObjectDefinitionType> objectContainer =
                getValueModel().getObject().findContainer(ShadowAssociationTypeDefinitionType.F_OBJECT);
        if (objectContainer == null) {
            return values;
        }

        for (PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType> objectValue : objectContainer.getValues()) {
            PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeContainer =
                    objectValue.findContainer(ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE);
            if (objectTypeContainer != null) {
                values.addAll(objectTypeContainer.getValues());
            }
        }
        return values;
    }

    @Override
    protected List<ResourceObjectTypeDefinition> getListOfSupportedObjectTypeDef() throws SchemaException, ConfigurationException {
        List<ResourceObjectTypeDefinition> values = new ArrayList<>();
        PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeOfSubject =
                getValueModel().getObject().findContainer(ItemPath.create(
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
        if (!match){
            values.add(objectTypeDef);
        }
    }

    protected void performSelectedObjects() {
        List<ObjectTypeWrapper> selectedNewItems = new ArrayList<>(getSelectedItemsModel().getObject());

        PrismContainerWrapper<ShadowAssociationTypeObjectDefinitionType> objectParticipantContainer;
        try {
            objectParticipantContainer = getValueModel().getObject().findContainer(ShadowAssociationTypeDefinitionType.F_OBJECT);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find object container in " + getValueModel().getObject());
            return;
        }
        for (PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType> objectValue : objectParticipantContainer.getValues()) {
            PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> value;
            try {
                PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectTypeContainer =
                        objectValue.findContainer(ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE);
                if (objectTypeContainer == null || objectTypeContainer.getValues().isEmpty()) {
                    continue;
                }
                value = objectTypeContainer.getValues().iterator().next();
            } catch (SchemaException e) {
            LOGGER.error("Couldn't find object type subcontainer of " + getNameOfParticipant() + " objectParticipantContainer in " + objectValue);
                continue;
            }
            boolean match = getSelectedItemsModel().getObject().stream()
                    .anyMatch(wrapper -> equalValueAndObjectTypeWrapper(value, wrapper));
            if (!match) {
                try {
                    objectParticipantContainer.remove(objectValue, getPageBase());
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't remove deselected value " + objectValue);
                }
            } else {
                selectedNewItems.removeIf(wrapper -> equalValueAndObjectTypeWrapper(value, wrapper));
            }
        }
        selectedNewItems.forEach(wrapper -> {
            try {
                PrismContainerValue<ShadowAssociationTypeObjectDefinitionType> newValue = objectParticipantContainer.getItem().createNewValue();
                newValue.asContainerable().beginObjectType().kind(wrapper.getKind()).intent(wrapper.getIntent());
                PrismContainerValueWrapper<ShadowAssociationTypeObjectDefinitionType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                        objectParticipantContainer, newValue, getPageBase(), getDetailsModel().createWrapperContext());
                objectParticipantContainer.getValues().add(valueWrapper);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create new value for ShadowAssociationTypeObjectDefinitionType in " + objectParticipantContainer);
            }
        });
    }
}
