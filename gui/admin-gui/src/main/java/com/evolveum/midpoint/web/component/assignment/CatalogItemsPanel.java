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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

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
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_SUBMIT_BUTTON = "submitButton";
    private static final String ID_FOOTER = "footer";

    private ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> objectDataProvider;
    private ListDataProvider listProvider;
    private IModel<List<AssignmentEditorDto>> itemsListModel;

    private int itemsPerRow = 4;
    private static final long DEFAULT_ROWS_COUNT = 5;
    private PageBase pageBase;
    private IModel<String> catalogOidModel;
    private long currentPage = 0;
    private boolean isListProvider = false;

    public CatalogItemsPanel(String id) {
        super(id);
    }

    public CatalogItemsPanel(String id, final PageBase pageBase, int itemsPerRow,
                             ListDataProvider provider) {
        super(id);
        this.pageBase = pageBase;
        this.listProvider = provider;
        this.catalogOidModel = null;

        if (itemsPerRow > 0){
            this.itemsPerRow = itemsPerRow;
        }
        isListProvider = true;
        initItemListModel();
        setCurrentPage(0);
        initLayout();
    }

    public CatalogItemsPanel(String id, IModel<String> catalogOidModel, final PageBase pageBase, int itemsPerRow,
                             ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> provider) {
        super(id);
        this.pageBase = pageBase;
        this.objectDataProvider = provider;
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
        List<AssignmentEditorDto> itemList = new ArrayList<>();
        if (isListProvider){
            itemList = listProvider != null ? listProvider.getAvailableData() : new ArrayList<>();
        } else {
            itemList = objectDataProvider != null ? objectDataProvider.getAvailableData() : new ArrayList<>();
        }
        itemsListModel = Model.ofList(itemList);
    }

    private void refreshItemsPanel() {
        if (isListProvider){
            if (listProvider != null) {
                if (listProvider.getAvailableData() != null) {
                    listProvider.getAvailableData().clear();
                }
                long from = currentPage * itemsPerRow * DEFAULT_ROWS_COUNT;
                try {
                    listProvider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
                } catch (ArrayIndexOutOfBoundsException ex){
                    // nothing to do here
                }
            }
        } else {
            if (objectDataProvider != null) {
                if (objectDataProvider.getAvailableData() != null) {
                    objectDataProvider.getAvailableData().clear();
                }
                long from = currentPage * itemsPerRow * DEFAULT_ROWS_COUNT;
                objectDataProvider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
            }
        }
        MultiButtonTable assignmentsTable = new MultiButtonTable(ID_MULTI_BUTTON_TABLE, itemsPerRow, itemsListModel, pageBase);
        assignmentsTable.setOutputMarkupId(true);
        replace(assignmentsTable);
    }

    private MultiButtonTable getMultiButtonTable() {
        return (MultiButtonTable) get(ID_MULTI_BUTTON_TABLE);
    }

    protected WebMarkupContainer createFooter(String footerId) {
        PagingFooter footer = new PagingFooter(footerId, ID_PAGING_FOOTER, CatalogItemsPanel.this);
        footer.add(new VisibleEnableBehaviour(){
           @Override
            public boolean isVisible(){
               return !isCatalogOidEmpty() && getPageCount() > 1;
           }
        });
        return footer;
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
    }
    @Override
    public void setCurrentPage(long page) {
        currentPage = page;
        long from  = page * itemsPerRow * DEFAULT_ROWS_COUNT;
        if (isListProvider){
            if (listProvider.getAvailableData() != null) {
                listProvider.getAvailableData().clear();
            }
            try {
                listProvider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
            } catch (ArrayIndexOutOfBoundsException ex){
                // nothing to do here
            }
        } else {
            if (objectDataProvider.getAvailableData() != null) {
                objectDataProvider.getAvailableData().clear();
            }
            objectDataProvider.internalIterator(from, itemsPerRow * DEFAULT_ROWS_COUNT);
        }
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
        if (isListProvider){
            if (listProvider != null) {
                long itemsPerPage = getItemsPerPage();
                return itemsPerPage != 0 ? (listProvider.size() % itemsPerPage == 0 ? (listProvider.size() / itemsPerPage) :
                        (listProvider.size() / itemsPerPage + 1)) : 0;
            }
        } else {
            if (objectDataProvider != null) {
                long itemsPerPage = getItemsPerPage();
                return itemsPerPage != 0 ? (objectDataProvider.size() % itemsPerPage == 0 ? (objectDataProvider.size() / itemsPerPage) :
                        (objectDataProvider.size() / itemsPerPage + 1)) : 0;
            }
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
