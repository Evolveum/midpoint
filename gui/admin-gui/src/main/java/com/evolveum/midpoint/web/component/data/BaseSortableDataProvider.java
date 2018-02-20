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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageDialog;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListsType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.safeLongToInteger;

/**
 * @author lazyman
 */
public abstract class BaseSortableDataProvider<T extends Serializable> extends SortableDataProvider<T, String> {

    private static final Trace LOGGER = TraceManager.getTrace(BaseSortableDataProvider.class);
    private static final String DOT_CLASS = BaseSortableDataProvider.class.getName() + ".";
    private static final String OPERATION_GET_EXPORT_SIZE_LIMIT = DOT_CLASS + "getDefaultExportSizeLimit";

    private Component component;
    private List<T> availableData;
    private ObjectQuery query;

    // after this amount of time cached size will be removed
    // from cache and replaced by new value, time in seconds
    private Map<Serializable, CachedSize> cache = new HashMap<Serializable, CachedSize>();
    private int cacheCleanupThreshold = 60;
    private boolean useCache;
    private boolean exportSize = false;
    private long exportLimit = -1;

    public BaseSortableDataProvider(Component component) {
        this(component, false, true);
    }

    public BaseSortableDataProvider(Component component, boolean useCache) {
        this(component, useCache, true);
    }

    public BaseSortableDataProvider(Component component, boolean useCache, boolean useDefaultSortingField) {
        Validate.notNull(component, "Component must not be null.");
        this.component = component;
        this.useCache = useCache;

        if (useDefaultSortingField) {
            setSort("name", SortOrder.ASCENDING);
        }
        setExportLimitValue();
    }

    protected ModelService getModel() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getModel();
    }

    protected RepositoryService getRepositoryService() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getRepositoryService();
    }

    protected TaskManager getTaskManager() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskManager();
    }

    protected PrismContext getPrismContext() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getPrismContext();
    }

    protected TaskService getTaskService() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getTaskService();
    }

    protected ModelInteractionService getModelInteractionService() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getModelInteractionService();
    }

    protected WorkflowService getWorkflowService() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getWorkflowService();
    }

    protected ModelAuditService getAuditService() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getAuditService();
    }

	protected WorkflowManager getWorkflowManager() {
		MidPointApplication application = (MidPointApplication) MidPointApplication.get();
		return application.getWorkflowManager();
	}

    public List<T> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<T>();
        }
        return availableData;
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }

    protected PageBase getPage() {
        if (component instanceof PageBase) {
            return (PageBase) component;
        }

        if (component.getPage() instanceof PageBase) {
            return (PageBase) component.getPage();
        }

        if (component.getPage() instanceof PageDialog) {
            return ((PageDialog) component.getPage()).getPageBase();
        }

        throw new IllegalStateException("Component is not instance of '" + PageBase.class.getName()
                + "' or is not placed on page of that instance.");
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    /**
     * Flag method for {@link TablePanel}. If true navigation panel with paging "X to Y from Z results is shown",
     * otherwise only "previous and next" simple paging is used.
     *
     * @return By defaults it returns true.
     */
    public IModel<Boolean> isSizeAvailableModel() {
        return new AbstractReadOnlyModel<Boolean>() {
            @Override
            public Boolean getObject() {
                return true;
            }
        };
    }

    protected boolean checkOrderingSettings() {
        return false;
    }

    protected boolean isOrderingDisabled() {
        PageBase page = (PageBase) component.getPage();
        AdminGuiConfigurationType config = page.getPrincipal().getAdminGuiConfiguration();
        if (config == null) {
            return false;
        }
        GuiObjectListsType lists = config.getObjectLists();
        if (lists == null) {
            return false;
        }

        GuiObjectListType def = lists.getDefault();
        if (def == null) {
            return false;
        }

        return def.isDisableSorting();
    }

    protected ObjectPaging createPaging(long offset, long pageSize) {
        Integer o = safeLongToInteger(offset);
        Integer size = safeLongToInteger(pageSize);
        List<ObjectOrdering> orderings = null;
        if (!checkOrderingSettings() || !isOrderingDisabled()) {
            orderings = createObjectOrderings(getSort());
        }
        return ObjectPaging.createPaging(o, size, orderings);
    }

	/**
	 * Could be overridden in subclasses.
	 */
	@NotNull
	protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
		if (sortParam != null && sortParam.getProperty() != null) {
			OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
			return Collections.singletonList(
					ObjectOrdering.createOrdering(
							new ItemPath(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty())), order));
		} else {
			return Collections.emptyList();
		}
	}

	public void clearCache() {
        cache.clear();
        getAvailableData().clear();
    }

    public int getCacheCleanupThreshold() {
        return cacheCleanupThreshold;
    }

    public void setCacheCleanupThreshold(int cacheCleanupThreshold) {
        Validate.isTrue(cacheCleanupThreshold > 0, "Cache cleanup threshold must be bigger than zero.");
        this.cacheCleanupThreshold = cacheCleanupThreshold;
    }

    @Override
    public Iterator<? extends T> iterator(long first, long count) {
        Iterator<? extends T> iterator = internalIterator(first, count);
        saveProviderPaging(getQuery(), createPaging(first, count));

        return iterator;
    }

    protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
    }

    public abstract Iterator<? extends T> internalIterator(long first, long count);

    @Override
    public long size() {
        LOGGER.trace("begin::size()");
        if (!useCache) {
            int internalSize = internalSize();
            return exportSize && exportLimit >= 0 && exportLimit < internalSize ? exportLimit : internalSize;
        }

        long size;
        CachedSize cachedSize = getCachedSize(cache);
        if (cachedSize != null) {
            long timestamp = cachedSize.getTimestamp();
            if (System.currentTimeMillis() - timestamp > cacheCleanupThreshold * 1000) {
                //recreate
                size = internalSize();
                addCachedSize(cache, new CachedSize(size, System.currentTimeMillis()));
            } else {
                LOGGER.trace("Size returning from cache.");
                size = cachedSize.getSize();
            }
        } else {
            //recreate
            size = internalSize();
            addCachedSize(cache, new CachedSize(size, System.currentTimeMillis()));
        }

        LOGGER.trace("end::size(): {}", size);
        return exportSize && exportLimit >= 0 && exportLimit < size ? exportLimit : size;
    }

    protected abstract int internalSize();

    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(query);
    }

    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(query, newSize);
    }

    public static class CachedSize implements Serializable {

        private long timestamp;
        private long size;

        private CachedSize(long size, long timestamp) {
            this.size = size;
            this.timestamp = timestamp;
        }

        public long getSize() {
            return size;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CachedSize that = (CachedSize) o;

            if (size != that.size) return false;
            if (timestamp != that.timestamp) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (timestamp ^ (timestamp >>> 32));
            result = 31 * result + (int) (size ^ (size >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "CachedSize(size=" + size + ", timestamp=" + timestamp + ")";
        }
    }

    private void setExportLimitValue() {
        OperationResult result = new OperationResult(OPERATION_GET_EXPORT_SIZE_LIMIT);
        try {
            AdminGuiConfigurationType adminGui = getModelInteractionService().getAdminGuiConfiguration(null, result);
            if (adminGui.getDefaultExportSettings() != null && adminGui.getDefaultExportSettings().getSizeLimit() != null) {
                exportLimit = adminGui.getDefaultExportSettings().getSizeLimit();
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to get default export size limit, ", ex);
        }
    }

    public boolean isExportSize() {
        return exportSize;
    }

    public void setExportSize(boolean exportSize) {
        this.exportSize = exportSize;
    }
}
