/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

/**
 * RadioTileTablePanel is an extension of TileTablePanel that wraps the tile view inside a RadioGroup.
 * It allows selection of a single tile via radio button behavior.
 */
public abstract class RadioTileTablePanel<T extends Tile<?>, O extends Serializable> extends TileTablePanel<T, O> {

    protected static final String ID_TILES_RADIO_FRAGMENT = "tilesRadioFragment";
    protected static final String ID_TILES_RADIO_FORM = "tileForm";
    protected static final String ID_TILES_RADIO = "radioGroup";

    protected IModel<O> selectedTileModel;

    public RadioTileTablePanel(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId) {
        super(id, viewToggle, tableId);
        this.selectedTileModel = Model.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected PageableListView<T, O> getTiles() {
        WebMarkupContainer container = (WebMarkupContainer) get(ID_TILE_VIEW).get(ID_TILES_CONTAINER);
        return (PageableListView<T, O>) container.get(ID_TILES_RADIO_FORM).get(ID_TILES_RADIO).get(ID_TILES);

    }

    @Override
    protected WebMarkupContainer createTilesContainer(String idTilesContainer, ISortableDataProvider<O, String> provider, UserProfileStorage.TableId tableId) {
        Fragment tilesFragment = new Fragment(idTilesContainer, ID_TILES_RADIO_FRAGMENT, this);
        tilesFragment.add(AttributeModifier.replace("class", getTileContainerCssClass()));

        PageableListView<T, O> tiles = createTilesPanel(ID_TILES, provider);
        tiles.setOutputMarkupId(true);

        RadioGroup<O> radioGroup = new RadioGroup<>(ID_TILES_RADIO, getSelectedTileModel());
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                onRadioTileSelected(selectedTileModel, ajaxRequestTarget);
                ajaxRequestTarget.add(RadioTileTablePanel.this);
            }
        });
        radioGroup.add(tiles);

        Form<Void> form = new Form<>(ID_TILES_RADIO_FORM);

        form.setOutputMarkupId(true);
        form.add(radioGroup);

        tilesFragment.add(form);
        return tilesFragment;
    }

    protected IModel<O> getSelectedTileModel() {
        return selectedTileModel;
    }

    ;

    protected void onRadioTileSelected(IModel<O> selectedTileModel, AjaxRequestTarget target) {

    }

    ;
}
