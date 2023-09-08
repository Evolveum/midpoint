/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedPagingPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.GridView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * Created by honchar.
 */
public abstract class GridViewComponent<O extends Object> extends BasePanel<ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>> {
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_ROW_COUNT = 5;
    private static final int DEFAULT_COLS_COUNT = 4;
    private static final String ID_CELL_ITEM = "cellItem";
    private static final String ID_ROWS = "rows";
    private static final String ID_COLS = "cols";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";

    public GridViewComponent(String id, IModel<ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>> dataProviderModel){
        super(id, dataProviderModel);
    }


    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        GridView<AssignmentEditorDto> gridView = new GridView<>(ID_ROWS, getModelObject()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateEmptyItem(Item<AssignmentEditorDto> item) {
                GridViewComponent.this.populateEmptyItem(item);
            }

            @Override
            protected void populateItem(Item<AssignmentEditorDto> item) {
                GridViewComponent.this.populateItem(item);
                item.add(AttributeAppender.append("class", getGridItemStyleClass(item.getModel())));
            }

         };
        gridView.add(new VisibleEnableBehaviour(){
            public boolean isVisible(){
                BaseSortableDataProvider p = (BaseSortableDataProvider) GridViewComponent.this.getModelObject();
                return true;
            }
        });
        gridView.setRows(getRowsCount());
        gridView.setColumns(getColsCount());
        gridView.setOutputMarkupId(true);
        gridView.setItemsPerPage(getColsCount() * getRowsCount());
        add(gridView);

        add(createFooter());
    }

    protected WebMarkupContainer createFooter() {
        WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
        footerContainer.setOutputMarkupId(true);
        footerContainer.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return GridViewComponent.this.getGridView().getPageCount() > 1;
            }
        });

        final Label count = new Label(ID_COUNT, new IModel<String>() {

            @Override
            public String getObject() {
                return "";
            }
        });
        count.setOutputMarkupId(true);
        footerContainer.add(count);

        BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, getGridView(), true) {

            @Override
            protected void onPageChanged(AjaxRequestTarget target, long page) {
                GridViewComponent.this.getGridView().setCurrentPage(page);
                target.add(GridViewComponent.this.getGridView().getParent());
                target.add(count);
            }
        };
        footerContainer.add(nb2);
        return footerContainer;
    }

    private GridView getGridView(){
        return (GridView) get(ID_ROWS);
    }

    protected int getRowsCount(){
        return DEFAULT_ROW_COUNT;
    }

    protected int getColsCount(){
        return DEFAULT_COLS_COUNT;
    }

    protected void populateEmptyItem(Item<AssignmentEditorDto> item) {
        item.add(new WebMarkupContainer(ID_CELL_ITEM));
    }

    protected String getGridItemStyleClass(IModel<AssignmentEditorDto> model){
        return "";
    }

    public static String getCellItemId(){
        return ID_CELL_ITEM;
    }

    protected abstract void populateItem(Item<AssignmentEditorDto> item);

    public ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> getProvider(){
        return GridViewComponent.this.getModelObject();
    }
}
