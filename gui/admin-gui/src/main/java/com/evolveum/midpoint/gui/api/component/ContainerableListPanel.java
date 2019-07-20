/*
 * Copyright (c) 2010-2019 Evolveum et al.
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
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.ContainerListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class ContainerableListPanel<C extends Containerable> extends BasePanel<C>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerableListPanel.class);

    private static final String ID_TABLE = "table";

    private LoadableModel<Search> searchModel = null;
    UserProfileStorage.TableId tableId;
    ContainerListDataProvider provider = null;

    public ContainerableListPanel(String id, UserProfileStorage.TableId tableId){
        super(id);
        this.tableId = tableId;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initSearchModel();
        initLayout();
    }

    private void initSearchModel() {
        searchModel = new LoadableModel<Search>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            public Search load() {
                Search search = null;
                PageStorage storage = getPageStorage();
                if (storage != null) {
                    search = storage.getSearch();
                }
                if (search == null) {
                    search = SearchFactory.createContainerSearch(getType(), ContainerableListPanel.this.getPageBase());
                }
                return search;
            }
        };
    }

    private void initLayout() {
        initProvider();
        BoxedTablePanel<PrismContainerValueWrapper<C>> table =
                new BoxedTablePanel<PrismContainerValueWrapper<C>>(ID_TABLE, provider, initColumns(),
                        tableId, (int)getItemsPerPage()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected WebMarkupContainer createHeader(String headerId) {
                        SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                                ContainerableListPanel.this.searchPerformed(target);
                            }
                        };
                        searchPanel.add(new VisibleBehaviour(() -> isSearchVisible()));
                        return searchPanel;
                    }

                    @Override
                    protected boolean hideFooterIfSinglePage(){
                        return ContainerableListPanel.this.hideFooterIfSinglePage();
                    }
                };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        add(table);
    }

    private void initProvider() {
        provider = new ContainerListDataProvider<C>(this,
                getType(), getQueryOptions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                getPageStorage().setPaging(paging);
            }

            @Override
            public ObjectQuery getQuery() {
                try {
                    return createQuery();
                } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException
                        | ConfigurationException | SecurityViolationException e) {
                    LOGGER.error("Couldn't create query for type " + getType().getSimpleName(), e.getLocalizedMessage());
                }
                return null;
            }

        };
    }

    protected abstract Class<C> getType();

    public long getItemsPerPage() {
        UserProfileStorage userProfile = getPageBase().getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableId);
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
        return null;
    }

    protected abstract <PS extends PageStorage> PS getPageStorage();

    private ObjectQuery createQuery() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Search search = searchModel.getObject();
        ObjectQuery query = search != null ? search.createObjectQuery(getPageBase().getPrismContext()) :
                getPrismContext().queryFor(getType()).build();
        ObjectFilter customFilter = getCustomFilter();
        if (customFilter != null){
            if (query == null){
                query = getPrismContext().queryFor(getType()).build();
            }
            query.addFilter(customFilter);
        }
        return query;
    }

    protected ObjectFilter getCustomFilter(){
        return null;
    }

    private void searchPerformed(AjaxRequestTarget target){
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setSearch(searchModel.getObject());
            storage.setPaging(null);
        }

        BoxedTablePanel table = (BoxedTablePanel) get(ID_TABLE);
        table.setCurrentPage(null);
        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    protected abstract List<IColumn<PrismContainerValueWrapper<C>, String>> initColumns();

    protected boolean hideFooterIfSinglePage(){
        return false;
    }

    protected boolean isSearchVisible(){
        return true;
    }

    public ContainerListDataProvider getProvider(){
        return provider;
    }
}
