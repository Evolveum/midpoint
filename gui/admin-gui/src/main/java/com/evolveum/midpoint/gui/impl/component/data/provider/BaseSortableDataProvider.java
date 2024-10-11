/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.safeLongToInteger;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DistinctSearchOptionType;

/**
 * @author lazyman
 */
public abstract class BaseSortableDataProvider<T extends Serializable> extends SortableDataProvider<T, String> {

    private static final Trace LOGGER = TraceManager.getTrace(BaseSortableDataProvider.class);
    private static final String DOT_CLASS = BaseSortableDataProvider.class.getName() + ".";
    private static final String OPERATION_GET_EXPORT_SIZE_LIMIT = DOT_CLASS + "getDefaultExportSizeLimit";

    private final Component component;
    private final Map<Serializable, CachedSize> cache = new HashMap<>();
    private final boolean useCache;

    private List<T> availableData;
    private ObjectQuery query;

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
            setSort(getDefaultSortParam(), getDefaultSortOrder());
        }
        setExportLimitValue();
    }

    protected String getDefaultSortParam() {
        return "name";
    }

    protected SortOrder getDefaultSortOrder() {
        return SortOrder.ASCENDING;
    }

    protected ModelService getModelService() {
        MidPointApplication application = MidPointApplication.get();
        return application.getModel();
    }

    protected TaskManager getTaskManager() {
        MidPointApplication application = MidPointApplication.get();
        return application.getTaskManager();
    }

    protected PrismContext getPrismContext() {
        MidPointApplication application = MidPointApplication.get();
        return application.getPrismContext();
    }

    protected SchemaService getSchemaService() {
        MidPointApplication application = MidPointApplication.get();
        return application.getSchemaService();
    }

    protected GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return getSchemaService().getOperationOptionsBuilder();
    }

    protected GetOperationOptionsBuilder getOperationOptionsBuilder(Collection<SelectorOptions<GetOperationOptions>> createFrom) {
        return getSchemaService().getOperationOptionsBuilder().setFrom(createFrom);
    }

    protected RelationRegistry getRelationRegistry() {
        MidPointApplication application = MidPointApplication.get();
        return application.getRelationRegistry();
    }

    protected ModelInteractionService getModelInteractionService() {
        MidPointApplication application = MidPointApplication.get();
        return application.getModelInteractionService();
    }

    protected ModelAuditService getAuditService() {
        MidPointApplication application = MidPointApplication.get();
        return application.getAuditService();
    }

    public List<T> getAvailableData() {
        if (availableData == null) {
            availableData = new ArrayList<>();
        }
        return availableData;
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<>(object);
    }

    protected PageBase getPageBase() {
        if (component instanceof PageBase) {
            return (PageBase) component;
        }

        if (component.getPage() instanceof PageBase) {
            return (PageBase) component.getPage();
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
        return () -> true;
    }

    protected boolean checkOrderingSettings() {
        return false;
    }

    public boolean isDistinct() {
        // TODO: Default list view setting should never be needed. Always check setting for specific object type (and archetype).
        CompiledObjectCollectionView def = WebComponentUtil.getDefaultGuiObjectListType((PageBase) component.getPage());
        return def == null || def.getDistinct() != DistinctSearchOptionType.NEVER;      // change after other options are added
    }

    protected GetOperationOptionsBuilder getDefaultOptionsBuilder() {
        return getDistinctRelatedOptionsBuilder();  // probably others in the future
    }

    @NotNull
    protected Collection<SelectorOptions<GetOperationOptions>> getDistinctRelatedOptions() {
        return getDistinctRelatedOptionsBuilder().build();
    }

    @NotNull
    protected GetOperationOptionsBuilder getDistinctRelatedOptionsBuilder() {
        GetOperationOptionsBuilder builder = getOperationOptionsBuilder();
        if (isDistinct()) {
            return builder.distinct();
        } else {
            return builder;
        }
    }

    public boolean isOrderingDisabled() {
        if (!checkOrderingSettings()) {
            return false;
        }
        // TODO: Default list view setting should never be needed. Always check setting for specific object type (and archetype).
        CompiledObjectCollectionView def = WebComponentUtil.getDefaultGuiObjectListType((PageBase) component.getPage());
        return def != null && def.isDisableSorting() != null && def.isDisableSorting();
    }

    public ObjectPaging createPaging(long offset, long pageSize) {
        Integer o = safeLongToInteger(offset);
        Integer size = safeLongToInteger(pageSize);
        List<ObjectOrdering> orderings = null;
        if (!isOrderingDisabled()) {
            orderings = createObjectOrderings(getSort());
        }
        return getPrismContext().queryFactory().createPaging(o, size, orderings);
    }

    /**
     * Could be overridden in subclasses.
     */
    @NotNull
    protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
        if (sortParam == null || sortParam.getProperty() == null) {
            return List.of();
        }

        OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
        return List.of(
                getPrismContext().queryFactory().createOrdering(
                        getPrismContext().itemPathParser().asItemPath(sortParam.getProperty()), order));
    }

    public void clearCache() {
        cache.clear();
        getAvailableData().clear();
    }

    @Override
    public Iterator<? extends T> iterator(long first, long count) {
        Iterator<? extends T> iterator = internalIterator(first, count);
        return iterator;
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
            /*
              After this amount of time cached size will be removed from cache and replaced by new value
              (time in seconds).
             */
            int cacheCleanupThreshold = 60;
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

        private final long timestamp;
        private final long size;

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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CachedSize that = (CachedSize) o;

            if (size != that.size) {
                return false;
            }
            return timestamp == that.timestamp;
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
            CompiledGuiProfile adminGui = getModelInteractionService().getCompiledGuiProfile(null, result);
            if (adminGui.getDefaultExportSettings() != null && adminGui.getDefaultExportSettings().getSizeLimit() != null) {
                exportLimit = adminGui.getDefaultExportSettings().getSizeLimit();
            }
        } catch (Exception ex) {
            LOGGER.error("Unable to get default export size limit, ", ex);
        }
    }

    public void setExportSize(boolean exportSize) {
        this.exportSize = exportSize;
    }

    public boolean isUseCache() {
        return useCache;
    }

    @Override
    public void detach() {
        super.detach();

        if (availableData != null) {
            availableData.clear();
        }
    }
}
