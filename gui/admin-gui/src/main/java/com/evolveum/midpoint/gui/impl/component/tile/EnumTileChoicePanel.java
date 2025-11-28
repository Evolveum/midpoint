/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

public abstract class EnumTileChoicePanel<T extends TileEnum> extends BasePanel<T> {

    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private LoadableModel<List<Tile<T>>> tilesModel;
    private final Class<T> tileTypeClass;

    public EnumTileChoicePanel(String id, Class<T> tileTypeClass) {
        super(id);
        this.tileTypeClass = tileTypeClass;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        tilesModel = loadTilesModel();
        initLayout();
    }

    private LoadableModel<List<Tile<T>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<T>> load() {
                List<Tile<T>> list = new ArrayList<>();

                for (T type : tileTypeClass.getEnumConstants()) {
                    Tile tile = new Tile(type.getIcon(), getTitleOfEnum(type));
                    tile.setValue(type);
                    tile.setDescription(getDescriptionForTile(type));
                    list.add(tile);
                }
                return list;
            }
        };
    }

    protected String getTitleOfEnum(T type) {
        return getString((Enum) type);
    }

    protected String getDescriptionForTile(T type) {
        return type.getDescription();
    }

    private void initLayout() {
        ListView<Tile<T>> list = new ListView<>(ID_LIST, tilesModel) {

            @Override
            protected void populateItem(ListItem<Tile<T>> item) {
                item.add(createTilePanel(ID_TILE, item.getModel()));
            }
        };
        add(list);
    }

    protected Component createTilePanel(String id, IModel<Tile<T>> tileModel) {
        return new HorizontalSTilePanel<>(id, tileModel) {

            @Override
            protected WebMarkupContainer createIconPanel(String idIcon) {
                return createTemplateIconPanel(tileModel, idIcon);
            }

            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(() -> StringUtils.isNotEmpty(tileModel.getObject().getDescription()));
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                onTemplateChosePerformed(tileModel.getObject().getValue(), target);
            }
        };
    }

    protected abstract void onTemplateChosePerformed(T view, AjaxRequestTarget target);

    protected WebMarkupContainer createTemplateIconPanel(IModel<Tile<T>> tileModel, String idIcon) {
        WebMarkupContainer icon = new WebMarkupContainer(idIcon);
        icon.add(AttributeAppender.append("class", () -> tileModel.getObject().getIcon()));
        return icon;
    }

    public LoadableModel<List<Tile<T>>> getTilesModel() {
        return tilesModel;
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        tilesModel.detach();
    }
}
