/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class ObjectDataProvider2<W extends Serializable, T extends ObjectType>
        extends BaseSortableDataProvider<W> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDataProvider2.class);
    private static final String DOT_CLASS = ObjectDataProvider2.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private Set<T> selected = new HashSet<>();
    
    private boolean emptyListOnNullQuery = false;
    private boolean useObjectCounting = true;
    
    private Class<T> type;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    public ObjectDataProvider2(Component component, Class<T> type) {
        super(component, true, true);

        Validate.notNull(type);
        this.type = type;
    }
    
    public List<T> getSelectedData() {
    	preprocessSelectedDataInternal();
    	for (Serializable s : super.getAvailableData()){
    		if (s instanceof SelectableBean){
    			SelectableBean<T> selectable = (SelectableBean<T>) s;
    			if (selectable.isSelected()){
    				selected.add(selectable.getValue());
    			}
    		}
    	}
    	List<T> allSelected = new ArrayList<>();
    	allSelected.addAll(selected);
    	return allSelected;
    }
    
    private void preprocessSelectedData(){
    	 preprocessSelectedDataInternal();
         getAvailableData().clear();
    }
    
    private void preprocessSelectedDataInternal(){
    	for (W available : getAvailableData()){
         	if (available instanceof SelectableBean){
         		SelectableBean<T> selectableBean = (SelectableBean<T>) available;
         		if (selectableBean.isSelected()){
         			selected.add(selectableBean.getValue());
         		}
         	}
         }
         
         for (W available : getAvailableData()){
         	if (available instanceof SelectableBean){
         		SelectableBean<T> selectableBean = (SelectableBean<T>) available;
         		if (!selectableBean.isSelected()){
         			if (selected.contains(selectableBean.getValue())){
         				selected.remove(selectableBean.getValue());
         			}
         		}
         	}
         }
    }
    
   
    @Override
    public Iterator<W> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        
        preprocessSelectedData();
        

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);
            
            ObjectQuery query = getQuery();
            if (query == null){
            	if (emptyListOnNullQuery){
            		return new ArrayList<W>().iterator();
            	}
            	query = new ObjectQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Query {} with {}", type.getSimpleName(), query.debugDump());
            }

            List<PrismObject<T>> list = getModel().searchObjects(type, query, options, task, result);
            
            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
            }
            
            for (PrismObject<T> object : list) {
                getAvailableData().add(createDataObjectWrapper(object.asObjectable()));
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            handleNotSuccessOrHandledErrorInIterator(result);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    protected void handleNotSuccessOrHandledErrorInIterator(OperationResult result){
        getPage().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    public W createDataObjectWrapper(T obj) {
    	SelectableBean<T> selectable = new SelectableBean<T>(obj);
    	for (T s : selected){
    		if (s.getOid().equals(obj.getOid())){
    			selectable.setSelected(true);
    		}
    	}
//    	if (selected.contains(obj)){
//    		selectable.setSelected(true);
//    	}
        return (W) selectable;
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
            LoggingUtils.logException(LOGGER, "Couldn't count objects", ex);
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

    public void setType(Class<T> type) {
        Validate.notNull(type, "Class must not be null.");
        this.type = type;

        clearCache();
    }
    
    protected boolean isUseObjectCounting(){
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
}
