/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.input.ContainersDropDownPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.internal.Enclosure;
import org.apache.wicket.markup.html.internal.InlineEnclosure;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
public class ActionStepPanel extends AbstractResourceWizardStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(ActionStepPanel.class);

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER = "header";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
    private static final String ID_ACTION_CONTAINER = "actionContainer";
    private static final String ID_ACTION_PANEL = "action";
    private static final String ID_PANEL = "panel";

    private boolean expanded = true;

    private final IModel<PrismContainerWrapper<SynchronizationActionsType>> parentValueModel;
    private LoadableDetachableModel<PrismContainerValueWrapper> valueModel;

    public ActionStepPanel(ResourceDetailsModel model,
            IModel<PrismContainerWrapper<SynchronizationActionsType>> parentValueModel) {
        super(model);
        this.parentValueModel = parentValueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initValueModel();
        initLayout();
    }

    private void initValueModel() {
        valueModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerValueWrapper load() {
                PrismContainerWrapper<SynchronizationActionsType> parentItem = parentValueModel.getObject();
                try {
                    List<PrismContainerWrapper<? extends Containerable>> containers = parentItem.getValue().getContainers()
                            .stream()
                            .filter(container -> !ValueStatus.DELETED.equals(container.getValues().iterator().next().getStatus()))
                            .collect(Collectors.toList());
                    if (containers.size() == 1) {
                        for (PrismContainerWrapper container : containers) {
                            if (AbstractSynchronizationActionType.class.isAssignableFrom(container.getItem().getDefinition().getTypeClass())) {
                                return (PrismContainerValueWrapper) container.getValues().iterator().next();
                            }
                        }
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value of synchronization actions", e);
                }
                return null;
            }
        };
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        container.add(createHeaderPanel());
        container.add(createActionPanel());
        container.add(createValuePanel());
    }

    private WebMarkupContainer createActionPanel() {
        WebMarkupContainer actionContainer = new WebMarkupContainer(ID_ACTION_CONTAINER);
        actionContainer.setOutputMarkupId(true);
        actionContainer.add(new VisibleBehaviour(() -> getValueModel().getObject() != null ? getValueModel().getObject().isExpanded() : expanded));

        ContainersDropDownPanel<SynchronizationActionsType> panel = new ContainersDropDownPanel(
                ID_ACTION_PANEL,
                parentValueModel) {
            @Override
            protected boolean validateChildContainer(ItemDefinition definition) {
                return AbstractSynchronizationActionType.class.isAssignableFrom(definition.getTypeClass());
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getValueModel().detach();
                if (getValueModel().getObject() != null && ValueStatus.ADDED.equals(getValueModel().getObject().getStatus())) {
                    getValueModel().getObject().setShowEmpty(true);
                    getValueModel().getObject().setExpanded(true);
                }
                refreshPanel(target);
            }
        };
        panel.setOutputMarkupId(true);

        actionContainer.add(panel);
        return actionContainer;
    }

    private Component createValuePanel() {
        ItemPanelSettingsBuilder settings = new ItemPanelSettingsBuilder();
        settings.headerVisibility(false);
        VerticalFormPrismContainerValuePanel panel = new VerticalFormPrismContainerValuePanel(ID_PANEL, getValueModel(), settings.build()) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                Component parent = get(
                        createComponentPath(
                                ID_VALUE_FORM,
                                ID_VALUE_CONTAINER,
                                ID_INPUT,
                                VerticalFormDefaultContainerablePanel.ID_PROPERTIES_LABEL,
                                VerticalFormDefaultContainerablePanel.ID_FORM_CONTAINER));
                if (parent != null) {
                    parent.add(AttributeAppender.replace("class", "pt-0 pb-3 px-3 mb-0"));
                }
            }
        };
        panel.setOutputMarkupId(true);
        panel.add(new VisibleBehaviour(() -> getValueModel().getObject() != null));
        return panel;
    }

    private LoadableDetachableModel<PrismContainerValueWrapper> getValueModel() {
        return valueModel;
    }

    private WebMarkupContainer createHeaderPanel() {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onHeaderClick(target);
            }
        });

        header.add(createExpandCollapseButton());
        header.setOutputMarkupId(true);

        return header;
    }

    private ToggleIconButton createExpandCollapseButton() {
        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

            @Override
            public boolean isOn() {
                return getValueModel().getObject() != null ? getValueModel().getObject().isExpanded() : expanded;
            }
        };
        expandCollapseButton.setOutputMarkupId(true);
        return expandCollapseButton;
    }

    private void refreshPanel(AjaxRequestTarget target) {
        target.add(get(ID_CONTAINER));
        target.add(get(createComponentPath(ID_CONTAINER, ID_HEADER)));
        VerticalFormPrismContainerValuePanel panel = getPanel();
        target.add(getPanel());
        panel.refreshPanel(target);
        target.add(get(createComponentPath(ID_CONTAINER, ID_ACTION_CONTAINER)));
        target.add(get(createComponentPath(ID_CONTAINER, ID_ACTION_CONTAINER, ID_ACTION_PANEL)));
    }

    private VerticalFormPrismContainerValuePanel getPanel() {
        return (VerticalFormPrismContainerValuePanel) get(createComponentPath(ID_CONTAINER, ID_PANEL));
    }

    private void onHeaderClick(AjaxRequestTarget target) {
        PrismContainerValueWrapper wrapper = getValueModel().getObject();
        if (wrapper == null) {
            expanded = !expanded;
        } else {
            wrapper.setExpanded(!wrapper.isExpanded());
        }
        refreshPanel(target);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.action");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.action.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.synchronization.reaction.action.subText");
    }
}
