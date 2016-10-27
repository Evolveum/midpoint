/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CatalogItemsPanel extends BasePanel implements IPageableItems {
	private static final long serialVersionUID = 1L;

	private static final String ID_MULTI_BUTTON_TABLE = "multiButtonTable";
    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_MENU = "menu";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_FOOTER = "footer";

    private ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> provider;
    private IModel<List<AssignmentEditorDto>> itemsListModel;

    private int itemsPerRow = 4;
    private static final long DEFAULT_ROWS_COUNT = 5;
    private PageBase pageBase;
    private IModel<String> catalogOidModel;
    private long currentPage = 0;

    public CatalogItemsPanel(String id) {
        super(id);
    }

    public CatalogItemsPanel(String id, IModel<String> catalogOidModel, PageBase pageBase) {
        this(id, catalogOidModel, pageBase, 0, null);
    }

    public CatalogItemsPanel(String id, IModel<String> catalogOidModel, final PageBase pageBase, int itemsPerRow,
                             ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> provider) {
        super(id);
        this.pageBase = pageBase;
        this.provider = provider;
        this.catalogOidModel = catalogOidModel;

        if (itemsPerRow > 0){
            this.itemsPerRow = itemsPerRow;
        }

        initItemListModel();
        setCurrentPage(0);
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        Component assignmentsTable;
        if (isCatalogOidEmpty()) {
            assignmentsTable = new Label(ID_MULTI_BUTTON_TABLE, createStringResource("PageAssignmentShoppingKart.roleCatalogIsNotConfigured"));
        } else {
            assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, itemsPerRow, itemsListModel, pageBase);
        }
        assignmentsTable.setOutputMarkupId(true);
        add(assignmentsTable);

        add(createFooter(ID_FOOTER));

    }

    protected void refreshCatalogItemsPanel() {

    }

    private void initItemListModel() {
        itemsListModel = new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return provider != null ? provider.getAvailableData() : new ArrayList<AssignmentEditorDto>();
            }

            @Override
            public void setObject(List<AssignmentEditorDto> assignmentTypeList) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private void refreshItemsPanel() {
        if (provider != null) {
            if (provider.getAvailableData() != null){
                provider.getAvailableData().clear();
            }
            long from  = currentPage * itemsPerRow * DEFAULT_ROWS_COUNT;
            provider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
        }
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, itemsPerRow, itemsListModel, pageBase);
        assignmentsTable.setOutputMarkupId(true);
        replace(assignmentsTable);
    }

    private MultiButtonTable getMultiButtonTable() {
        return (MultiButtonTable) get(ID_MULTI_BUTTON_TABLE);
    }

    protected WebMarkupContainer createFooter(String footerId) {
        return new PagingFooter(footerId, ID_PAGING_FOOTER, CatalogItemsPanel.this);
    }

    private static class PagingFooter extends Fragment {

        public PagingFooter(String id, String markupId, CatalogItemsPanel markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider);
        }

        private void initLayout(final CatalogItemsPanel catalogItemsPanel) {
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);

            final Label count = new Label(ID_COUNT, new AbstractReadOnlyModel<String>() {

                @Override
                public String getObject() {
                    return "";
                }
            });
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, catalogItemsPanel, true) {

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    CatalogItemsPanel catalogPanel = PagingFooter.this.findParent(CatalogItemsPanel.class);
                    catalogPanel.refreshItemsPanel();
                    target.add(catalogPanel);
                    target.add(count);
                }
            };
            footerContainer.add(nb2);

            add(footerContainer);
        }

        public Component getFooterMenu() {
            return get(ID_FOOTER_CONTAINER).get(ID_MENU);
        }

        public Component getFooterCountLabel() {
            return get(ID_FOOTER_CONTAINER).get(ID_COUNT);
        }

        public Component getFooterPaging() {
            return get(ID_FOOTER_CONTAINER).get(ID_PAGING);
        }

        private String createCountString(IPageable pageable) {
            long from = 0;
            long to = 0;
            long count = 0;

            if (pageable instanceof DataViewBase) {
                DataViewBase view = (DataViewBase) pageable;

                from = view.getFirstItemOffset() + 1;
                to = from + view.getItemsPerPage() - 1;
                long itemCount = view.getItemCount();
                if (to > itemCount) {
                    to = itemCount;
                }
                count = itemCount;
            } else if (pageable instanceof DataTable) {
                DataTable table = (DataTable) pageable;

                from = table.getCurrentPage() * table.getItemsPerPage() + 1;
                to = from + table.getItemsPerPage() - 1;
                long itemCount = table.getItemCount();
                if (to > itemCount) {
                    to = itemCount;
                }
                count = itemCount;
            }

            if (count > 0) {
                if (count == Integer.MAX_VALUE) {
                    return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.label.unknownCount",
                            new Object[]{from, to}).getString();
                }

                return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.label",
                        new Object[]{from, to, count}).getString();
            }

            return PageBase
                    .createStringResourceStatic(PagingFooter.this, "CountToolbar.noFound", new Object[]{})
                    .getString();
        }
    }

    @Override
    public void setCurrentPage(long page) {
        currentPage = page;
        long from  = page * itemsPerRow * DEFAULT_ROWS_COUNT;
        if (provider.getAvailableData() != null){
            provider.getAvailableData().clear();
        }
        provider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
    }

    @Override
    public void setItemsPerPage(long page) {
    }

    @Override
    public long getCurrentPage() {
        return currentPage;
    }

    @Override
    public long getPageCount() {
        if (provider != null){
            long itemsPerPage = getItemsPerPage();
            return itemsPerPage != 0 ? (provider.size() % itemsPerPage == 0 ? (provider.size() / itemsPerPage) :
                    (provider.size() / itemsPerPage + 1)) : 0;
        }
        return 0;
    }

    @Override
    public long getItemsPerPage() {
        return DEFAULT_ROWS_COUNT * itemsPerRow;
    }

    @Override
    public long getItemCount() {
        return 0l;
    }

    private boolean isCatalogOidEmpty(){
        return AssignmentViewType.ROLE_CATALOG_VIEW.equals(AssignmentViewType.getViewTypeFromSession(pageBase)) &&
                (catalogOidModel == null || StringUtils.isEmpty(catalogOidModel.getObject()));
    }

}
