/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by honchar
 */
public abstract class PageAdminObjectList<O extends ObjectType> extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminObjectList.class);

    private static final String DOT_CLASS = PageAdminObjectList.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";


    public PageAdminObjectList() {
        this(null);
    }

    public PageAdminObjectList(PageParameters parameters) {
        super(parameters);
        initLayout();
    }

    protected void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        initTable(mainForm);
    }

    private void initTable(Form mainForm) {
//        StringValue collectionNameParameter = getCollectionNameParameterValue();
        MainObjectListPanel<O> userListPanel = new  MainObjectListPanel<O>(ID_TABLE,
                getType(), getTableId(), getQueryOptions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<IColumn<SelectableBean<O>, String>> createColumns() {
                return PageAdminObjectList.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, O object) {
                PageAdminObjectList.this.objectDetailsPerformed(target, object);
            }

            @Override
            protected boolean isCreateNewObjectEnabled(){
                return PageAdminObjectList.this.isCreateNewObjectEnabled();
            }


            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                return PageAdminObjectList.this.addCustomFilterToContentQuery(query);
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<O>> rowModel) {
                return isNameColumnClickable(rowModel);
            }

            @Override
            protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
                return PageAdminObjectList.this.createCustomOrdering(sortParam);
            }


            protected void setDefaultSorting(BaseSortableDataProvider<SelectableBean<O>> provider){
                PageAdminObjectList.this.setDefaultSorting(provider);
            }

            @Override
            protected BaseSortableDataProvider<SelectableBean<O>> initProvider() {
                if (getCustomProvider() != null) {
                    return getCustomProvider();
                }
                return super.initProvider();
            }

            @Override
            protected String getStorageKey() {
                return super.getStorageKey();
            }

            @Override
            protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
                if (isCreateCheckColumnEnabled()) {
                    return super.createCheckboxColumn();
                }
                return null;
            }

            @Override
            protected boolean getNewObjectGenericButtonVisibility() {
                return PageAdminObjectList.this.getNewObjectGenericButtonVisibility();
            }

            @Override
            protected List<ItemPath> getFixedSearchItems() {
                return PageAdminObjectList.this.getFixedSearchItems();
            }
        };

        userListPanel.setAdditionalBoxCssClasses(WebComponentUtil.getBoxCssClasses(WebComponentUtil.classToQName(getPrismContext(), getType())));
        userListPanel.setOutputMarkupId(true);
        mainForm.add(userListPanel);
    }

    protected boolean getNewObjectGenericButtonVisibility(){
        return true;
    }

    protected BaseSortableDataProvider<SelectableBean<O>> getCustomProvider() {
        return null;
    }

    protected abstract Class<O> getType();

    protected abstract List<IColumn<SelectableBean<O>, String>> initColumns();

    protected abstract List<InlineMenuItem> createRowActions();

    protected void objectDetailsPerformed(AjaxRequestTarget target, O object){}

    protected boolean isCreateNewObjectEnabled(){
        return true;
    }

    protected List<ItemPath> getFixedSearchItems() {
        return null;
    }

    protected ObjectQuery addCustomFilterToContentQuery(ObjectQuery query){
        return query;
    }

    protected void setDefaultSorting(BaseSortableDataProvider<SelectableBean<O>> provider){
        //should be overrided if needed
    }

    protected boolean isNameColumnClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
        return null;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions(){
        return null;
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected Form getMainForm(){
        return (Form) get(ID_MAIN_FORM);
    }

    public MainObjectListPanel<O> getObjectListPanel() {
        return (MainObjectListPanel<O>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    protected boolean isCreateCheckColumnEnabled() {
        return true;
    }

}
