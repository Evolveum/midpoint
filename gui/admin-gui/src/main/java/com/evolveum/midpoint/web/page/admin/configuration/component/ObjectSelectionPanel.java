/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageDialog;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
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

    public ObjectSelectionPanel(String id, Class<? extends ObjectType> type, PageBase pageBase){
        super(id);

        searchModel = new LoadableModel<ObjectSearchDto>(false) {

            @Override
            protected ObjectSearchDto load() {
                return new ObjectSearchDto();
            }
        };

        objectType = type;

        initLayout(pageBase);
        initialized = true;
    }

    public void initLayout(PageBase pageBase){
        Form searchForm = new Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        add(searchForm);
        searchForm.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return isSearchEnabled();
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

        add(createExtraContentContainer(ID_EXTRA_CONTENT_CONTAINER));

        List<IColumn<SelectableBean<ObjectType>, String>> columns = initColumns();
        ObjectDataProvider provider = new ObjectDataProvider(pageBase, this.objectType);
        provider.setQuery(getDataProviderQuery());
        TablePanel table = new TablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        addOrReplace(table);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("chooseTypeDialog.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                cancelPerformed(ajaxRequestTarget);
            }
        };
        add(cancelButton);
    }

    private List<IColumn<SelectableBean<ObjectType>, String>> initColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        IColumn column = new LinkColumn<SelectableBean<ObjectType>>(createStringResource("chooseTypeDialog.column.name"), getSortableProperty(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ObjectType>> rowModel){
                ObjectType object = rowModel.getObject().getValue();
                chooseOperationPerformed(target, object);
            }

        };
        columns.add(column);

        return columns;
    }

    protected WebMarkupContainer createExtraContentContainer(String extraContentId){
        WebMarkupContainer container = new WebMarkupContainer(extraContentId);
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);
        return container;
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

        target.add(this, getPageBase().getFeedbackPanel(), table);
    }

    public void updateTablePerformed(AjaxRequestTarget target, ObjectQuery query){
        TablePanel table = getTablePanel();
        DataTable dataTable = table.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider)dataTable.getDataProvider();
        provider.setQuery(query);

        target.add(this, getPageBase().getFeedbackPanel(), table);
    }

    protected ObjectQuery getDataProviderQuery(){
        return null;
    }

    public String getSortableProperty(){
        return DEFAULT_SORTABLE_PROPERTY;
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
        // subclasses should close the modal window here
    }

    protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){}

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private PageBase getPageBase() {
        Page page = getPage();
        if (page instanceof PageBase) {
            return (PageBase) page;
        } else if (page instanceof PageDialog) {
            return ((PageDialog) page).getPageBase();
        } else {
            throw new IllegalStateException("Couldn't determine page base for " + page);
        }
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
            if(getDataProviderQuery() != null){
                return getDataProviderQuery();
            } else {
                return query;
            }
        }

        try {
            PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
            String normalized = normalizer.normalize(dto.getText());

            SubstringFilter filter = SubstringFilter.createSubstring(getSearchProperty(), objectType, getPageBase().getPrismContext(),
                    PolyStringNormMatchingRule.NAME, normalized);

            if(getDataProviderQuery() != null){
                AndFilter and = AndFilter.createAnd(getDataProviderQuery().getFilter(), filter);
                query = ObjectQuery.createObjectQuery(and);
            } else {
                query = ObjectQuery.createObjectQuery(filter);
            }

        } catch (Exception e){
            error(getString("chooseTypeDialog.message.queryError") + " " + e.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", e);
        }

        return query;
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        searchModel.setObject(new ObjectSearchDto());

        TablePanel panel = getTablePanel();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();

        if(getDataProviderQuery() != null){
            provider.setQuery(getDataProviderQuery());
        } else {
            provider.setQuery(null);
        }

        target.add(panel, getSearchForm());
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

    protected <T extends Component> T theSameForPage(T object, PageReference containingPageReference) {
        Page containingPage = containingPageReference.getPage();
        if (containingPage == null) {
            throw new IllegalStateException("Containing page cannot be determined");
        }
        String path = object.getPageRelativePath();
        T retval = (T) containingPage.get(path);
        if (retval == null) {
            throw new IllegalStateException("There is no component like " + object + " (path '" + path + "') on " + containingPage);
        }
        return retval;
    }
}
