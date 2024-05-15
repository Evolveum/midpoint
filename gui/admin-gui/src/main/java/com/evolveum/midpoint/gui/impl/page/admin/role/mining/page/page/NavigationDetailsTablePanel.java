/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

public class NavigationDetailsTablePanel extends BasePanel<List<DetailsTableItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_ICON_BOX = "iconBox";
    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS = "details";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_VALUE = "value";

    private final IModel<DisplayType> display;

    public NavigationDetailsTablePanel(String id, IModel<DisplayType> display, IModel<List<DetailsTableItem>> model) {
        super(id, model);
        this.display = display;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        addPanelProperty();

        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        add(iconBox);

        Label icon = new Label(ID_ICON, "");
        icon.add(AttributeModifier.append("class", getIconCssClassModel()));
        iconBox.add(icon);

        IModel<String> titleModel = getTitleModel();
        Label title = new Label(ID_TITLE, titleModel);
        title.setRenderBodyOnly(true);
        title.add(new VisibleBehaviour(() -> titleModel.getObject() != null));
        add(title);

        IModel<String> descriptionModel = getDescriptionModel();
        Label description = new Label(ID_DESCRIPTION, descriptionModel);
        description.setRenderBodyOnly(true);
        description.add(new VisibleBehaviour(() -> descriptionModel.getObject() != null));
        add(description);

        ListView<DetailsTableItem> details = new ListView<>(ID_DETAILS, getModel()) {

            @Override
            protected void populateItem(@NotNull ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                item.add(new Label(ID_LABEL, () -> data.getLabel().getObject()));
                item.add(data.createValueComponent(ID_VALUE));

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
            }
        };
        add(details);

        add(getNavigationComponent());
    }

    protected void addPanelProperty() {
        add(AttributeModifier.append("class", "card"));
    }

    protected Component getNavigationComponent() {
        return new WebMarkupContainer("navigation");
    }

    private @NotNull IModel<String> getIconCssClassModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }

            return GuiDisplayTypeUtil.getIconCssClass(display.getObject());
        };
    }

    private @NotNull IModel<String> getTitleModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getTranslatedLabel(display.getObject());
        };

    }

    private @NotNull IModel<String> getDescriptionModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getHelp(display.getObject());
        };

    }

    ;
}
