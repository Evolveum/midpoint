/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
public class ContainersDropDownPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(ContainersDropDownPanel.class);

    private final static String ID_PANEL = "panel";

    private IModel<ItemName> dropDownModel;

    public ContainersDropDownPanel(String id, IModel<PrismContainerWrapper<C>> model) {
        super(id, model);
        initDropDownModel();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initDropDownModel() {
        this.dropDownModel = new IModel<>() {
            @Override
            public ItemName getObject() {
                PrismContainerWrapper<C> parentItem = getModelObject();
                try {
                    List<PrismContainerWrapper<? extends Containerable>> containers = parentItem.getValue().getContainers()
                            .stream()
                            .filter(container -> !ValueStatus.DELETED.equals(container.getValues().iterator().next().getStatus()))
                            .collect(Collectors.toList());
                    if (containers.size() == 1) {
                        for (ItemWrapper<?, ?> container : containers) {
                            if (validateChildContainer(container.getItem().getDefinition())) {
                                return container.getItemName();
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
                PrismContainerWrapper<C> parentItem = getModelObject();
                try {
                    List<ItemWrapper> itemsForRemoving = new ArrayList<>();
                    boolean found = false;
                    for (ItemWrapper<?, ?> item : parentItem.getValue().getContainers()) {
                        if (!validateChildContainer(item.getItem().getDefinition())
                                || item.getValues().size() != 1) {
                            continue;
                        }
                        boolean match = item.getItemName().equivalent(object);
                        switch (item.getValues().iterator().next().getStatus()) {
                            case DELETED:
                                if (match) {
                                    found = true;
                                    item.getValues().iterator().next().setStatus(ValueStatus.NOT_CHANGED);
                                }
                                continue;
                            case ADDED:
                                if (match) {
                                    found = true;
                                    continue;
                                }
                                itemsForRemoving.add(item);
                                parentItem.getValue().getNewValue().remove(item.getItem());
                                break;
                            case NOT_CHANGED:
                                if (match) {
                                    found = true;
                                    continue;
                                }
                                item.getValues().iterator().next().setStatus(ValueStatus.DELETED);
                                break;
                        }
                        if (match) {
                            found = true;
                        }
                    }
                    parentItem.getValue().getItems().removeAll(itemsForRemoving);
                    parentItem.getValue().getContainers().removeAll(itemsForRemoving);

                    if (object == null) {
                        return;
                    }

                    if (!found) {
                        PrismContainer<Containerable> container = parentItem.getValue().getNewValue().findOrCreateContainer(object);
                        Task task = getPageBase().createSimpleTask("create_action_container");
                        WrapperContext ctx = new WrapperContext(task, task.getResult());
                        ctx.setCreateIfEmpty(true);
                        ItemWrapper iw = getPageBase().createItemWrapper(container, parentItem.getValue(), ItemStatus.ADDED, ctx);
                        if (iw.isMultiValue()) {
                            PrismValueWrapper value = getPageBase().createValueWrapper(iw, container.createNewValue(), ValueStatus.ADDED, ctx);
                            iw.getValues().add(value);
                        }
                        parentItem.getValue().addItem(iw);
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value of synchronization actions", e);
                }
            }
        };
    }

    public IModel<ItemName> getDropDownModel() {
        return dropDownModel;
    }

    private void initLayout() {

        IModel<? extends List<DisplayableValue<ItemName>>> choicesModel = getChoices();

        DropDownChoicePanel panel = new DropDownChoicePanel(
                ID_PANEL,
                getDropDownModel(),
                choicesModel,
                new DisplayableValueChoiceRenderer(choicesModel.getObject()),
                true) {
            @Override
            protected IConverter<?> createConverter(Class<?> type) {
                return new AutoCompleteDisplayableValueConverter<>(choicesModel);
            }

            @Override
            protected String getNullValidDisplayValue() {
                return ContainersDropDownPanel.this.getNullValidDisplayValue();
            }
        };
        panel.setOutputMarkupId(true);
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        panel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change"){
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ContainersDropDownPanel.this.onUpdate(target);
            }
        });
        add(panel);
    }

    protected void onUpdate(AjaxRequestTarget target) {
    }

    protected String getNullValidDisplayValue() {
        return getString("DropDownChoicePanel.notDefined");
    }

    protected boolean validateChildContainer(ItemDefinition definition) {
        return true;
    }

    private IModel<? extends List<DisplayableValue<ItemName>>> getChoices() {
        return () -> {
            List<DisplayableValue<ItemName>> containers = new ArrayList<>();
            PrismContainerWrapper<C> parentItem = getModelObject();
            parentItem.getDefinitions().stream()
                    .filter(def -> ((def instanceof PrismContainerDefinition) && validateChildContainer(def)))
                    .forEach(def -> containers.add(new ContainerDisplayableValue(def)));
            return containers;
        };
    }

    private class ContainerDisplayableValue implements DisplayableValue<ItemName>, Serializable {

        private final String displayName;
        private final String help;
        private final ItemName value;

        private ContainerDisplayableValue(ItemDefinition itemDefinition) {
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
