/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@Component
public class ObjectDeltaUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaUpdater.class);

    @Autowired
    private EntityModificationRegistry entityModificationRegistry;

    /**
     * modify
     */
    public <T extends ObjectType> RObject<T> update(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
                                                    RObject<T> objectToMerge, Session session, OperationResult result) {

        if (1 == 1) {
            return tryHibernateMerge(objectToMerge, session);
        }

        EntityType mainEntityType = entityModificationRegistry.getJaxbMapping(type);

        QueryParameters queryParameters = new QueryParameters();

        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();

            // for now we expect only simple item paths todo fix this!!!
            QName first = path.getFirstName();

            String queryPath = first.getLocalPart();
            String parameterPrefix = first.getLocalPart();

            Attribute attribute = mainEntityType.getAttribute(first.getLocalPart());
            switch (attribute.getPersistentAttributeType()) {
                case BASIC:
                    // todo handle QName->String
                    queryParameters.add(path, queryPath, parameterPrefix, delta.getAnyValue().getRealValue());
                    break;
                case EMBEDDED:
                    Object realValue = delta.getAnyValue().getRealValue();
                    if (realValue instanceof PolyString) {
                        realValue = RPolyString.toRepo((PolyString) realValue);

                        queryParameters.add(path, queryPath + ".orig", parameterPrefix + "orig", ((RPolyString) realValue).getOrig());
                        queryParameters.add(path, queryPath + ".norm", parameterPrefix + "norm", ((RPolyString) realValue).getNorm());
                    }
                    // todo handle 5 more types

                    break;
            }
        }

        String objectRepositoryClass = objectToMerge.getClass().getSimpleName();

        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(objectRepositoryClass).append(" o set o.version=:version,o.fullObject=:fullObject");

        for (List<QueryParameter> params : queryParameters.getParametersLists()) {
            if (params == null || params.isEmpty()) {
                continue;
            }

            for (QueryParameter param : params) {
                sb.append(",");
                sb.append(param.path).append("=:").append(param.paramName);
            }
        }

        sb.append(" where o.oid=:oid");

        Query query = session.createQuery(sb.toString());
        query.setParameter("version", objectToMerge.getVersion());
        query.setParameter("oid", objectToMerge.getOid());
        query.setParameter("fullObject", objectToMerge.getFullObject());

        for (List<QueryParameter> params : queryParameters.getParametersLists()) {
            if (params == null || params.isEmpty()) {
                continue;
            }

            for (QueryParameter param : params) {
                query.setParameter(param.paramName, param.value);
            }
        }

        int rowsCount = query.executeUpdate();
        LOGGER.trace("Updated {} rows.", rowsCount);

        return objectToMerge;
    }

    private void prepareQueryParametersForAttribute() {
        // todo implement
    }

    /**
     * add with overwrite
     */
    public <T extends ObjectType> RObject<T> update(PrismObject<T> object, RObject<T> objectToMerge, Session session,
                                                    OperationResult result) {

        return tryHibernateMerge(objectToMerge, session); // todo remove

        // todo implement

    }

    private <T extends ObjectType> RObject<T> tryHibernateMerge(RObject<T> object, Session session) {
        LOGGER.warn("One more attempt to update object {} using standard hibernate merge (slow).", object.toString());

        return (RObject) session.merge(object);
    }

    private static class QueryParameters {

        private Map<ItemPath, List<ObjectDeltaUpdater.QueryParameter>> parameters = new HashMap<>();

        public void add(ItemPath path, String queryPath, String paramName, Object value) {
            List<ObjectDeltaUpdater.QueryParameter> list = getParameters(path);
            list.add(new QueryParameter(queryPath, paramName, value));
        }


        public void add(ItemPath path, ObjectDeltaUpdater.QueryParameter param) {
            List<ObjectDeltaUpdater.QueryParameter> list = getParameters(path);
            list.add(param);
        }

        public List<ObjectDeltaUpdater.QueryParameter> getParameters(ItemPath path) {
            List<ObjectDeltaUpdater.QueryParameter> list = parameters.get(path);
            if (list == null) {
                list = new ArrayList<>();
                parameters.put(path, list);
            }

            return list;
        }

        public Map<ItemPath, List<ObjectDeltaUpdater.QueryParameter>> getParameters() {
            return parameters;
        }

        public Collection<List<ObjectDeltaUpdater.QueryParameter>> getParametersLists() {
            return parameters.values();
        }
    }

    private static class QueryParameter {

        private String path;
        private String paramName;
        private Object value;

        public QueryParameter() {
            this(null, null, null);
        }

        public QueryParameter(String path, String paramName, Object value) {
            this.path = path;
            this.paramName = paramName;
            this.value = value;
        }
    }
}
