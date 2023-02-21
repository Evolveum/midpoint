/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationItemsPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ContainerVisualizationPanel extends CardOutlineLeftPanel<ContainerVisualization> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEMS = "items";
    private static final String FRAGMENT_ID_HEADER = "header";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_LINK = "link";

    private IModel<Boolean> expanded = Model.of(false);

    public ContainerVisualizationPanel(String id, IModel<ContainerVisualization> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> VisualizationGuiUtil.createChangeTypeCssClassForOutlineCard(getModelObject().getChangeType())));

        VisualizationItemsPanel items = new VisualizationItemsPanel(ID_ITEMS, () -> new VisualizationDto(getModelObject().getVisualization()));
        items.add(new VisibleBehaviour(() -> expanded.getObject()));
        add(items);
    }

    @Override
    protected Component createBodyHeader(String id) {
        setOutputMarkupId(true);

        Fragment fragment = new Fragment(id, FRAGMENT_ID_HEADER, this);
        fragment.add(AttributeAppender.append("class", () -> {
            String css = expanded.getObject() ? " border-bottom border-gray pb-3 mb-3" : "";
            return "d-flex gap-2 align-items-center" + css;
        }));

        WebComponent icon = new WebComponent(ID_ICON);
        icon.add(AttributeAppender.append("class", "fa-solid fa-user"));    // todo fix
        fragment.add(icon);

        // todo make sure to handle escaping inside model
        Label title = new Label(ID_TITLE, () -> {
            Name name = getModelObject().getVisualization().getName();
            if (name == null || name.getSimpleDescription() == null) {
                return name.getDisplayName();
            }
            return LocalizationUtil.translateMessage(getModelObject().getVisualization().getName().getSimpleDescription());
        });// todo fix createTitleModel()));
        title.setEscapeModelStrings(false);
        fragment.add(title);

        AjaxIconButton link = new AjaxIconButton(ID_LINK,
                () -> {
                    final String prefix = "mr-2 ";
                    if (expanded.getObject()) {
                        return prefix + "fa-solid fa-down-left-and-up-right-to-center";
                    }

                    return prefix + "fa-solid fa-up-right-and-down-left-from-center";
                },
                () -> {
                    String key = expanded.getObject() ? "ContainerVisualizationPanel.collapse" : "ContainerVisualizationPanel.expand";
                    return getString(key);
                }) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandCollapseClicked(target);
            }
        };
        link.showTitleAsLabel(true);
        fragment.add(link);

        return fragment;
    }

    private void onExpandCollapseClicked(AjaxRequestTarget target) {
        expanded.setObject(!expanded.getObject());

        target.add(this);
    }
}
