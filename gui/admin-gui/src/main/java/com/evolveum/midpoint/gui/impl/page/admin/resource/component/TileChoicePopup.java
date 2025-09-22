/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * A generic popup panel that presents a list of {@link Tile} options to the user.
 *
 * <p>Each tile represents a selectable value of type {@code O}. When the user
 * clicks on a tile, it becomes the current selection. The footer contains:
 * <ul>
 *   <li>a primary "accept" button that executes {@link #performAction(AjaxRequestTarget, Serializable)}
 *       with the selected value,</li>
 *   <li>a "close" link that dismisses the popup.</li>
 * </ul>
 *
 * <p>Subclasses should override {@link #performAction(AjaxRequestTarget, Serializable)}
 * to define what happens after the user confirms their selection.</p>
 *
 * @param <O> type of value represented by each {@link Tile}
 */
public abstract class TileChoicePopup<O extends Serializable>
        extends BasePanel<List<Tile<O>>> implements Popupable {

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_TILES_CONTAINER = "tilesContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private static final String ID_BUTTON_CREATE_NEW_TASK = "accept";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private Fragment footer;
    private AjaxIconButton acceptButton;

    private O selectedValue;

    public TileChoicePopup(String id, IModel<List<Tile<O>>> tileModel, O selectedValue) {
        super(id, tileModel);
        this.selectedValue = selectedValue;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        initFooter();
    }

    private void initLayout() {

        Label text = new Label(ID_TEXT, getText());
        text.setOutputMarkupId(true);
        add(text);

        Label subText = new Label(ID_SUBTEXT, getSubText());
        subText.setOutputMarkupId(true);
        add(subText);

        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.setOutputMarkupId(true);
        add(tilesContainer);

        ListView<Tile<O>> list = new ListView<>(ID_LIST, getModel()) {
            @Override
            protected void populateItem(@NotNull ListItem<Tile<O>> listItem) {
                listItem.add(createTilePanel(ID_TILE, listItem.getModel()));
            }
        };
        list.setOutputMarkupId(true);
        tilesContainer.add(list);
    }

    protected Component createTilePanel(String id, IModel<Tile<O>> tileModel) {
        TilePanel<Tile<O>, O> components = new TilePanel<>(id, tileModel) {

            @Contract(" -> new")
            @Override
            protected @NotNull VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(() -> {
                    String description = tileModel.getObject().getDescription();
                    return StringUtils.isNotEmpty(description);
                });
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                Tile<O> selectedTile = tileModel.getObject();
                selectedValue = selectedTile != null ? selectedTile.getValue() : null;

                if (acceptButton != null) {
                    target.add(acceptButton);
                }
                target.add(geTilesContainer());
            }
        };

        initTileStatus(tileModel, components);
        return components;
    }

    private void initTileStatus(IModel<Tile<O>> tileModel, TilePanel<Tile<O>, O> components) {
        if (getSelectedValue() != null && getSelectedValue()
                .equals(tileModel.getObject().getValue())) {
            components.add(AttributeModifier.append("class", "active"));
        }
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        acceptButton = new AjaxIconButton(ID_BUTTON_CREATE_NEW_TASK,
                getAcceptButtonIcon(),
                getAcceptButtonLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                performAction(target, getSelectedValue());
            }
        };
        acceptButton.showTitleAsLabel(true);
        acceptButton.setOutputMarkupId(true);
        acceptButton.add(new EnableBehaviour(() -> getSelectedValue() != null));
        footer.add(acceptButton);

        footer.add(new AjaxLink<Void>(ID_CLOSE) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        });
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected IModel<String> getAcceptButtonLabel() {
        return createStringResource("TileChoicePopup.accept");
    }

    protected IModel<String> getAcceptButtonIcon() {
        return () -> "fa-solid fa-check";
    }

    protected void performAction(AjaxRequestTarget target, O value) {
    }

    protected IModel<String> getSubText() {
        return createStringResource("TileChoicePopup.subText");
    }

    protected IModel<String> getText() {
        return createStringResource("TileChoicePopup.text");
    }

    protected final O getSelectedValue() {
        return selectedValue;
    }

    private Component geTilesContainer() {
        return get(ID_TILES_CONTAINER);
    }
}
