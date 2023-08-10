/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPopupPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
public class CapabilitiesPanel extends BasePanel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperImpl.class);

    private static final String ID_CAPABILITIES = "capabilities";
    private static final String ID_CAPABILITY_BUTTON = "capabilityButton";
    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";

    private final ResourceDetailsModel resourceModel;
    private LoadableDetachableModel<ResourceType> resourceWithApplyDelta;
    private LoadableDetachableModel<ResourceObjectTypeDefinitionType> objectTypeWithApplyDelta;

    public CapabilitiesPanel(
            String id,
            ResourceDetailsModel resourceModel) {
        this(id, resourceModel, null);
    }

    public CapabilitiesPanel(
            String id,
            ResourceDetailsModel resourceModel,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
        super(id, model);
        this.resourceModel = resourceModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModelsWithApplyDeltas();
        initLayout();
    }

    private void initModelsWithApplyDeltas() {
        resourceWithApplyDelta = new LoadableDetachableModel<>() {
            @Override
            protected ResourceType load() {
                try {
                    return resourceModel.getObjectWrapper().getObjectApplyDelta().asObjectable();
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get resource with applied deltas", e);
                }
                return null;
            }
        };

        objectTypeWithApplyDelta = new LoadableDetachableModel<>() {
            @Override
            protected ResourceObjectTypeDefinitionType load() {
                if (getModelObject() != null) {
                    try {
                        return getModelObject().getContainerValueApplyDelta().asContainerable();
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't get object type with applied deltas", e);
                    }
                }
                return null;
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        IModel<List<PrismContainerWrapper<CapabilityType>>> containers = getContainers();

        ListView<PrismContainerWrapper<CapabilityType>> capabilities = new ListView<>(ID_CAPABILITIES, containers) {
            @Override
            protected void populateItem(ListItem<PrismContainerWrapper<CapabilityType>> item) {

                AjaxButton button = new AjaxButton(ID_CAPABILITY_BUTTON) {

                    private void refreshButton(AjaxRequestTarget target) {
                        target.add(this);
                    }
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (useEnabledAttribute(item)) {
                            try {
                                item.getModelObject().findProperty(CapabilityType.F_ENABLED)
                                        .getValues().iterator().next().setRealValue(!isCapabilityEnabled(item.getModelObject()));
                                refreshButton(target);
                            } catch (SchemaException e) {
                                LOGGER.error("Couldn't find property enabled in capability", e);
                            }
                        } else {
                            getPageBase().showMainPopup(new SingleContainerPopupPanel<>(
                                    getPageBase().getMainPopupBodyId(), item.getModel()) {
                                @Override
                                public IModel<String> getTitle() {
                                    return getLabelModel(item.getModelObject());
                                }

                                @Override
                                protected void onSubmitPerformed(AjaxRequestTarget target) {
                                    getPageBase().hideMainPopup(target);
                                    refreshButton(target);
                                }
                            }, target);
                        }
                        resourceWithApplyDelta.detach();
                        objectTypeWithApplyDelta.detach();
                    }
                };

                IModel<String> enabled = getActiveCss(item.getModel());
                button.add(AttributeAppender.append("class", enabled));
                WebComponent icon = new WebComponent(ID_ICON);
                icon.add(AttributeAppender.append("class", getIcon(item.getModelObject())));
                button.setOutputMarkupId(true);
                button.add(icon);
                button.add(new Label(ID_LABEL, getLabelModel(item.getModelObject())));

                item.add(button);
                item.setOutputMarkupId(true);
            }
        };
        capabilities.setOutputMarkupId(true);
        add(capabilities);
    }

    private boolean useEnabledAttribute(ListItem<PrismContainerWrapper<CapabilityType>> item) {
        if (item.getModelObject().getValues().iterator().next().getItems().size() == 2) {
            try {
                PrismPropertyWrapper<Object> manual = item.getModelObject().findProperty(AbstractWriteCapabilityType.F_MANUAL);
                return manual != null;
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find property manual in capability " + item.getModelObject(), e);
                return false;
            }
        }
        return item.getModelObject().getValues().iterator().next().getItems().size() == 1;
    }

    private IModel<List<PrismContainerWrapper<CapabilityType>>> getContainers() {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerWrapper<CapabilityType>> load() {
                PrismContainerValueWrapper<Containerable> capabilitiesContainer = null;
                try {
                    if (getModelObject() == null) {
                        capabilitiesContainer = resourceModel.getObjectWrapper().findContainer(
                                        ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CONFIGURED))
                                .getValues().iterator().next();

                    } else {
                        capabilitiesContainer = getModelObject().findContainer(ResourceObjectTypeDefinitionType.F_CONFIGURED_CAPABILITIES)
                                .getValues().iterator().next();
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find capabilities container", e);
                }
                List<PrismContainerWrapper<CapabilityType>> capabilities = new ArrayList<>();
                if (capabilitiesContainer != null) {
                    capabilitiesContainer.getContainers().stream()
                            .filter(container -> CapabilityType.class.isAssignableFrom(container.getTypeClass())
                                    && !SchemaCapabilityType.class.isAssignableFrom(container.getTypeClass()))
                            .forEach(container -> {
                                capabilities.add((PrismContainerWrapper<CapabilityType>) container);
                                List<PrismContainerWrapper<? extends Containerable>> childContainers =
                                        container.getValues().iterator().next().getContainers().stream()
                                                .filter(childContainer -> CapabilityType.class.isAssignableFrom(childContainer.getTypeClass()))
                                                .collect(Collectors.toList());

                                childContainers.forEach(childContainer ->
                                        capabilities.add((PrismContainerWrapper<CapabilityType>) childContainer));
                            });
                }

                return capabilities;
            }
        };
    }

    private IModel<String> getLabelModel(PrismContainerWrapper<CapabilityType> modelObject) {
        if (StringUtils.isEmpty(modelObject.getDisplayName())) {
            return Model.of(modelObject.getItemName().getLocalPart());
        }
        return getPageBase().createStringResource(modelObject.getDisplayName());
    }

    private String getIcon(PrismContainerWrapper<CapabilityType> capability) {
        if (CapabilityType.class.isAssignableFrom(capability.getParent().getParent().getTypeClass())) {
            return getIcon(capability.getParent().getParent().getTypeClass());
        }
        return getIcon(capability.getTypeClass());
    }

    private String getIcon(Class capability) {
        if (CredentialsCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-key";
        }
        if (ActivationCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-lock";
        }
        if (LiveSyncCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-rotate";
        }
        if (TestConnectionCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-tower-broadcast";
        }
        if (ScriptCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-code";
        }
        if (ReadCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-book";
        }
        if (CountObjectsCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-calculator";
        }
        if (PagedSearchCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-folder-tree";
        }
        if (RunAsCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-circle-play";
        }
        if (AuxiliaryObjectClassesCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-circle-dot";
        }
        if (UpdateCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-circle-arrow-up";
        }
        if (DeleteCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-circle-minus";
        }
        if (CreateCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-circle-plus";
        }
        if (DiscoverConfigurationCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-magnifying-glass";
        }
        if (AsyncUpdateCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-arrows-turn-to-dots";
        }
        if (SchemaCapabilityType.class.isAssignableFrom(capability)) {
            return "fa fa-table-cells";
        }
        return "fa fa-circle";
    }

    private IModel<String> getActiveCss(IModel<PrismContainerWrapper<CapabilityType>> model) {
        return () -> isCapabilityEnabled(model.getObject()) ? "bg-primary" : "";
    }

    private boolean isCapabilityEnabled(PrismContainerWrapper<CapabilityType> modelObject) {
        if (ActivationCapabilityType.F_STATUS.equivalent(modelObject.getItemName())) {
            return ResourceTypeUtil.isActivationStatusCapabilityEnabled(
                    resourceWithApplyDelta.getObject(),
                    objectTypeWithApplyDelta.getObject());
        }

        if (ActivationCapabilityType.F_LOCKOUT_STATUS.equivalent(modelObject.getItemName())) {
            return ResourceTypeUtil.isActivationLockoutStatusCapabilityEnabled(
                    resourceWithApplyDelta.getObject(),
                    objectTypeWithApplyDelta.getObject());
        }

        if (ActivationCapabilityType.F_VALID_FROM.equivalent(modelObject.getItemName())) {
            return ResourceTypeUtil.isActivationValidityFromCapabilityEnabled(
                    resourceWithApplyDelta.getObject(),
                    objectTypeWithApplyDelta.getObject());
        }

        if (ActivationCapabilityType.F_VALID_TO.equivalent(modelObject.getItemName())) {
            return ResourceTypeUtil.isActivationValidityToCapabilityEnabled(
                    resourceWithApplyDelta.getObject(),
                    objectTypeWithApplyDelta.getObject());
        }

        if (CredentialsCapabilityType.F_PASSWORD.equivalent(modelObject.getItemName())) {
            return ResourceTypeUtil.isPasswordCapabilityEnabled(
                    resourceWithApplyDelta.getObject(),
                    objectTypeWithApplyDelta.getObject());
        }

        return ResourceTypeUtil.getEnabledCapability(
                resourceWithApplyDelta.getObject(),
                objectTypeWithApplyDelta.getObject(),
                modelObject.getTypeClass()
        ) != null;
    }
}
