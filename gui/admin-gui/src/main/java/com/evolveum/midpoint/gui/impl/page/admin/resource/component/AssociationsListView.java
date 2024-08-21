/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.AssociationTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.AssociationDefinitionWrapper;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

public abstract class AssociationsListView extends PageableListView<Tile<AssociationDefinitionWrapper>, Tile<AssociationDefinitionWrapper>> {

    private final ResourceDetailsModel resourceDetailsModel;

    public AssociationsListView(
            String id,
            ListDataProvider<Tile<AssociationDefinitionWrapper>> provider,
            ResourceDetailsModel resourceDetailsModel) {
        super(id, provider, null);
        this.resourceDetailsModel = resourceDetailsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setItemsPerPage(4);
    }

    @Override
    protected void populateItem(ListItem<Tile<AssociationDefinitionWrapper>> item) {
        item.add(createTilePanel(getTitlePanelId(), item.getModel()));
    }

    protected abstract String getTitlePanelId();

    private Component createTilePanel(String id, IModel<Tile<AssociationDefinitionWrapper>> tileModel) {
        return new AssociationTilePanel(id, tileModel, resourceDetailsModel) {

            @Override
            protected void onClick(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
                onTileClickPerformed(value, target);
            }
        };
    }

    protected abstract void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target);
}
