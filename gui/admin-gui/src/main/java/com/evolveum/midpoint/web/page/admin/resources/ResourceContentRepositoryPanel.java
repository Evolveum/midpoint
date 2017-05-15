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
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageAccounts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

public class ResourceContentRepositoryPanel extends ResourceContentPanel {
	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentRepositoryPanel.class);
	
	private static final String DOT_CLASS = ResourceContentRepositoryPanel.class.getName() + ".";
    private static final String OPERATION_GET_TOTALS = DOT_CLASS + "getTotals";
	
	
    private static final String ID_TOTAL = "total";
    private static final String ID_DELETED = "deleted";
    private static final String ID_UNMATCHED = "unmatched";
    private static final String ID_DISPUTED = "disputed";
    private static final String ID_LINKED = "linked";
    private static final String ID_UNLINKED = "unlinked";
    private static final String ID_NOTHING = "nothing";
    
      private LoadableModel<Integer> totalModel;
	    private LoadableModel<Integer> deletedModel;
	    private LoadableModel<Integer> unmatchedModel;
	    private LoadableModel<Integer> disputedModel;
	    private LoadableModel<Integer> linkedModel;
	    private LoadableModel<Integer> unlinkedModel;
	    private LoadableModel<Integer> nothingModel;
	    

	public ResourceContentRepositoryPanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			QName objectClass, ShadowKindType kind, String intent, String searchMode, PageBase pageBase) {
		super(id, resourceModel, objectClass, kind, intent, searchMode, pageBase);
		
		
	}
	
	 protected void initShadowStatistics(WebMarkupContainer totals) {
	        
		 totals.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return createQuery() != null;
			}
		 });

	        totalModel = createTotalModel();
	        deletedModel = createTotalsModel(SynchronizationSituationType.DELETED);
	        unmatchedModel = createTotalsModel(SynchronizationSituationType.UNMATCHED);
	        disputedModel = createTotalsModel(SynchronizationSituationType.DISPUTED);
	        linkedModel = createTotalsModel(SynchronizationSituationType.LINKED);
	        unlinkedModel = createTotalsModel(SynchronizationSituationType.UNLINKED);
	        nothingModel = createTotalsModel(null);

	        totals.add(new Label(ID_TOTAL, totalModel));
	        totals.add(new Label(ID_DELETED, deletedModel));
	        totals.add(new Label(ID_UNMATCHED, unmatchedModel));
	        totals.add(new Label(ID_DISPUTED, disputedModel));
	        totals.add(new Label(ID_LINKED, linkedModel));
	        totals.add(new Label(ID_UNLINKED, unlinkedModel));
	        totals.add(new Label(ID_NOTHING, nothingModel));

	    }

	    private LoadableModel<Integer> createTotalModel() {
	        return new LoadableModel<Integer>(false) {
	        	private static final long serialVersionUID = 1L;
	            @Override
	            protected Integer load() {
	                int total = 0;

	                total += deletedModel.getObject();
	                total += unmatchedModel.getObject();
	                total += disputedModel.getObject();
	                total += linkedModel.getObject();
	                total += unlinkedModel.getObject();
	                total += nothingModel.getObject();

	                return total;
	            }
	        };
	    }
	
	 private LoadableModel<Integer> createTotalsModel(final SynchronizationSituationType situation) {
	        return new LoadableModel<Integer>(false) {
	        	private static final long serialVersionUID = 1L;
	            @Override
	            protected Integer load() {
	                ObjectFilter resourceFilter =  QueryBuilder.queryFor(ShadowType.class, getPageBase().getPrismContext())
	                        .item(ShadowType.F_RESOURCE_REF).ref(ResourceContentRepositoryPanel.this.getResourceModel().getObject().getOid())
	                        .buildFilter();

	                if (resourceFilter == null) {
	                    return 0;
	                }

	                ObjectFilter filter = createQuery().getFilter();
	                if (filter == null) {
	                	return 0;
	                }
	                Collection<SelectorOptions<GetOperationOptions>> options =
	                        SelectorOptions.createCollection(GetOperationOptions.createRaw());
	                Task task = getPageBase().createSimpleTask(OPERATION_GET_TOTALS);
	                OperationResult result = new OperationResult(OPERATION_GET_TOTALS);
	                try {
	                    ObjectFilter situationFilter = QueryBuilder.queryFor(ShadowType.class, getPageBase().getPrismContext())
	                            .item(ShadowType.F_SYNCHRONIZATION_SITUATION).eq(situation)
	                            .buildFilter();
	                    ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(filter, situationFilter));
	                    return getPageBase().getModelService().countObjects(ShadowType.class, query, options, task, result);
	                } catch (CommonException|RuntimeException ex) {
	                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count shadows", ex);
	                }

	                return 0;
	            }
	        };
	    }

	@Override
	protected SelectorOptions<GetOperationOptions> addAdditionalOptions() {
		return new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch());
	}

	@Override
	protected boolean isUseObjectCounting() {
		return true;
	}

	@Override
	protected Search createSearch() {
		return SearchFactory.createSearch(ShadowType.class, getPageBase());
	}

	@Override
	protected ModelExecuteOptions createModelOptions() {
		return ModelExecuteOptions.createRaw();
	}

}
