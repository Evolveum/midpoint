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
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.helpers.modify.HqlQuery;
import com.evolveum.midpoint.repo.sql.helpers.modify.QueryParameter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
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

        // todo improve multitable id bulk strategy
        // todo handle nameCopy/name correctly

        List<HqlQuery> queries = new ArrayList<>();

        ManagedType mainEntityType = entityModificationRegistry.getJaxbMapping(type);

        Map<String, QueryParameter> queryParameters = new HashMap<>();

        String queryPath;
        String parameterPrefix;
        for (ItemDelta delta : modifications) {
            queryPath = "";
            parameterPrefix = "";

            ManagedType managedType = mainEntityType;

            ItemPath path = delta.getPath();
            Iterator<ItemPathSegment> segments = path.getSegments().iterator();
            while (segments.hasNext()) {
                ItemPathSegment segment = segments.next();
                if (!(segment instanceof NameItemPathSegment)) {
                    throw new SystemException("segment not name item"); // todo proper handling
                }

                NameItemPathSegment nameSegment = (NameItemPathSegment) segment;
                String nameLocalPart = nameSegment.getName().getLocalPart();

                Attribute attribute = entityModificationRegistry.findAttribute(managedType, nameLocalPart);
                if (attribute == null) {
                    attribute = entityModificationRegistry.findAttributeOverride(managedType, nameLocalPart);
                }

                if (attribute == null) {
                    // there's no table/column that needs update
                    continue;
                }

                if (!queryPath.isEmpty()) {
                    queryPath += ".";
                }

                queryPath += attribute.getName();
                parameterPrefix += attribute.getName();

                if (segments.hasNext()) {
                    managedType = entityModificationRegistry.getMapping(attribute.getJavaType());
                    continue;
                }

                switch (attribute.getPersistentAttributeType()) {
                    case BASIC:
                        // todo handle QName->String
                        queryParameters.put(parameterPrefix,
                                new QueryParameter(queryPath, parameterPrefix, delta.getAnyValue().getRealValue()));
                        break;
                    case EMBEDDED:
                        Object realValue = delta.getAnyValue().getRealValue();
                        if (realValue instanceof PolyString) {
                            realValue = RPolyString.toRepo((PolyString) realValue);

                            queryParameters.put(parameterPrefix + "orig",
                                    new QueryParameter(queryPath + ".orig", parameterPrefix + "orig", ((RPolyString) realValue).getOrig()));
                            queryParameters.put(parameterPrefix + "norm",
                                    new QueryParameter(queryPath + ".norm", parameterPrefix + "norm", ((RPolyString) realValue).getNorm()));
                        }
                        // todo handle 5 more types

                        break;
                }
            }
        }

        String objectRepositoryClass = objectToMerge.getClass().getSimpleName();

        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(objectRepositoryClass).append(" o set o.version=:version,o.fullObject=:fullObject");

        for (QueryParameter param : queryParameters.values()) {
            sb.append(",");
            sb.append(param.getPath()).append("=:").append(param.getParamName());
        }

        sb.append(" where o.oid=:oid");

        Query query = session.createQuery(sb.toString());
        query.setParameter("version", objectToMerge.getVersion());
        query.setParameter("oid", objectToMerge.getOid());
        query.setParameter("fullObject", objectToMerge.getFullObject());

        for (QueryParameter param : queryParameters.values()) {
            query.setParameter(param.getParamName(), param.getValue());
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
}
