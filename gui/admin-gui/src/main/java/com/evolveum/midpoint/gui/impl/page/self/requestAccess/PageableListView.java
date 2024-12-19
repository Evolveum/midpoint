/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class PageableListView<LI extends Serializable, SPI extends Serializable> extends ListView<LI> implements IPageableItems {

    private static final long serialVersionUID = 1L;

    public PageableListView(String id, ISortableDataProvider<SPI, String> provider, UserProfileStorage.TableId tableId) {
        super(id);

        setModel(new PageableListModel<>(provider, tableId) {

            @Override
            protected List<LI> createItem(SPI providerItem) {
                return PageableListView.this.createItem(providerItem);
            }
        });
    }

    protected List<LI> createItem(SPI providerItem) {
        return List.of((LI) providerItem);
    }

    private PageableListModel getPageableModel() {
        return (PageableListModel) getModel();
    }

    @Override
    public long getItemCount() {
        return getPageableModel().getItemCount();
    }

    @Override
    public long getPageCount() {
        return getPageableModel().getPageCount();
    }

    @Override
    public long getItemsPerPage() {
        return getPageableModel().getItemsPerPage();
    }

    @Override
    public void setItemsPerPage(long itemsPerPage) {
        if (itemsPerPage < 0) {
            itemsPerPage = 0;
        }

//        addStateChange();
        getPageableModel().setItemsPerPage(itemsPerPage);
    }

    @Override
    public long getCurrentPage() {
        return getPageableModel().getCurrentPage();
    }

    @Override
    public void setCurrentPage(long page) {
        if (page < 0) {
            page = 0;
        }

        getPageableModel().setCurrentPage(page);
    }

    public ISortableDataProvider<SPI, String> getProvider() {
        return getPageableModel().getProvider();
    }

    private static class PageableListModel<LI extends Serializable, SPI extends Serializable> implements IModel<List<LI>> {

        private ISortableDataProvider<SPI, String> provider;

        private UserProfileStorage.TableId tableId;

        private long itemsPerPage;

        private long currentPage = 0;

        private List<LI> result;

        public PageableListModel(ISortableDataProvider<SPI, String> provider, UserProfileStorage.TableId tableId) {
            this.provider = provider;
            this.tableId = tableId;
        }

        public ISortableDataProvider<SPI, String> getProvider() {
            return provider;
        }

        public void setProvider(ISortableDataProvider<SPI, String> provider) {
            this.provider = provider;
        }

        public long getItemsPerPage() {
            if (tableId == null) {
                if (itemsPerPage <= 0) {
                    itemsPerPage = UserProfileStorage.DEFAULT_PAGING_SIZE;
                }

                return itemsPerPage;
            }

            MidPointAuthWebSession session = MidPointAuthWebSession.get();
            UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();

            return userProfile.getPagingSize(tableId);
        }

        public void setItemsPerPage(long itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
        }

        public long getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(long currentPage) {
            this.currentPage = currentPage;
        }

        public long getItemCount() {
            return provider.size();
        }

        public long getPageCount() {
            long itemCount = getItemCount();

            long pageCount = itemCount / getItemsPerPage();

            return itemCount % getItemsPerPage() == 0 ? pageCount : pageCount + 1;
        }

        @Override
        public List<LI> getObject() {
            if (result != null) {
                return result;
            }

            List<LI> list = new ArrayList<>();
            if (provider.size() == 0) {
                return list;
            }

            Iterator<? extends SPI> iterator = provider.iterator(getCurrentPage() * getItemsPerPage(), getItemsPerPage());
            iterator.forEachRemaining(i -> list.addAll(createItem(i)));

            result = list;

            return result;
        }

        @Override
        public void detach() {
            result = null;
        }

        protected List<LI> createItem(SPI providerItem) {
            return List.of((LI) providerItem);
        }
    }
}
