package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.MultiSelectContainerTileWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class ParticipantAssociationStepPanel
        extends MultiSelectContainerTileWizardStepPanel<ParticipantObjectTypeWrapper, ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    protected static final Trace LOGGER = TraceManager.getTrace(ParticipantAssociationStepPanel.class);

    private final IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel;
    private final IModel<List<ParticipantObjectTypeWrapper>> selectedItems = Model.ofList(new ArrayList<>());

    public ParticipantAssociationStepPanel(
            ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel) {
        super(model);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSelectedItemsModel();
        add(AttributeAppender.append("class", "col-12"));
    }

    protected final IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    @Override
    protected abstract ItemPath getPathForValueContainer();

    private void initSelectedItemsModel() {
        try {
            List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> values = getInitValues();
            for (PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> resourceObjectTypeIdWrapper : values) {

                if (resourceObjectTypeIdWrapper.getStatus() == ValueStatus.DELETED) {
                    continue;
                }

                ResourceObjectTypeIdentificationType idBean = resourceObjectTypeIdWrapper.getRealValue();
                boolean match = selectedItems.getObject().stream()
                        .anyMatch(selectedItems -> equalValueAndObjectTypeWrapper(resourceObjectTypeIdWrapper, selectedItems));
                if (match) {
                    continue;
                }

                @Nullable ResourceObjectTypeDefinition objectTypeDef = getDetailsModel().getRefinedSchema().getObjectTypeDefinition(
                        ResourceObjectTypeIdentification.of(idBean));
                if (objectTypeDef == null) {
                    continue;
                }

                @NotNull ResourceObjectTypeDefinitionType objectType = objectTypeDef.getDefinitionBean().clone();
                objectType.setIntent(objectTypeDef.getIntent());
                QName objectClass = getObjectClass(objectType);
                ParticipantObjectTypeWrapper wrapper = new ParticipantObjectTypeWrapper(
                        objectType.getKind(),
                        objectType.getIntent(),
                        GuiDisplayNameUtil.getDisplayName(objectType),
                        objectClass);
                selectedItems.getObject().add(wrapper);
            }
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Couldn't find object type subcontainer of " + getNameOfParticipant() + " container in " + getDetailsModel().getObjectWrapper());
        }
    }

    protected List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> getInitValues() throws SchemaException {
        ItemPath containerPath = getPathForValueContainer();
        PrismContainerWrapper<ShadowAssociationTypeParticipantDefinitionType> container = getValueModel().getObject().findContainer(containerPath);
        if (container == null) {
            return Collections.emptyList();
        }
        List<PrismContainerValueWrapper<ResourceObjectTypeIdentificationType>> list = new ArrayList<>();
        container.getValues().forEach(value -> {
            try {
                PrismContainerWrapper<ResourceObjectTypeIdentificationType> subContainer =
                        value.findContainer(ShadowAssociationTypeParticipantDefinitionType.F_OBJECT_TYPE);
                list.addAll(subContainer.getValues());
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find sub container " + ShadowAssociationTypeParticipantDefinitionType.F_OBJECT_TYPE + " in " + container);
            }
        });
        return list;
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-11";
    }

    @Override
    protected IModel<List<ParticipantObjectTypeWrapper>> getSelectedItemsModel() {
        return selectedItems;
    }

    @Override
    protected IModel<String> getItemLabelModel(ParticipantObjectTypeWrapper wrapper) {
        return Model.of(wrapper.getDisplayName());
    }

    @Override
    protected void deselectItem(ParticipantObjectTypeWrapper removedWrapper) {
        removeSelectedItem(removedWrapper);
    }

    private void removeSelectedItem(ParticipantObjectTypeWrapper removedWrapper) {
        selectedItems.getObject().removeIf(wrapper -> wrapper.equals(removedWrapper));
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createTileObject(
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> object) {
        ResourceObjectTypeDefinitionType objectTypeBean = object.getRealValue();
        String icon = WebComponentUtil.createIconForResourceObjectType(objectTypeBean);
        String title = GuiDisplayNameUtil.getDisplayName(objectTypeBean);
        TemplateTile<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> tile
                = new TemplateTile<>(icon, title, object);
        tile.setDescription(objectTypeBean.getDescription());
        QName objectClass = getObjectClass(objectTypeBean);
        if (objectClass != null) {
            tile.addTag(new DisplayType().label(objectClass.getLocalPart()));
        }

        try {
            CompleteResourceSchema resourceSchema = getDetailsModel().getRefinedSchema();
            @Nullable ResourceObjectTypeDefinition objectTypeDef =
                    resourceSchema.getObjectTypeDefinition(ResourceObjectTypeIdentification.of(objectTypeBean));

            boolean match = selectedItems.getObject().stream()
                    .anyMatch(selectedItem -> selectedItem.getKind() == objectTypeDef.getKind()
                            && Objects.equals(selectedItem.getIntent(), objectTypeDef.getIntent()));
            tile.setSelected(match);

        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Couldn't load resource schema");
        }

        return tile;
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>>> createValuesModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> load() {
                IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> wrapperModel = PrismContainerWrapperModel.fromContainerWrapper(
                        getDetailsModel().getObjectWrapperModel(),
                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
                return wrapperModel.getObject().getValues();
            }
        };
    }

    @Override
    protected void processSelectOrDeselectItem(PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> value, MultivalueContainerListDataProvider<ResourceObjectTypeDefinitionType> provider, AjaxRequestTarget target) {
        getTable().refresh(target);
        refreshSubmitAndNextButton(target);

        ResourceObjectTypeDefinitionType objectType = value.getRealValue();
        QName objectClass = getObjectClass(objectType);

        try {
            CompleteResourceSchema resourceSchema = getDetailsModel().getRefinedSchema();
            @Nullable ResourceObjectTypeDefinition objectTypeDef =
                    resourceSchema.getObjectTypeDefinition(ResourceObjectTypeIdentification.of(objectType));

            ParticipantObjectTypeWrapper wrapper = new ParticipantObjectTypeWrapper(
                    objectTypeDef.getKind(),
                    objectTypeDef.getIntent(),
                    GuiDisplayNameUtil.getDisplayName(objectType),
                    objectClass);

            if (value.isSelected()) {
                selectedItems.getObject().add(wrapper);
            } else {
                removeSelectedItem(wrapper);
            }

        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Couldn't load resource schema");
        }

        getTable().getTilesModel().detach();
    }

    private QName getObjectClass(ResourceObjectTypeDefinitionType objectType) {
        return objectType.getDelineation() == null || objectType.getDelineation().getObjectClass() == null ? objectType.getObjectClass() : objectType.getDelineation().getObjectClass();
    }

    @Override
    protected ObjectQuery getCustomQuery() {
        S_FilterExit orFilter = PrismContext.get().queryFor(ResourceObjectTypeDefinitionType.class);
        try {
            List<ResourceObjectTypeDefinition> objectTypes = getListOfSupportedObjectTypeDef();
            objectTypes = objectTypes.stream()
                    .filter(objectType -> {
                        if (!selectedItems.getObject().isEmpty()){
                            QName objectClass = selectedItems.getObject().get(0).getObjectClass();
                            if (!QNameUtil.match(objectType.getObjectClassName(), objectClass)) {
                                return false;
                            }
                        }
                        return true;
                    }).toList();
            for (ResourceObjectTypeDefinition objectType : objectTypes) {
                orFilter = orFilter.or()
                        .block().item(ResourceObjectTypeDefinitionType.F_KIND).eq(objectType.getKind());

                        if (objectType.isDefaultForKind()) {
                            orFilter = orFilter.and().item(ResourceObjectTypeDefinitionType.F_DEFAULT).eq(true);
                        } else {
                            orFilter = orFilter.and().item(ResourceObjectTypeDefinitionType.F_INTENT).eq(objectType.getIntent());
                        }
                orFilter = orFilter.endBlock();
            }
            if(objectTypes.isEmpty()) {
                return PrismContext.get().queryFor(ResourceObjectTypeDefinitionType.class).none().build();
            }
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Couldn't create query for ResourceObjectTypeDefinitionType " + e.getMessage());
            return PrismContext.get().queryFor(ResourceObjectTypeDefinitionType.class).none().build();
        }

        return orFilter.build();
    }

    protected abstract List<ResourceObjectTypeDefinition> getListOfSupportedObjectTypeDef() throws SchemaException, ConfigurationException;

    protected abstract String getNameOfParticipant();

    protected boolean equalValueAndObjectTypeWrapper(
            PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> value, ParticipantObjectTypeWrapper wrapper) {
        ResourceObjectTypeIdentificationType objectTypeIdentifier = value.getRealValue();
        return wrapper.equals(objectTypeIdentifier.getKind(), objectTypeIdentifier.getIntent());
    }

    @Override
    protected Class<ResourceObjectTypeDefinitionType> getType() {
        return ResourceObjectTypeDefinitionType.class;
    }

    protected String getIcon() {
        return "fa fa-list";
    }

    @Override
    protected VisibleEnableBehaviour getHeaderFragmentVisibility() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
