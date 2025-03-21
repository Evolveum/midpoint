/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

public abstract class MultiSelectObjectTileTablePanel<E extends Serializable, O extends ObjectType>
        extends MultiSelectTileTablePanel<E, SelectableBean<O>, TemplateTile<SelectableBean<O>>>
        implements SelectTileTablePanel<TemplateTile<SelectableBean<O>>, O> {

    public MultiSelectObjectTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId) {
        this(id, Model.of(ViewToggle.TILE), tableId);
    }

    public MultiSelectObjectTileTablePanel(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId) {
        super(id, viewToggle, tableId);
    }

    @Override
    public SelectableBeanObjectDataProvider<O> getProvider() {
        return (SelectableBeanObjectDataProvider<O>) super.getProvider();
    }

    @Override
    public Class<O> getType() {
        return (Class<O>) ObjectType.class;
    }

    @Override
    public @Nullable Set<O> initialSelectedObjects() {
        return SelectTileTablePanel.super.initialSelectedObjects();
    }

    @Override
    public Component createTile(String id, IModel<TemplateTile<SelectableBean<O>>> model) {
        return new SelectableObjectTilePanel<>(id, model) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                super.onClick(target);
                getModelObject().getValue().setSelected(getModelObject().isSelected());

                processSelectOrDeselectItem(getModelObject().getValue(), getProvider(), target);
                if (isSelectedItemsPanelVisible()) {
                    target.add(getSelectedItemPanel());
                }
            }
        };
    }

    protected void onSelectTableRow(IModel<SelectableBean<O>> model, AjaxRequestTarget target) {
        super.onSelectTableRow(model, target);
        if (model.getObject().isSelected()) {
            ((SelectableBeanDataProvider) getProvider()).getSelected().add(model.getObject().getValue());
        }
    }

    @Override
    protected TemplateTile<SelectableBean<O>> createTileObject(SelectableBean<O> object) {
        TemplateTile<SelectableBean<O>> t = TemplateTile.createTileFromObject(object, getPageBase());
        return t;
    }
}
