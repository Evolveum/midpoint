/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

/**
 * @author skublik
 *
 * Abstract class for List panels with table.
 *
 * @param <C>
 *     the container of displayed objects in table
 * @param <PO>
 *     the type of the object processed by provider
 */
public abstract class AbstractContainerableListPanel<C extends Containerable, PO extends Serializable> extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractContainerableListPanel.class);

    public static final String ID_ITEMS = "items";
    private static final String ID_ITEMS_TABLE = "itemsTable";

    private Class<? extends C> type;

    public AbstractContainerableListPanel(String id, Class<? extends C> type) {
        super(id);
        this.type = type;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initPaging();
        initLayout();
    }

    private void initLayout() {

        initListPanel();
        setOutputMarkupId(true);

    }

    protected abstract void initPaging();

    private void initListPanel() {
        WebMarkupContainer itemsContainer = new WebMarkupContainer(ID_ITEMS);
        itemsContainer.setOutputMarkupId(true);
        itemsContainer.setOutputMarkupPlaceholderTag(true);
        add(itemsContainer);

        BoxedTablePanel<PO> itemTable = initItemTable();
        itemTable.setOutputMarkupId(true);
        itemTable.setOutputMarkupPlaceholderTag(true);
        itemsContainer.add(itemTable);

        itemsContainer.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isListPanelVisible();
            }
        });

    }

    protected boolean isListPanelVisible() {
        return true;
    }

    protected WebMarkupContainer getSearchPanel(String contentAreaId) {
        return new WebMarkupContainer(contentAreaId);
    }

    protected WebMarkupContainer createHeader(String headerId) {
        return initSearch(headerId);
    }

    protected BoxedTablePanel<PO> initItemTable() {

        List<IColumn<PO, String>> columns = createColumns();
        int itemPerPage = getTableId() == null ? UserProfileStorage.DEFAULT_PAGING_SIZE : (int) getPageBase().getItemsPerPage(getTableId());
        ISelectableDataProvider<C, PO> provider = createProvider();
        BoxedTablePanel<PO> itemTable = new BoxedTablePanel<PO>(ID_ITEMS_TABLE,
                provider, columns, getTableId(), itemPerPage) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                WebMarkupContainer header = AbstractContainerableListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;

            }

            @Override
            protected Item customizeNewRowItem(Item item, IModel model) {
                String status = GuiImplUtil.getObjectStatus(model.getObject());
                if (status != null) {
                    item.add(AttributeModifier.append("class", new IModel<String>() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public String getObject() {
                            return status;
                        }
                    }));
                }
                return item;
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                WebMarkupContainer bar = initButtonToolbar(id);
                return bar != null ? bar : super.createButtonToolbar(id);
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return AbstractContainerableListPanel.this.getAdditionalBoxCssClasses();
            }

            @Override
            protected boolean hideFooterIfSinglePage(){
                return AbstractContainerableListPanel.this.hideFooterIfSinglePage();
            }

            @Override
            public int getAutoRefreshInterval() {
                return AbstractContainerableListPanel.this.getAutoRefreshInterval();
            }

            @Override
            public boolean isAutoRefreshEnabled() {
                return AbstractContainerableListPanel.this.isRefreshEnabled();
            }

            @Override
            public boolean enableSavePageSize() {
                return AbstractContainerableListPanel.this.enableSavePageSize();
            }
        };
        itemTable.setOutputMarkupId(true);
        if (getPageStorage() != null) {
            ObjectPaging pageStorage = getPageStorage().getPaging();
            if (pageStorage != null) {
                itemTable.setCurrentPage(pageStorage);
            }
        }
        return itemTable;
    }

//    protected String getTableIdKeyValue() {
//        String key;
//        if (getParent() == null) {
//            key = Classes.simpleName(getPageBase().getClass()) + "." + getId();
//        } else {
//            key = Classes.simpleName(getParent().getClass()) + "." + getId();
//        }
//
//        return key;
//    }

    protected abstract String getTableIdStringValue();

    protected abstract UserProfileStorage.TableId getTableId();

    protected abstract WebMarkupContainer initButtonToolbar(String id);

    protected boolean isRefreshEnabled(){
        return false;
    }

    protected int getAutoRefreshInterval() {
        return 0;
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    protected boolean isHeaderVisible() {
        return true;
    }

    protected String getStorageKey(){

        String key = WebComponentUtil.getObjectListPageStorageKey(getType().getSimpleName());
//        if (key == null) {
//            key = getTableIdKeyValue();
//        }

        return key;
    }

    protected PageStorage getPageStorage(String storageKey){
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
        if (storage == null) {
            storage = getSession().getSessionStorage().initPageStorage(storageKey);
        }
        return storage;
    }

    protected PageStorage getPageStorage() {
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            return getPageStorage(storageKey);
        }
        return null;
    }

    protected abstract ISelectableDataProvider<C, PO> createProvider();

    protected List<MultiFunctinalButtonDto> createNewButtonDescription() {
        return null;
    }

    public String getAdditionalBoxCssClasses() {
        return null;
    }

    protected boolean isNewObjectButtonEnabled(){
        return true;
    }

    protected boolean getNewObjectGenericButtonVisibility(){
        return true;
    }


    protected DisplayType getNewObjectButtonDisplayType(){
        return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", createStringResource("MainObjectListPanel.newObject").getString());
    }


    protected abstract WebMarkupContainer initSearch(String headerId);

    protected boolean isSearchEnabled(){
        return true;
    }

    protected abstract List<IColumn<PO, String>> createColumns();

    public BoxedTablePanel getTable() {
        return (BoxedTablePanel) get(createComponentPath(ID_ITEMS, ID_ITEMS_TABLE));
    }

    public void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
        ajaxRequestTarget.add(getItemTable());
    }

    public BoxedTablePanel<PO> getItemTable() {
        return (BoxedTablePanel<PO>) get(ID_ITEMS).get(ID_ITEMS_TABLE);
    }

    public Class<C> getType() {
        return (Class<C>) type;
    }

    protected void setType(Class<? extends C> type) {
        this.type = type;
    }

    protected boolean enableSavePageSize() {
        return true;
    }

}
