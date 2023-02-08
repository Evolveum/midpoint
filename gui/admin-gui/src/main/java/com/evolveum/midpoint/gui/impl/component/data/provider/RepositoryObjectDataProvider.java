/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.impl.component.search.Search;

import com.evolveum.midpoint.web.component.data.TypedCacheKey;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;

/**
 * @author lazyman
 */
public class RepositoryObjectDataProvider<O extends ObjectType>
        extends BaseSearchDataProvider<O, DebugObjectItem> {

    private static final String DOT_CLASS = RepositoryObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryObjectDataProvider.class);

    private Map<String, ResourceDescription> resourceCache = new HashMap<>();

    public RepositoryObjectDataProvider(Component component, IModel<Search<O>> searchModel) {
        super(component, searchModel, true);
    }

    @Override
    public Iterator<DebugObjectItem> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", first, count);
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            ObjectQuery query = getQuery();
            if (query == null) {
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            Collection<SelectorOptions<GetOperationOptions>> options = getOptions();
            Class<O> type = SearchBoxModeType.OID.equals(getSearchModel().getObject().getSearchMode()) ? (Class<O>) ObjectType.class : getType();
            List<? extends PrismObject<? extends ObjectType>> list = getModelService().searchObjects(type, query, options,
                    getPageBase().createSimpleTask(OPERATION_SEARCH_OBJECTS), result);
            for (PrismObject<? extends ObjectType> object : list) {
                getAvailableData().add(createItem(object, result));
            }
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        getPageBase().showResult(result, false);

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @NotNull
    private Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return getDefaultOptionsBuilder()
                .raw()
                .retrieve(RetrieveOption.DEFAULT)
                .build();
    }

    @Override
    protected boolean checkOrderingSettings() {
        return true;
    }

    private DebugObjectItem createItem(PrismObject<? extends ObjectType> object, OperationResult result) {
        DebugObjectItem item = DebugObjectItem.createDebugObjectItem(object);
        if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())) {
            PrismReference ref = object.findReference(ShadowType.F_RESOURCE_REF);
            if (ref == null || ref.getValue() == null) {
                return item;
            }

            PrismReferenceValue refValue = ref.getValue();
            String resourceOid = refValue.getOid();
            ResourceDescription desc = resourceCache.get(resourceOid);
            if (desc == null) {
                desc = loadDescription(resourceOid, result);
                resourceCache.put(resourceOid, desc);
            }

            item.setResourceName(desc.getName());
            item.setResourceType(desc.getType());
        }

        return item;
    }

    private ResourceDescription loadDescription(String oid, OperationResult result) {
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
        subResult.addParam("oid", oid);

        PrismObject<ResourceType> resource = null;
        String type = null;
        try {
            resource = getModelService().getObject(ResourceType.class, oid, options,
                    getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE), subResult);

            PrismReference ref = resource.findReference(ResourceType.F_CONNECTOR_REF);
            if (ref != null && ref.getValue() != null) {
                PrismReferenceValue refValue = ref.getValue();
                if (refValue.getObject() != null) {
                    PrismObject connector = refValue.getObject();
                    PrismProperty<String> pType = connector.findProperty(ConnectorType.F_CONNECTOR_TYPE);
                    if (pType != null && pType.getRealValue() != null) {
                        type = pType.getRealValue(String.class);
                    }
                }
            }

            subResult.recordSuccess();
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Resource with oid {} not found", e, oid);
            result.recordPartialError(getPageBase().createStringResource("ObjectDataProvider.message.loadResourceForAccount.notFound", oid).getString());
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load resource for account", ex);
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.loadResourceForAccount.fatalError").getString(), ex);
        } finally {
            subResult.recomputeStatus();
        }

        return new ResourceDescription(oid, WebComponentUtil.getName(resource), type);
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Class<O> type = SearchBoxModeType.OID.equals(getSearchModel().getObject().getSearchMode()) ? (Class<O>) ObjectType.class : getType();
            count = getModelService().countObjects(type, getQuery(), getOptions(),
                    getPageBase().createSimpleTask(OPERATION_COUNT_OBJECTS), result);
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.countObjects.fatalError").getString(), ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        getPageBase().showResult(result, false);
        LOGGER.trace("end::internalSize()");
        return count;
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), getType()));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), getType()), newSize);
    }

    private static class ResourceDescription implements Serializable {

        private String oid;
        private String name;
        private String type;

        private ResourceDescription(String oid, String name, String type) {
            this.oid = oid;
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getOid() {
            return oid;
        }

        public String getType() {
            return type;
        }
    }

    @Override
    public boolean isUseCache() {
        return false;
    }
}
