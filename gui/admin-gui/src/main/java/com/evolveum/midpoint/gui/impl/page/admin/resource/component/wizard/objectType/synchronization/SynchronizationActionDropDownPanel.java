/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.input.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DisplayableValueChoiceRenderer;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationActionsType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class SynchronizationActionDropDownPanel extends BasePanel<PrismContainerWrapper<SynchronizationActionsType>> {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationActionDropDownPanel.class);

    private final static String ID_PANEL = "panel";

    public SynchronizationActionDropDownPanel(String id, IModel<PrismContainerWrapper<SynchronizationActionsType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        IModel<ItemName> dropDownModel = new IModel<>() {
            @Override
            public ItemName getObject() {
                PrismContainerWrapper<SynchronizationActionsType> actions = getModelObject();
                try {
                    List<? extends ItemWrapper<?, ?>> items = actions.getValue().getItems();
                    if (items.size() == 1) {
                        for (ItemWrapper<?, ?> item : items) {
                            if (AbstractSynchronizationActionType.class.isAssignableFrom(item.getTypeClass())
                                    && !ValueStatus.DELETED.equals(item.getValues().iterator().next().getStatus())) {
                                return item.getItemName();
                            }
                        }
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value of synchronization actions", e);
                }
                return null;
            }

            @Override
            public void setObject(ItemName object) {
                PrismContainerWrapper<SynchronizationActionsType> actions = getModelObject();
                try {
                    List<ItemWrapper> itemsForRemoving = new ArrayList<>();
                    boolean found = false;
                    for (ItemWrapper<?, ?> item : actions.getValue().getItems()) {
                        if (!AbstractSynchronizationActionType.class.isAssignableFrom(item.getTypeClass())
                                || item.getValues().size() != 1) {
                            continue;
                        }
                        boolean match = item.getItemName().equivalent(object);
                        switch (item.getValues().iterator().next().getStatus()) {
                            case DELETED:
                                if (match) {
                                    item.getValues().iterator().next().setStatus(ValueStatus.NOT_CHANGED);
                                }
                                continue;
                            case ADDED:
                                if (match) {
                                    continue;
                                }
                                itemsForRemoving.add(item);
                                actions.getValue().getNewValue().remove(item.getItem());
                                break;
                            case NOT_CHANGED:
                                if (match) {
                                    continue;
                                }
                                item.getValues().iterator().next().setStatus(ValueStatus.DELETED);
                                break;
                        }
                        if (match) {
                            found = true;
                        }
                    }
                    actions.getValue().getItems().removeAll(itemsForRemoving);
                    actions.getValue().getContainers().removeAll(itemsForRemoving);

                    if (object == null) {
                        return;
                    }

                    if (!found) {
                        PrismContainer<Containerable> action = actions.getValue().getNewValue().findOrCreateContainer(object);
                        Task task = getPageBase().createSimpleTask("create_action_container");
                        WrapperContext ctx = new WrapperContext(task, task.getResult());
                        ctx.setCreateIfEmpty(true);
                        action.createNewValue();
                        ItemWrapper iw = getPageBase().createItemWrapper(action, actions.getValue(), ItemStatus.ADDED, ctx);
                        actions.getValue().addItem(iw);
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value of synchronization actions", e);
                }
            }
        };

        IModel<? extends List<DisplayableValue<ItemName>>> choicesModel = getChoices();

        DropDownChoicePanel panel = new DropDownChoicePanel(
                ID_PANEL,
                dropDownModel,
                choicesModel,
                new DisplayableValueChoiceRenderer(choicesModel.getObject()),
                true) {
            @Override
            protected IConverter<?> createConverter(Class<?> type) {
                return new AutoCompleteDisplayableValueConverter<>(choicesModel);
            }
        };
        panel.setOutputMarkupId(true);
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(panel);
    }

    private IModel<? extends List<DisplayableValue<ItemName>>> getChoices() {
        return () -> {
            List<DisplayableValue<ItemName>> containers = new ArrayList<>();
            PrismContainerWrapper<SynchronizationActionsType> actions = getModelObject();
            actions.getDefinitions().forEach(def -> containers.add(new ActionDisplayableValue(def)));
            return containers;
        };
    }

    private class ActionDisplayableValue implements DisplayableValue<ItemName>, Serializable {

        private final String displayName;
        private final String help;
        private final ItemName value;

        private ActionDisplayableValue(ItemDefinition itemDefinition) {
            this.displayName = itemDefinition.getDisplayName() == null ?
                    itemDefinition.getItemName().getLocalPart() : itemDefinition.getDisplayName();
            this.help = itemDefinition.getHelp();
            this.value = itemDefinition.getItemName();
        }

        @Override
        public ItemName getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return displayName;
        }

        @Override
        public String getDescription() {
            return help;
        }
    }

}
