/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

public abstract class WizardChoicePanel<T extends Serializable, AHD extends AssignmentHolderDetailsModel>
        extends AbstractWizardBasicPanel<AHD> {

    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private LoadableModel<List<Tile<T>>> tilesModel;

    public WizardChoicePanel(String id, AHD resourceModel) {
        super(id, resourceModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        tilesModel = loadTilesModel();
        initLayout();
    }

    protected abstract LoadableModel<List<Tile<T>>> loadTilesModel();

    private void initLayout() {
        ListView<Tile<T>> list = new ListView<>(ID_LIST, tilesModel) {

            @Override
            protected void populateItem(ListItem<Tile<T>> item) {
                item.add(createTilePanel(ID_TILE, item.getModel()));
            }
        };
        add(list);
    }

    protected abstract Component createTilePanel(String id, IModel<Tile<T>> tileModel);

    public LoadableModel<List<Tile<T>>> getTilesModel() {
        return tilesModel;
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        tilesModel.detach();
    }
}
