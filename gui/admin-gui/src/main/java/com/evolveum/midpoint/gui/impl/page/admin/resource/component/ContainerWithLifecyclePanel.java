/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.AjaxButton;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerWithLifecyclePanel<C extends Containerable> extends BasePanel<PrismContainerValueWrapper<C>> {

    private static final String ID_VALUE_COLLAPSE_BUTTON = "valueCollapseButton";
    private static final String ID_VALUE_ICON = "valueIcon";
    private static final String ID_VALUE_NAME = "valueName";
    private static final String ID_LIFECYCLE_INPUT = "lifecycleInput";
    private static final String ID_CONTAINERS_CONTAINER = "containersContainer";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_CONTAINER = "container";
    private static final String ID_CONTAINER_COLLAPSE_BUTTON = "containerCollapseButton";
    private static final String ID_CONTAINER_NAME = "containerName";
    private static final String ID_CHILD_CONTAINER = "childContainer";
    private static final String ID_CHILD = "child";

    private IModel<LifecycleContainerValueWrapper> wrapperModel;
    private final String parentContainerName;

    public ContainerWithLifecyclePanel(String id, IModel<PrismContainerValueWrapper<C>> model) {
        this(id, model, null);
    }

    ContainerWithLifecyclePanel(String id, IModel<PrismContainerValueWrapper<C>> model, String parentContainerName) {
        super(id, model);
        this.parentContainerName = parentContainerName;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initWrapperModel();
        initLayout();
    }

    private void initWrapperModel() {
        wrapperModel = new LoadableDetachableModel<>() {
            @Override
            protected LifecycleContainerValueWrapper load() {
                return new LifecycleContainerValueWrapper(getModelObject());
            }
        };
    }

    private void initLayout() {
        setOutputMarkupId(true);

        AjaxButton valueCollapseButton = new AjaxButton(ID_VALUE_COLLAPSE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getWrapperModelObject().value.setExpanded(!getWrapperModelObject().value.isExpanded());
                target.add(ContainerWithLifecyclePanel.this.get(ID_VALUE_COLLAPSE_BUTTON));
                target.add(ContainerWithLifecyclePanel.this.get(ID_CONTAINERS_CONTAINER));
                target.add(ContainerWithLifecyclePanel.this);
            }
        };
        valueCollapseButton.add(AttributeAppender.append(
                "class",
                () -> getWrapperModelObject().value.isExpanded() ? "fa-angle-down" : "fa-angle-right"));
        valueCollapseButton.setOutputMarkupId(true);
        valueCollapseButton.add(new VisibleBehaviour(() -> !getWrapperModelObject().containers.isEmpty()));
        add(valueCollapseButton);

        WebMarkupContainer valueIcon = new WebMarkupContainer(ID_VALUE_ICON);
        valueIcon.add(AttributeAppender.append(
                "class",
                () -> getWrapperModelObject().containers.isEmpty() ? "fa-regular fa-circle-dot" : "fa fa-folder-tree"));
        valueIcon.setOutputMarkupId(true);
        add(valueIcon);

        AjaxButton valueName = new AjaxButton(ID_VALUE_NAME, getValueNameModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getWrapperModelObject().value.setExpanded(!getWrapperModelObject().value.isExpanded());
                target.add(ContainerWithLifecyclePanel.this.get(ID_VALUE_COLLAPSE_BUTTON));
                target.add(ContainerWithLifecyclePanel.this.get(ID_CONTAINERS_CONTAINER));
                target.add(ContainerWithLifecyclePanel.this);
            }
        };
        valueName.setOutputMarkupId(true);
        valueName.add(AttributeAppender.append("class", getWrapperModelObject().containers.isEmpty() ? "": "text-bold"));
        add(valueName);

        PrismPropertyWrapperModel<C, String> lifecycleModel = PrismPropertyWrapperModel.fromContainerValueWrapper(getModel(), ObjectType.F_LIFECYCLE_STATE);
        LifecycleStatePanel lifecycleInput = new LifecycleStatePanel(ID_LIFECYCLE_INPUT, lifecycleModel);
        lifecycleInput.setOutputMarkupId(true);
        lifecycleInput.add(new VisibleBehaviour(() -> lifecycleModel.getObject() != null));
        add(lifecycleInput);

        WebMarkupContainer containerContainer = new WebMarkupContainer(ID_CONTAINERS_CONTAINER);
        containerContainer.setOutputMarkupId(true);
        containerContainer.add(new VisibleBehaviour(() ->
                getWrapperModelObject().value.isExpanded() && !getWrapperModelObject().containers.isEmpty()));
        add(containerContainer);

        ListView<Map.Entry<String, PrismContainerWrapper>> containers = new ListView<>(
                ID_CONTAINERS, () -> new ArrayList<>(getWrapperModelObject().containers.entrySet())) {
            @Override
            protected void populateItem(ListItem<Map.Entry<String, PrismContainerWrapper>> item) {
                WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
                container.setOutputMarkupId(true);
                container.add(new VisibleBehaviour(() -> item.getModelObject().getValue().getValues().size() > 1));
                item.add(container);

                AjaxButton containerCollapseButton = new AjaxButton(ID_CONTAINER_COLLAPSE_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        item.getModelObject().getValue().setExpanded(!item.getModelObject().getValue().isExpanded());
                        target.add(container.get(ID_CONTAINER_COLLAPSE_BUTTON));
                        target.add(item.get(ID_CHILD_CONTAINER));
                        target.add(ContainerWithLifecyclePanel.this);
                    }
                };
                containerCollapseButton.add(AttributeAppender.append(
                        "class",
                        () -> getWrapperModelObject().value.isExpanded() ? "fa-angle-down" : "fa-angle-right"));
                containerCollapseButton.add(new VisibleBehaviour(() -> !getWrapperModelObject().containers.isEmpty()));
                containerCollapseButton.setOutputMarkupId(true);
                container.add(containerCollapseButton);

                AjaxButton valueName = new AjaxButton(ID_CONTAINER_NAME, Model.of(item.getModelObject().getKey())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        item.getModelObject().getValue().setExpanded(!item.getModelObject().getValue().isExpanded());
                        target.add(container.get(ID_CONTAINER_COLLAPSE_BUTTON));
                        target.add(item.get(ID_CHILD_CONTAINER));
                        target.add(ContainerWithLifecyclePanel.this);
                    }
                };
                valueName.setOutputMarkupId(true);
                container.add(valueName);

                WebMarkupContainer childContainer = new WebMarkupContainer(ID_CHILD_CONTAINER);
                childContainer.setOutputMarkupId(true);
                childContainer.add(new VisibleBehaviour(() -> item.getModelObject().getValue().isExpanded()));
                childContainer.add(AttributeAppender.append(
                        "style",
                        item.getModelObject().getValue().getValues().size() > 1 ? "margin-left:50px" : "margin-left:25px"));
                item.add(childContainer);

                RepeatingView child = new RepeatingView(ID_CHILD);
                child.setOutputMarkupId(true);
                item.getModelObject().getValue().getValues()
                        .forEach(value -> child.add(
                                new ContainerWithLifecyclePanel<>(
                                        child.newChildId(), () -> (PrismContainerValueWrapper) value, item.getModelObject().getKey())));
                childContainer.add(child);
            }
        };
        containers.setOutputMarkupId(true);
        containerContainer.add(containers);

    }

    private LifecycleContainerValueWrapper getWrapperModelObject() {
        return wrapperModel.getObject();
    }

    private IModel<String> getValueNameModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                PrismContainerValueWrapper value = getWrapperModelObject().value;

                if (value instanceof PrismObjectValueWrapper<?> objectValue) {
                    return WebComponentUtil.getDisplayNameOrName(((PrismObjectWrapper) objectValue.getParent()).getObject());
                }

                String name = null;
                try {
                    Method method = null;
                    Method[] methods = GuiDisplayNameUtil.class.getMethods();
                    for (Method m : methods) {
                        if (m.getName().equals("getDisplayName")) {
                            Class<?>[] paramTypes = m.getParameterTypes();
                            if (paramTypes.length != 1) {
                                continue;
                            }

                            if (paramTypes[0].equals(value.getDefinition().getTypeClass())) {
                                method = m;
                                continue;
                            }

                            if (paramTypes[0].isAssignableFrom(value.getDefinition().getTypeClass())) {
                                if (method == null || method.getParameterTypes()[0].isAssignableFrom(paramTypes[0])) {method = m;}
                            }

                        }
                    }
                    if (method != null) {
                        name = (String) method.invoke(null, value.getRealValue());
                    }
                } catch (SecurityException | IllegalAccessException | InvocationTargetException e) {
                    //ignore it
                }

                if (name == null) {
                    @Nullable List<QName> naturalKeysNames = value.getDefinition().getNaturalKeyConstituents();
                    if (naturalKeysNames != null && !naturalKeysNames.isEmpty()) {
                        String localName = ((List<ItemWrapper>) value.getNonContainers()).stream()
                                .filter(item -> naturalKeysNames.stream().anyMatch(key -> QNameUtil.match(item.getItemName(), key)))
                                .map(item -> ((List<PrismValueWrapper>) item.getValues()).stream()
                                        .map(itemValue -> WebComponentUtil.getLabelForItemValue(itemValue, getPageBase()))
                                        .filter(StringUtils::isNotEmpty)
                                        .collect(Collectors.joining(", ")))
                                .filter(StringUtils::isNotEmpty)
                                .collect(Collectors.joining(" - "));
                        if (StringUtils.isNotEmpty(localName)) {
                            name = localName;
                        }
                    }
                }

                String containerName = parentContainerName;
                if (name != null) {
                    if (StringUtils.isNotEmpty(containerName)) {
                        if (containerName.contains(name)) {
                            name = containerName;
                        } else if (value.getParent().getValues().size() == 1) {
                            name = containerName + " - " + name;
                        }
                    }
                    return name;
                }

                if (StringUtils.isEmpty(containerName) || value.getParent().getValues().size() > 1) {
                    containerName = value.getParent().getDisplayName();
                }

                return containerName + (value.getNewValue().getId() != null ? " (" + value.getNewValue().getId() + ")" : "");
            }
        };
    }

    private class LifecycleContainerValueWrapper implements Serializable {

        private PrismContainerValueWrapper value;
        private Map<String, PrismContainerWrapper> containers = new HashMap<>();

        private LifecycleContainerValueWrapper(PrismContainerValueWrapper value) {
            this.value = value;
            this.value.setExpanded(true);
            searchContainersWithLifecycle(value, "", containers);
        }

        private void searchContainersWithLifecycle(
                PrismContainerValueWrapper value, String containerName, Map<String, PrismContainerWrapper> containers) {
            for (Object item : value.getItems()) {

                if (!(item instanceof PrismContainerWrapper)
                        || ((PrismContainerWrapper<?>) item).isMetadata()
                        || ((PrismContainerWrapper<?>) item).isOperational()
                        || ((PrismContainerWrapper<?>) item).isDeprecated()
                        || (((PrismContainerWrapper<?>) item).isExperimental() && !WebModelServiceUtils.isEnableExperimentalFeature(getPageBase()))) {
                    continue;
                }
                PrismContainerWrapper container = (PrismContainerWrapper) item;

                PrismPropertyDefinition<String> def = null;
                try {
                    def = container.getComplexTypeDefinition().findLocalItemDefinition(
                            ObjectType.F_LIFECYCLE_STATE, PrismPropertyDefinition.class, false);
                } catch (Exception e) {
                    //ignore exception
                }

                String localContainerName = "";
                if (StringUtils.isNotEmpty(containerName)) {
                    localContainerName = containerName + " - ";
                }
                localContainerName = localContainerName + container.getDisplayName();

                if (def != null && !container.getValues().isEmpty() && ItemStatus.NOT_CHANGED == container.getStatus()) {
                    containers.put(localContainerName, container);
                    continue;
                }

                String finalLocalContainerName = localContainerName;
                container.getValues().forEach(containerValue -> searchContainersWithLifecycle(
                        (PrismContainerValueWrapper) containerValue, finalLocalContainerName, containers));

            }
        }
    }
}
