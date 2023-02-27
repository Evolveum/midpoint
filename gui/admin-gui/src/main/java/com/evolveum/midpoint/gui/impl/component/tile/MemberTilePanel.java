/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;

import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.IResource;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.RoundedImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MemberTilePanel<T extends Serializable> extends FocusTilePanel<T, TemplateTile<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_UNASSIGN = "unassign";
    private static final String ID_TAGS = "tags";
    private static final String ID_TAG = "tag";
    private static final String ID_TAG_LABEL = "tagLabel";
    private static final String ID_TAG_ICON = "tagIcon";
    private static final String ID_CHECK = "check";
    private static final String ID_MENU = "menu";

    public MemberTilePanel(String id, IModel<TemplateTile<T>> model) {
        super(id, model);
    }

    protected void initLayout() {
        super.initLayout();

        if (isSelectable()) {
            getLogo().add(new AjaxEventBehavior("click") {

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    MemberTilePanel.this.onClick(target);
                }
            });
        }

        add(AttributeAppender.append("class", "card catalog-tile-panel d-flex flex-column align-items-center bordered p-3 h-100 mb-0"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active selectable" : null));

        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK, new IModel<>() {
            @Override
            public Boolean getObject() {
                return MemberTilePanel.this.getModelObject().isSelected();
            }

            @Override
            public void setObject(Boolean object) {
            }
        }) {
            @Override
            public void onUpdate(AjaxRequestTarget target) {
                onClick(target);
            }
        };
        check.add(new VisibleBehaviour(this::isSelectable));
        add(check);

        DropdownButtonPanel menu = new DropdownButtonPanel(
                ID_MENU, new DropdownButtonDto(null, "fa-ellipsis", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Override
            protected String getSpecialButtonClass() {
                return " p-0 ";
            }

            @Override
            protected void onBeforeClickMenuItem(AjaxRequestTarget target, InlineMenuItemAction action, IModel<InlineMenuItem> item) {
                if (action instanceof ColumnMenuAction) {
                    ((ColumnMenuAction) action).setRowModel(() -> MemberTilePanel.this.getModelObject().getValue());
                }
            }
        };
        menu.add(new VisibleBehaviour(() -> !menu.getModel().getObject().getMenuItems().isEmpty()));
        add(menu);

        ListView<DisplayType> tagPanel = new ListView<>(ID_TAGS, () -> getModelObject().getTags()) {

            @Override
            protected void populateItem(ListItem<DisplayType> item) {
                DisplayType tag = item.getModelObject();

                WebMarkupContainer tagContainer = new WebMarkupContainer(ID_TAG);
                if (StringUtils.isNotEmpty(tag.getCssClass())) {
                    tagContainer.add(AttributeAppender.append("class", tag.getCssClass()));
                }
                if (StringUtils.isNotEmpty(tag.getCssStyle())) {
                    tagContainer.add(AttributeAppender.append("style", tag.getCssStyle()));
                }
                if (StringUtils.isNotEmpty(tag.getColor())) {
                    tagContainer.add(AttributeAppender.append("style", "color: " + tag.getColor()));
                }
                if (StringUtils.isEmpty(tag.getCssClass())
                        && StringUtils.isEmpty(tag.getCssStyle())
                        && StringUtils.isEmpty(tag.getColor())
                        && tag.getIcon() != null
                        && StringUtils.isNotEmpty(tag.getIcon().getColor())) {
                    tagContainer.add(AttributeAppender.append("style", "color: " + tag.getIcon().getColor()));
                }
                item.add(tagContainer);

                WebMarkupContainer icon = new WebMarkupContainer(ID_TAG_ICON);
                icon.add(AttributeAppender.append("class", () -> tag.getIcon() != null ? tag.getIcon().getCssClass() : ""));
                tagContainer.add(icon);

                Label tagLabel = new Label(ID_TAG_LABEL, () -> WebComponentUtil.getTranslatedPolyString(tag.getLabel()));
                tagContainer.add(tagLabel);
            }
        };

        tagPanel.add(new VisibleBehaviour(() -> getModelObject().getTags() != null));
        add(tagPanel);

        Component unassign = createUnassignButton(ID_UNASSIGN);
        unassign.add(AttributeAppender.append("class", getCssForUnassignButton()));
        add(unassign);
    }

    protected boolean isSelectable() {
        return true;
    }

    protected String getCssForUnassignButton() {
        return "btn btn-link mt-3 ml-auto";
    }

    protected List<InlineMenuItem> createMenuItems() {
        return new ArrayList<>();
    }

    protected Component createUnassignButton(String id) {
        return new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                MemberTilePanel.this.onUnassign(target);
            }
        };
    }

    protected void onUnassign(AjaxRequestTarget target) {

    }
}
