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

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanObjectDataProvider<O extends ObjectType> extends BaseSortableDataProvider<SelectableBean<O>> {
	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanObjectDataProvider.class);
    private static final String DOT_CLASS = SelectableBeanObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private Set<? extends O> selected = new HashSet<>();
    
    private boolean emptyListOnNullQuery = false;
    private boolean useObjectCounting = true;
    
    /**
     *  The number of all objects that the query can return. Defaults to a really big number
     *  if we cannot count the number of objects.
     *  TODO: make this default more reasonable. -1 or something like that (MID-3339)
     */
//    private int size = Integer.MAX_VALUE;
    
    private Class<? extends O> type;
    private Collection<SelectorOptions<GetOperationOptions>> options;

   public SelectableBeanObjectDataProvider(Component component, Class<? extends O> type, Set<? extends O> selected ) {
        super(component, true, true);

        Validate.notNull(type);
       if (selected != null) {
           this.selected = selected;
       }
        this.type = type;
    }

    public void clearSelectedObjects(){
    	selected.clear();
    }
    
    public List<O> getSelectedData() {
    	preprocessSelectedDataInternal();
    	for (SelectableBean<O> selectable : super.getAvailableData()) {
			if (selectable.isSelected() && selectable.getValue() != null) {
				((Set)selected).add(selectable.getValue());
			}
    	}
    	List<O> allSelected = new ArrayList<>();
    	allSelected.addAll(selected);
    	return allSelected;
    }
    
    private void preprocessSelectedData() {
    	 preprocessSelectedDataInternal();
         getAvailableData().clear();
    }
    
    private void preprocessSelectedDataInternal() {
    	for (SelectableBean<O> available : getAvailableData()) {
			if (available.isSelected() && available.getValue() != null) {
     			((Set)selected).add(available.getValue());
     		}
         }
         
         for (SelectableBean<O> available : getAvailableData()) {
			 if (!available.isSelected()) {
     			if (selected.contains(available.getValue())) {
     				selected.remove(available.getValue());
     			}
     		}
         }
    }
    
   
    @Override
    public Iterator<SelectableBean<O>> internalIterator(long offset, long pageSize) {
        LOGGER.trace("begin::iterator() offset {} pageSize {}.", new Object[]{offset, pageSize});
//        if (pageSize > 1000000) {
//        	// Failsafe. Do not even try this. This can have huge impact on the resource. (MID-3336)
//        	throw new IllegalArgumentException("Requested huge page size: "+pageSize);
//        }
        
        preprocessSelectedData();
        
        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(offset, pageSize);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);
            
            ObjectQuery query = getQuery();
            if (query == null){
            	if (emptyListOnNullQuery) {
            		return new ArrayList<SelectableBean<O>>().iterator();
            	}
            	query = new ObjectQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Query {} with {}", type.getSimpleName(), query.debugDump());
            }

            if (ResourceType.class.equals(type) && (options == null || options.isEmpty())){
            	options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
            List<PrismObject<? extends O>> list = (List)getModel().searchObjects(type, query, options, task, result);
            
            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
            }
            
            for (PrismObject<? extends O> object : list) {
                getAvailableData().add(createDataObjectWrapper(object.asObjectable()));
            }
//            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list objects", ex);
            return handleNotSuccessOrHandledErrorInIterator(result);
        } finally {
            result.computeStatusIfUnknown();
        }

        LOGGER.trace("end::iterator() {}", result);
        return getAvailableData().iterator();
    }

    protected Iterator<SelectableBean<O>> handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
    	LOGGER.trace("handling non-success result {}", result);
        // page.showResult() will not work here. We are too deep in the rendering now.
        // Also do NOT re-throw not redirect to to error page. That will break the page.
    	// Just return a SelectableBean that indicates the error.
        List<SelectableBean<O>> errorList = new ArrayList<>(1);
        SelectableBean<O> bean = new SelectableBean<>();
		bean.setResult(result);
        errorList.add(bean);
        return errorList.iterator();
    }

    public SelectableBean<O> createDataObjectWrapper(O obj) {
    	SelectableBean<O> selectable = new SelectableBean<O>(obj);
    	if (!WebComponentUtil.isSuccessOrHandledError(obj.getFetchResult())) {
    		try {
				selectable.setResult(obj.getFetchResult());
			} catch (SchemaException e) {
				throw new SystemException(e.getMessage(), e);
			}
    	}
    	for (O s : selected){
    		if (s.getOid().equals(obj.getOid())) {
    			selectable.setSelected(true);
    		}
    	}

        return selectable;
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        if (!isUseObjectCounting()) {
        	return Integer.MAX_VALUE;
        }
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
            Integer counted = getModel().countObjects(type, getQuery(), options, task, result);
            count = counted == null ? 0 : counted.intValue();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPage().showResult(result);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::internalSize(): {}", count);
        return count;
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), type));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), type), newSize);
    }

    public void setType(Class<O> type) {
        Validate.notNull(type, "Class must not be null.");
        this.type = type;

        clearCache();
    }
    
    public boolean isUseObjectCounting(){
    	return useObjectCounting;
    }
    
    public void setUseObjectCounting(boolean useCounting) {
    	this.useObjectCounting = useCounting;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }
    
    public boolean isEmptyListOnNullQuery() {
		return emptyListOnNullQuery;
	}
    
    public void setEmptyListOnNullQuery(boolean emptyListOnNullQuery) {
		this.emptyListOnNullQuery = emptyListOnNullQuery;
	}

//    public int getSize() {
//        return size;
//    }
//
//    public void setSize(int size) {
//        this.size = size;
//    }
}
