/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.*;

/**
 * Created by honchar
 */
public abstract class PageAdminObjectList<O extends ObjectType> extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAdminObjectList.class);

    private static final String DOT_CLASS = PageAdminObjectList.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";


    public PageAdminObjectList(){
        this(null);
    }

    public PageAdminObjectList(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        initTable(mainForm);
    }

    private void initTable(Form mainForm) {
        //TODO fix tableId
        StringValue collectionNameParameter = getCollectionNameParameterValue();
        MainObjectListPanel<O> userListPanel = new MainObjectListPanel<O>(ID_TABLE,
                getType(), collectionNameParameter == null || collectionNameParameter.isEmpty() ?
                getTableId() : UserProfileStorage.TableId.COLLECTION_VIEW_TABLE, getQueryOptions(), this) {
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
            protected void newObjectPerformed(AjaxRequestTarget target) {
                newObjectActionPerformed(target);
            }

            @Override
            protected ObjectQuery createContentQuery() {
                ObjectQuery contentQuery = super.createContentQuery();
                ObjectFilter usersViewFilter = getArchetypeViewFilter();
                if (usersViewFilter != null){
                    if (contentQuery == null) {
                        contentQuery = PageAdminObjectList.this.getPrismContext().queryFactory().createQuery();
                    }
                    contentQuery.addFilter(usersViewFilter);
                }
                return contentQuery;
            }

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                return PageAdminObjectList.this.addCustomFilterToContentQuery(query);
            }

            @Override
            protected boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                return isNameColumnClickable(rowModel);
            }

            @Override
            protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
                return PageAdminObjectList.this.createCustomOrdering(sortParam);
            }

            @Override
            protected String getTableIdKeyValue(){
                return collectionNameParameter == null || collectionNameParameter.isEmpty() ?
                        super.getTableIdKeyValue() : super.getTableIdKeyValue() + "." + collectionNameParameter.toString();
            }
        };

        userListPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES);
        userListPanel.setOutputMarkupId(true);
        mainForm.add(userListPanel);
    }

    protected abstract Class<O> getType();

    protected abstract List<IColumn<SelectableBean<O>, String>> initColumns();

    protected abstract List<InlineMenuItem> createRowActions();

    protected void objectDetailsPerformed(AjaxRequestTarget target, O object){}

    protected void newObjectActionPerformed(AjaxRequestTarget target){}

    protected ObjectFilter getArchetypeViewFilter(){
        StringValue collectionNameParam = getCollectionNameParameterValue();
        if (collectionNameParam == null || collectionNameParam.isEmpty()) {
            return null;
        }
        String collectionName = collectionNameParam.toString();
        CompiledObjectCollectionView view = getCompiledUserProfile().findObjectViewByViewName(getType(), collectionName);
        return view != null ? view.getFilter() : null;
    }

    protected StringValue getCollectionNameParameterValue(){
        PageParameters parameters = getPageParameters();
        return parameters ==  null ? null : parameters.get(PARAMETER_OBJECT_COLLECTION_NAME);
    }

    protected ObjectQuery addCustomFilterToContentQuery(ObjectQuery query){
        return query;
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


}
