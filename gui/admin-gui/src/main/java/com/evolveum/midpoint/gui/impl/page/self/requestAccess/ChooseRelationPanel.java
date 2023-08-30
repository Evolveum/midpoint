/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ChooseRelationPanel extends BasePanel<List<QName>> {

    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private IModel<List<RelationDefinitionType>> systemRelations;

    private LoadableModel<List<Tile<QName>>> relations;

    public ChooseRelationPanel(String id, IModel<List<QName>> model) {
        super(id, model);

        initModels();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void resetModel() {
        relations.reset();
    }

    private void initModels() {
        systemRelations = new LoadableDetachableModel<>() {

            @Override
            protected List<RelationDefinitionType> load() {
                RelationRegistry registry = MidPointApplication.get().getRelationRegistry();
                return registry.getRelationDefinitions();
            }
        };

        relations = new LoadableModel<>(false) {

            @Override
            protected List<Tile<QName>> load() {
                List<Tile<QName>> tiles = new ArrayList<>();

                List<QName> availableRelations = getModelObject();
                for (QName name : availableRelations) {
                    Tile<QName> tile = createTileForRelation(name);
                    tile.setSelected(name.equals(getDefaultRelation()));
                    tile.setValue(name);

                    tiles.add(tile);
                }

                return tiles;
            }
        };
    }

    protected QName getDefaultRelation() {
        return null;
    }

    private void initLayout() {
        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        add(listContainer);

        ListView<Tile<QName>> list = new ListView<>(ID_LIST, relations) {

            @Override
            protected void populateItem(ListItem<Tile<QName>> item) {
                TilePanel tp = new TilePanel(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<QName> tile = item.getModelObject();
                        boolean wasSelected = tile.isSelected();

                        relations.getObject().forEach(t -> t.setSelected(false));
                        tile.setSelected(!wasSelected);

                        target.add(ChooseRelationPanel.this.get(ID_LIST_CONTAINER));
                        onTileClick(target);
                    }
                };
                customizeTilePanel(tp);
                item.add(tp);
            }
        };
        listContainer.add(list);
    }

    protected void customizeTilePanel(TilePanel tp) {

    }

    protected void onTileClick(AjaxRequestTarget target) {
    }

    private Tile<QName> createTileForRelation(QName name) {
        List<RelationDefinitionType> relations = systemRelations.getObject();

        String icon = RelationUtil.getRelationIcon(name, relations);
        PolyString label = RelationUtil.getRelationLabel(name, relations);

        return createTile(icon, LocalizationUtil.translatePolyString(label), name);
    }

    private Tile<QName> createTile(String icon, String label, QName value) {
        Tile<QName> tile = new Tile<>(icon, label);
        tile.setValue(value);

        return tile;
    }

    public QName getSelectedRelation() {
        Tile<QName> selected = relations.getObject().stream().filter(t -> t.isSelected()).findFirst().orElse(null);
        if (selected == null) {
            return null;
        }

        return selected.getValue();
    }

    public boolean isSelectedRelation() {
        return relations.getObject().stream().anyMatch(t -> t.isSelected());
    }

    public List<Tile<QName>> getRelations() {
        return relations.getObject();
    }
}
