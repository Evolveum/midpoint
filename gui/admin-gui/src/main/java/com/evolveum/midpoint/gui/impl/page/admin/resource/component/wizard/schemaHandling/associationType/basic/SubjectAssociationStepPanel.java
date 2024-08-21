package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismItemAccessDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import java.util.*;

@PanelType(name = "rw-association-subject")
@PanelInstance(identifier = "rw-association-subject",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageResource.wizard.step.associationType.subject", icon = "fa fa-list"),
        containerPath = "empty")
public class SubjectAssociationStepPanel extends ParticipantAssociationStepPanel {

    protected static final Trace LOGGER = TraceManager.getTrace(SubjectAssociationStepPanel.class);

    public static final String PANEL_TYPE = "rw-association-subject";

    public SubjectAssociationStepPanel(
            ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel) {
        super(model, valueModel);
    }

    @Override
    protected List<ResourceObjectTypeDefinition> getListOfSupportedObjectTypeDef() throws SchemaException, ConfigurationException {
        return getDetailsModel().getRefinedSchema().getObjectTypeDefinitions().stream()
                .filter(def -> !def.getReferenceAttributeDefinitions().isEmpty()
                        && def.getReferenceAttributeDefinitions().stream().anyMatch(PrismItemAccessDefinition::canRead))
                .toList();
    }

    @Override
    protected boolean isMandatory() {
        return true;
    }

    @Override
    protected ItemPath getPathForValueContainer() {
        return ShadowAssociationTypeDefinitionType.F_SUBJECT;
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String userFriendlyNameOfSelectedObject(String key) {
        return createStringResource("PageResource.wizard.step.associationType.subject").getString();
    }

    @Override
    protected String getNameOfParticipant() {
        return "subject";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.associationType.subject");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.subject.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.associationType.subject.subText");
    }

    protected void performSelectedObjects() {
        List<ParticipantObjectTypeWrapper> selectedNewItems = new ArrayList<>(getSelectedItemsModel().getObject());

        ItemPath containerPath = ItemPath.create(ShadowAssociationTypeDefinitionType.F_SUBJECT, ShadowAssociationTypeSubjectDefinitionType.F_OBJECT_TYPE);

        PrismContainerWrapper<ResourceObjectTypeIdentificationType> container;
        try {
            container = getValueModel().getObject().findContainer(containerPath);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find object type subcontainer " + containerPath + " container in " + getValueModel().getObject());
            return;
        }

        List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> valueForRemove = new ArrayList<>();
        container.getValues().forEach(value -> {
            boolean match = getSelectedItemsModel().getObject().stream()
                    .anyMatch(wrapper -> equalValueAndObjectTypeWrapper(value, wrapper));
            if (!match) {
                if (value.getStatus() == ValueStatus.ADDED) {
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
                container.remove(value, getPageBase());
            } catch (SchemaException e) {
                LOGGER.error("Couldn't remove deselected value " + value);
            }
        });

        selectedNewItems.forEach(wrapper -> {
            try {
                PrismContainerValue<ResourceObjectTypeIdentificationType> newValue = container.getItem().createNewValue();
                newValue.asContainerable().kind(wrapper.getKind()).intent(wrapper.getIntent());
                PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> valueWrapper = WebPrismUtil.createNewValueWrapper(
                        container, newValue, getPageBase(), getDetailsModel().createWrapperContext());
                container.getValues().add(valueWrapper);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create new value for ResourceObjectTypeIdentificationType container in " + container);
            }
        });
    }
}
