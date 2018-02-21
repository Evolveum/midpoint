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

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugObjectItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;

import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class RepositoryObjectDataProvider
        extends BaseSortableDataProvider<DebugObjectItem> {

    private static final String DOT_CLASS = RepositoryObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryObjectDataProvider.class);
    private Class<? extends ObjectType> type;

    private Map<String, ResourceDescription> resourceCache = new HashMap<String, ResourceDescription>();

    public RepositoryObjectDataProvider(Component component, Class<? extends ObjectType> type) {
        super(component, true);

        setType(type);
    }

    @Override
    public Iterator<DebugObjectItem> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
			ObjectQuery query = getQuery();
			if (query == null) {
				query = new ObjectQuery();
			}
			query.setPaging(paging);

            //RAW and DEFAULT retrieve option selected
            Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
            GetOperationOptions opt = GetOperationOptions.createRaw();
            opt.setRetrieve(RetrieveOption.DEFAULT);
            options.add(SelectorOptions.create(ItemPath.EMPTY_PATH, opt));

            List<PrismObject<? extends ObjectType>> list = getModel().searchObjects((Class) type, query, options,
                    getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS), result);
            for (PrismObject<? extends ObjectType> object : list) {
                getAvailableData().add(createItem(object, result));
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

            getPage().showResult(result, false);

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @Override
    protected boolean checkOrderingSettings() {
        return true;
    }

    private DebugObjectItem createItem(PrismObject<? extends ObjectType> object, OperationResult result) {
        DebugObjectItem item = DebugObjectItem.createDebugObjectItem(object);
        if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())) {
            PrismReference ref = object.findReference(new ItemPath(ShadowType.F_RESOURCE_REF));
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
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ResourceType.F_CONNECTOR, GetOperationOptions.createResolve());

        OperationResult subResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
        subResult.addParam("oid", oid);

        PrismObject<ResourceType> resource = null;
        String type = null;
        try {
            resource = getModel().getObject(ResourceType.class, oid, options,
                    getPage().createSimpleTask(OPERATION_LOAD_RESOURCE), subResult);

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
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load resource for account", ex);
            subResult.recordFatalError("Couldn't load resource for account.");
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
            count = getModel().countObjects(type, getQuery(),
                    SelectorOptions.createCollection(ItemPath.EMPTY_PATH, GetOperationOptions.createRaw()),
                    getPage().createSimpleTask(OPERATION_COUNT_OBJECTS), result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

            getPage().showResult(result, false);
        LOGGER.trace("end::internalSize()");
        return count;
    }

    public void setType(Class<? extends ObjectType> type) {
        Validate.notNull(type);
        this.type = type;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), type));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), type), newSize);
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
}
