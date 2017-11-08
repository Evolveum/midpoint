/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  @author shood
 * */
public class ObjectSelectionPanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectSelectionPanel.class);

    private static final String DEFAULT_SORTABLE_PROPERTY = null;

    private static final String ID_EXTRA_CONTENT_CONTAINER = "extraContentContainer";
    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";

    private Class<? extends ObjectType> objectType;
    private boolean initialized;
    private IModel<ObjectSearchDto> searchModel;
    private ModalWindow modalWindow;
    private Context context;

    /**
     * Used to communicate between this panel (shown in a popup window) and the calling component (panel).
     * Above all, this is the place where customization of popup behavior is done, by overriding methods
     * in this class. (Originally this was done by subclassing the panel itself; it is not possible anymore.)
     */
    public static abstract class Context implements Serializable {

        // It is not possible to refer to the calling page using Java object, because it is (seemingly)
        // reinstantiated by the wicket during processing. So we have to use the reference, and the
        // context.getCallingPage() method.
        protected PageReference callingPageReference;
        protected Component callingComponent;

        public Context(Component callingComponent) {
            this.callingComponent = callingComponent;
        }

        public abstract void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object);

        public ObjectQuery getDataProviderQuery() {
            return null;
        }

        /**
         *  Determines if search capabilities in this modal window are or are not enabled
         * */
        public boolean isSearchEnabled(){
            return false;
        }

        /**
         *  provides search property to the search filter
         * */
        public QName getSearchProperty(){
            return ObjectType.F_NAME;
        }

        public String getSortableProperty(){
            return DEFAULT_SORTABLE_PROPERTY;
        }

        public abstract Class<? extends ObjectType> getObjectTypeClass();

        public PageReference getCallingPageReference() {
            return callingPageReference;
        }

        public PageBase getCallingPage() {
            return (PageBase) callingComponent.getPage();
        }

        protected WebMarkupContainer createExtraContentContainer(String extraContentId, ObjectSelectionPanel objectSelectionPanel) {
            WebMarkupContainer container = new WebMarkupContainer(extraContentId);
            container.setOutputMarkupId(true);
            container.setOutputMarkupPlaceholderTag(true);
            return container;
        }

        public Collection<SelectorOptions<GetOperationOptions>> getDataProviderOptions(){
            return null;
        }
    }

    public ObjectSelectionPanel(String id, Class<? extends ObjectType> type, ModalWindow modalWindow, Context context) {
        super(id);

        this.modalWindow = modalWindow;
        this.context = context;

        searchModel = new LoadableModel<ObjectSearchDto>(false) {

            @Override
            protected ObjectSearchDto load() {
                return new ObjectSearchDto();
            }
        };

        objectType = type;

        initLayout(context.getCallingPage());
        initialized = true;
    }

    public void initLayout(PageBase pageBase){
        Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        add(searchForm);
        searchForm.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return context.isSearchEnabled();
            }
        });

        BasicSearchPanel<ObjectSearchDto> basicSearch = new BasicSearchPanel<ObjectSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, ObjectSearchDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                ObjectSelectionPanel.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                ObjectSelectionPanel.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);

        add(context.createExtraContentContainer(ID_EXTRA_CONTENT_CONTAINER, this));

        List<IColumn<SelectableBean<ObjectType>, String>> columns = initColumns();
        ObjectDataProvider provider = new ObjectDataProvider(pageBase, this.objectType);
        provider.setQuery(context.getDataProviderQuery());
        provider.setOptions(context.getDataProviderOptions());
        TablePanel table = new TablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        addOrReplace(table);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("chooseTypeDialog.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                modalWindow.close(ajaxRequestTarget);
            }
        };
        add(cancelButton);
    }

    private List<IColumn<SelectableBean<ObjectType>, String>> initColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        IColumn column = new LinkColumn<SelectableBean<ObjectType>>(createStringResource("chooseTypeDialog.column.name"), context.getSortableProperty(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ObjectType>> rowModel){
                ObjectType object = rowModel.getObject().getValue();
                context.chooseOperationPerformed(target, object);
            }

        };
        columns.add(column);

        return columns;
    }

    private TablePanel getTablePanel() {
        return (TablePanel) get(StringUtils.join(new String[]{ID_TABLE}, ":"));
    }

    private Form getSearchForm(){
        return (Form) get(StringUtils.join(new String[]{ID_SEARCH_FORM}, ":"));
    }

    private WebMarkupContainer getExtraContentContainer(){
        return (WebMarkupContainer) get(StringUtils.join(new String[]{ID_EXTRA_CONTENT_CONTAINER}, ":"));
    }

    public void updateTableByTypePerformed(AjaxRequestTarget target, Class<? extends ObjectType> newType) {
        this.objectType = newType;
        TablePanel table = getTablePanel();
        DataTable dataTable = table.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider)dataTable.getDataProvider();
        provider.setType(objectType);

        target.add(this, WebComponentUtil.getPageBase(this).getFeedbackPanel(), table);
    }

    public void updateTablePerformed(AjaxRequestTarget target, ObjectQuery query){
        TablePanel table = getTablePanel();
        DataTable dataTable = table.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider)dataTable.getDataProvider();
        provider.setQuery(query);

        target.add(this, WebComponentUtil.getPageBase(this).getFeedbackPanel(), table);
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
//        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private void searchPerformed(AjaxRequestTarget target){
        ObjectQuery query = createObjectQuery();
        TablePanel panel = getTablePanel();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        target.add(panel);
    }

    private ObjectQuery createObjectQuery() {
        ObjectSearchDto dto = searchModel.getObject();
        ObjectQuery query = null;

        if(StringUtils.isEmpty(dto.getText())) {
            if(context.getDataProviderQuery() != null){
                return context.getDataProviderQuery();
            } else {
                return query;
            }
        }

        try {
            PageBase pageBase = WebComponentUtil.getPageBase(this);
            PrismContext prismContext = pageBase.getPrismContext();
            PolyStringNormalizer normalizer = prismContext.getDefaultPolyStringNormalizer();
            String normalized = normalizer.normalize(dto.getText());

            ObjectFilter filter = QueryBuilder.queryFor(objectType, prismContext)
                    .item(context.getSearchProperty()).contains(normalized).matchingNorm()
                    .buildFilter();

            if (context.getDataProviderQuery() != null) {
                AndFilter and = AndFilter.createAnd(context.getDataProviderQuery().getFilter(), filter);
                query = ObjectQuery.createObjectQuery(and);
            } else {
                query = ObjectQuery.createObjectQuery(filter);
            }

        } catch (Exception e){
            error(getString("chooseTypeDialog.message.queryError") + " " + e.getMessage());
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create query filter.", e);
        }

        return query;
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        searchModel.setObject(new ObjectSearchDto());

        TablePanel panel = getTablePanel();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();

        if(context.getDataProviderQuery() != null){
            provider.setQuery(context.getDataProviderQuery());
        } else {
            provider.setQuery(null);
        }

        target.add(panel, getSearchForm());
    }

}
