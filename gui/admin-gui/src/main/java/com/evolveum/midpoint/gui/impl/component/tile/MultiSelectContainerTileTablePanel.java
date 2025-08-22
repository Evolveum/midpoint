/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.web.session.UserProfileStorage;

public abstract class MultiSelectContainerTileTablePanel<E extends Serializable, C extends Containerable>
        extends MultiSelectTileTablePanel<E, PrismContainerValueWrapper<C>, TemplateTile<PrismContainerValueWrapper<C>>> {

    private final IModel<List<PrismContainerValueWrapper<C>>> model;


    public MultiSelectContainerTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId,
            IModel<ViewToggle> toggleView,
            IModel<List<PrismContainerValueWrapper<C>>> model) {
        super(id, toggleView, tableId);
        this.model = model;
    }

    @Override
    protected MultivalueContainerListDataProvider<C> createProvider() {
        return new MultivalueContainerListDataProvider<>(
                getPageBase(), () -> (Search) getSearchModel().getObject(), model) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return getCustomQuery();
            }

            @Override
            protected int internalSize() {
                if (skipSearch()) {
                    return 0;
                }
                return super.internalSize();
            }

            @Override
            public Iterator<? extends PrismContainerValueWrapper> internalIterator(long first, long count) {
                if (skipSearch()) {
                    model.getObject();
                }
                return super.internalIterator(first, count);
            }

            @Override
            protected GetOperationOptionsBuilder getDefaultOptionsBuilder() {
                return super.getDefaultOptionsBuilder();
            }
        };
    }

    @Override
    public MultivalueContainerListDataProvider<C> getProvider() {
        return (MultivalueContainerListDataProvider<C>) super.getProvider();
    }

    protected boolean skipSearch() {
        return false;
    }

    @Override
    protected Component createTile(String id, IModel<TemplateTile<PrismContainerValueWrapper<C>>> model) {
        return new TemplateTilePanel<>(id, model) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                super.onClick(target);
                getModelObject().setSelected(!getModelObject().isSelected());
                getModelObject().getValue().setSelected(getModelObject().isSelected());

                processSelectOrDeselectItem(getModelObject().getValue(), getProvider(), target);
                if (isSelectedItemsPanelVisible()) {
                    target.add(getSelectedItemPanel());
                }
            }

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();
                add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
            }
        };
    }

    protected IModel<List<PrismContainerValueWrapper<C>>> getListModel() {
        return model;
    }
}
