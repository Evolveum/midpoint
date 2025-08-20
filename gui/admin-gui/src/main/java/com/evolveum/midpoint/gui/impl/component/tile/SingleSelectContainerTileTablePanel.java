/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Iterator;
import java.util.List;

public abstract class SingleSelectContainerTileTablePanel<C extends Containerable>
        extends SingleSelectTileTablePanel<PrismContainerValueWrapper<C>, TemplateTile<PrismContainerValueWrapper<C>>> {

    private final IModel<List<PrismContainerValueWrapper<C>>> model;


    public SingleSelectContainerTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId,
            IModel<List<PrismContainerValueWrapper<C>>> model) {
        super(id, Model.of(ViewToggle.TILE), tableId);
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

                onSelectTableRow(() -> getModelObject().getValue(), target);
            }

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();
                add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
            }
        };
    }

    public IModel<List<PrismContainerValueWrapper<C>>> getDetailsModel() {
        return model;
    }
}
