/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public abstract class ViewTogglePanel extends BasePanel<ViewToggle> {

    public ViewTogglePanel(String id, IModel<ViewToggle> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        IModel<List<Toggle<ViewToggle>>> itemsModel = new LoadableModel<>(false) {
            @Override
            protected List<Toggle<ViewToggle>> load() {
                ViewToggle currentToggle = getModelObject();
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);
                asList.setActive(ViewToggle.TABLE == currentToggle);
                asList.setValue(ViewToggle.TABLE);
                asList.setTitle(LocalizationUtil.translate("TileTablePanel.switchToTable"));
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setActive(ViewToggle.TILE == currentToggle);
                asTile.setValue(ViewToggle.TILE);
                asTile.setTitle(LocalizationUtil.translate("TileTablePanel.switchToTile"));
                list.add(asTile);

                return list;
            }
        };

        TogglePanel<ViewToggle> toggle = new TogglePanel<>("toggle", itemsModel) {
            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                super.itemSelected(target, item);
                ViewTogglePanel.this.getModel().setObject(item.getObject().getValue());
                onToggleChanged(target);
            }
        };
        add(toggle);
    }

    protected abstract void onToggleChanged(AjaxRequestTarget target);
}
