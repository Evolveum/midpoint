/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DetailsTablePanel extends BasePanel<List<DetailsTableItem>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE_CONTAINER = "titleContainer";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS = "details";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_VALUE = "value";

    private IModel<DisplayType> display;

    public DetailsTablePanel(String id, IModel<DisplayType> display, IModel<List<DetailsTableItem>> model) {
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
        add(AttributeModifier.append("class", "card"));

        IModel<String> titleModel =  getTitleModel();
        WebMarkupContainer titleContainer = new WebMarkupContainer(ID_TITLE_CONTAINER);
        titleContainer.add(new VisibleBehaviour(() -> titleModel.getObject() != null));
        titleContainer.add(AttributeModifier.append("class", getAdditionalTitleClass()));
        titleContainer.setOutputMarkupId(true);
        add(titleContainer);

        WebComponent icon = new WebComponent(ID_ICON);
        IModel<String> iconCssClassModel =  getIconCssClassModel();
        icon.add(AttributeModifier.append("class", iconCssClassModel));
        icon.add(new VisibleBehaviour(() -> iconCssClassModel.getObject() != null));
        titleContainer.add(icon);

        Label title = new Label(ID_TITLE, titleModel);
//        title.setRenderBodyOnly(true);
        title.add(AttributeModifier.append("class", display.getObject().getCssClass()));
        titleContainer.add(title);

        IModel<String> descriptionModel = getDescriptionModel();
        Label description = new Label(ID_DESCRIPTION, descriptionModel);
        description.add(new VisibleBehaviour(() -> descriptionModel.getObject() != null));
        add(description);

        ListView<DetailsTableItem> details = new ListView<>(ID_DETAILS, getModel()) {

            @Override
            protected void populateItem(ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                item.add(new Label(ID_LABEL, () -> data.getLabel().getObject()));
                item.add(data.createValueComponent(ID_VALUE));

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }
            }
        };
        add(details);
    }

    private String getAdditionalTitleClass() {
        if (isVerticalTitlePanel()) {
            return "d-flex flex-column justify-content-center w-100";
        }
        return "";
    }

    protected boolean isVerticalTitlePanel() {
        return false;
    }

    private IModel<String> getIconCssClassModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }

            return GuiDisplayTypeUtil.getIconCssClass(display.getObject());
        };
    }

    private IModel<String> getTitleModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getTranslatedLabel(display.getObject());
        };

    }

    private IModel<String> getDescriptionModel() {
        return () -> {
            if (display == null || display.getObject() == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getHelp(display.getObject());
        };

    };
}
