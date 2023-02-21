/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectVisualizationPanel extends BasePanel<ObjectVisualization> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_CONTAINER = "container";

    private IModel<ContainerVisualization> defaultContainerModel;

    public ObjectVisualizationPanel(String id, IModel<ObjectVisualization> model) {
        super(id, model);

        initModels();
        initLayout();
    }

    private void initModels() {
        defaultContainerModel = new LoadableDetachableModel<>() {

            @Override
            protected ContainerVisualization load() {
                // todo not very good
                return new ContainerVisualization(getModelObject().getVisualization());
            }
        };
    }

    private void initLayout() {
        ContainerVisualizationPanel defaultContainer = new ContainerVisualizationPanel(ID_ITEMS, defaultContainerModel);
        defaultContainer.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(defaultContainer);

        ListView<ContainerVisualization> containers = new ListView<>(ID_CONTAINERS, () -> getModelObject().getContainers()) {

            @Override
            protected void populateItem(ListItem<ContainerVisualization> item) {
                item.add(new ContainerVisualizationPanel(ID_CONTAINER, item.getModel()));
            }
        };
        add(containers);
    }
}
