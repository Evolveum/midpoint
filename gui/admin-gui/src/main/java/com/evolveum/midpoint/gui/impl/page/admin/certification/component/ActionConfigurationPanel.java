/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.Collection;
import java.util.List;

/**
 * Abstract panel for configuration of actions.
 * Provides the generated panel for the specified in the configuration containers.
 * Supposed to be displayed in the popup
 */
public class ActionConfigurationPanel extends BasePanel<ContainerPanelConfigurationType> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ActionConfigurationPanel.class);

    private static final String ID_MESSAGE = "message";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_BUTTONS = "buttons";
    protected static final String ID_CONFIRM_BUTTON = "confirmButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_FEEDBACK = "feedback";

    LoadableModel<PrismContainerValueWrapper<Containerable>> model;

    public ActionConfigurationPanel(String id, IModel<ContainerPanelConfigurationType> configurationModel) {
        super(id, configurationModel);

        this.model = new LoadableModel<>(false) {
            @Override
            protected PrismContainerValueWrapper<Containerable> load() {
                return loadWrapper();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Label label = new Label(ID_MESSAGE, getMessageModel());
        label.setEscapeModelStrings(true);
        add(label);

        add(createGeneratedPanel());

        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        add(feedback);
    }

    protected IModel<String> getMessageModel() {
        return () -> {
            DisplayType display = getModelObject().getDisplay();
            if (display == null) {
                return null;
            }
            return LocalizationUtil.translatePolyString(display.getLabel());
        };
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    @NotNull
    public Component getFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxSubmitButton confirmButton = new AjaxSubmitButton(ID_CONFIRM_BUTTON, createConfirmButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                confirmPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

        };
        footer.add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createCancelButtonLabel()) {
            @Serial private static final long serialVersionUID = 1L;


            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        footer.add(cancelButton);

        return footer;
    }

    protected IModel<String> createConfirmButtonLabel() {
        return () -> "OK";
    }

    protected IModel<String> createCancelButtonLabel() {
        return () -> "Cancel";
    }

    private void confirmPerformed(AjaxRequestTarget target) {
        if (!isValidated(target)) {
            return;
        }
        Collection<ItemDelta<?, ?>> computedDeltas = computedDeltas();
        confirmPerformedWithDeltas(target, computedDeltas);
    }

    protected boolean isValidated(AjaxRequestTarget target) {
        return true;
    }

    public Component getFeedbackPanel() {
        return get(ID_FEEDBACK);
    }

    protected Collection<ItemDelta<?, ?>> computedDeltas() {
        try {
            PrismContainerValueWrapper<Containerable> iw = model.getObject();
            return iw.getDeltas();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    protected void confirmPerformedWithDeltas(AjaxRequestTarget target, Collection<ItemDelta<?, ?>> deltas) {
        // to be overridden
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        // to be overridden ?
        getPageBase().hideMainPopup(target);
    }

    protected Component createGeneratedPanel() {
        ContainerPanelConfigurationType configuration = getModelObject();
        List<VirtualContainersSpecificationType> containers = configuration.getContainer();

        ListView<VirtualContainersSpecificationType> items = new ListView<>(ID_ITEMS, () -> containers) {
            @Override
            protected void populateItem(ListItem<VirtualContainersSpecificationType> item) {

                IModel<PrismContainerWrapper<Containerable>> virtualContainerModel = createVirtualContainerModel(model, item.getModelObject());

                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler((wrapper) -> ItemVisibility.AUTO)
                        .mandatoryHandler((wrapper) -> isMandatoryItem(item.getModelObject(), wrapper))
                        .build();

                VerticalFormPrismContainerPanel<Containerable> panel = new VerticalFormPrismContainerPanel<>(ID_ITEM, virtualContainerModel, settings);
                panel.setOutputMarkupId(true);
                item.add(panel);
            }
        };
        items.setReuseItems(true);
        items.setOutputMarkupId(true);
        return items;
    }

    protected IModel<PrismContainerWrapper<Containerable>> createVirtualContainerModel(IModel<PrismContainerValueWrapper<Containerable>> model, VirtualContainersSpecificationType virtualContainer) {
        if (virtualContainer.getPath() != null) {
            return createContainerModel(model, virtualContainer.getPath().getItemPath());
        }
        if (StringUtils.isBlank(virtualContainer.getIdentifier())) {
            getSession().error(getString("SingleContainerPanel.empty.identifier", virtualContainer));
            return null;
        }
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, virtualContainer.getIdentifier());
    }

    //TODO copied from single container model
    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel(IModel<PrismContainerValueWrapper<Containerable>> model, ItemPath path) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(model, path);
    }

    private PrismContainerValueWrapper<Containerable> loadWrapper() {
        Task task = getPageBase().createSimpleTask("createWrapper");
        WrapperContext ctx = new WrapperContext(task, task.getResult());
        ctx.setCreateIfEmpty(true);
        ctx.setShowEmpty(true);
        ctx.forceCreateVirtualContainer(getModelObject().getContainer());

        PrismContainerValueWrapper<Containerable> wrapper;
        try {
            PrismContainerDefinition<Containerable> containerDef = getContainerDefinition();
            PrismContainerWrapperFactory<Containerable> cwf = getPageBase().getRegistry().findContainerWrapperFactory(containerDef);
            PrismContainerValue<Containerable> containerValue = instantiateContainerValue();
            containerValue.setParent(null); //todo is this correct? there is a problem while delta collection if parent != null
            wrapper = cwf.createValueWrapper(null, containerValue, ValueStatus.ADDED, ctx);
        } catch (SchemaException e) {
            throw new RuntimeException(e);//todo proper handling

        }
        return wrapper;
    }

    private boolean isMandatoryItem(VirtualContainersSpecificationType container, ItemWrapper<?, ?> wrapper) {
        VirtualContainerItemSpecificationType virtItem = container.getItem()
                .stream()
                .filter(item -> item.getPath() != null && wrapper.getPath().equivalent(item.getPath().getItemPath()))
                .findFirst()
                .orElse(null);
        if (virtItem == null) {
            return false;
        }
        return Boolean.TRUE.equals(virtItem.isMandatory());
    }

    private PrismContainerValue<Containerable> instantiateContainerValue() {
        PrismContainerDefinition<Containerable> containerDef = getContainerDefinition();
        PrismContainerValue<Containerable> containerValue = null;
        try {
            containerValue = containerDef.instantiate().createNewValue();
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Could not initialize container value", e);
        }
        return containerValue;
    }

    private PrismContainerDefinition<Containerable> getContainerDefinition() {
        return getPageBase().getPrismContext().getSchemaRegistry().findContainerDefinitionByType(getActionObjectType());
    }

    private QName getActionObjectType() {
        return getModelObject().getType();
    }

    @Override
    public int getWidth() {
        return 350;
    }

    @Override
    public int getHeight() {
        return 150;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("ActionConfigurationPanel.title");
    }

}
