/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

public class ResourceWizardPreviewPanel extends BasePanel {

    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";
    private static final String ID_EXIT = "exit";

    private final ResourceDetailsModel resourceModel;
    private LoadableModel<List<Tile<TileType>>> tiles;

    public ResourceWizardPreviewPanel(String id, ResourceDetailsModel resourceModel) {
        super(id);
        this.resourceModel = resourceModel;
    }

    private enum TileType {

        PREVIEW_DATA("fa fa-magnifying-glass"),

        CONFIGURE_OBJECT_TYPES("fa fa-object-group"),

        GO_TO_RESOURCE("fa fa-server");

        private String icon;

        TileType(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        tiles = new LoadableModel<>(false) {

            @Override
            protected List<Tile<TileType>> load() {
                List<Tile<TileType>> list = new ArrayList<>();

                for (TileType type : TileType.values()) {
                    Tile tile = new Tile(type.getIcon(), getString(type));
                    tile.setValue(type);
                    list.add(tile);
                }

                return list;
            }
        };
    }

    private void initLayout() {
        ListView<Tile<TileType>> list = new ListView<>(ID_LIST, tiles) {

            @Override
            protected void populateItem(ListItem<Tile<TileType>> item) {
                TilePanel tp = new TilePanel(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<TileType> tile = item.getModelObject();
                        switch (tile.getValue()) {
                            case PREVIEW_DATA:
                                break;
                            case CONFIGURE_OBJECT_TYPES:
                                break;
                            case GO_TO_RESOURCE:
                                goToResourcePerformed();
                                break;
                        }
                    }
                };
                item.add(tp);
            }
        };
        add(list);

        AjaxLink exit = new AjaxLink(ID_EXIT){
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().navigateToNext(PageResources.class);
            }
        };
        add(exit);
    }

    private void goToResourcePerformed() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, resourceModel.getObjectType().getOid());
        getPageBase().navigateToNext(PageResource.class, parameters);
    }
}
