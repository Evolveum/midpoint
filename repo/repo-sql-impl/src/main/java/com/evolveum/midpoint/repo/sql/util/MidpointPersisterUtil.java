/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

/**
 * Hibernate seems to load lazy ManyToOne associations when merge() or whole object fetch (via Criteria API) is called.
 * For "target" property in REmbeddedReference, RObjectReference and RCertCaseReference this is very unfortunate,
 * as it yields fetching the while RObject instance, which means joining approx. 20 tables - a huge overhead.
 *
 * I've found no easy way how to prevent this. One almost-working attempt was to declare the field as lazy
 * in the "no-proxy" way. However, it has 2 drawbacks:
 * 1) there are still SELECTs executed in such situations (perhaps one per each association)
 * 2) it required build-time instrumentation, which failed on RObject for unclear reasons
 * Even if #2 would be solved, problem #1 is still there, leading to performance penalties.
 *
 * So here is the hack.
 *
 * We provide a custom persister - for every entity that has any references, even if embedded via REmbeddedReference.
 * It simply erases any traces of "misbehaving" ManyToOne association values, replacing them by null values.
 * Of course, this means that such values will not be accessible within hydrated POJOs. But we don't care: we
 * do not need them there. The only reason of their existence is to allow their retrieval via HQL queries.
 *
 * Drawbacks/limitations:
 * 1) As mentioned, association values cannot be retrieved from POJOs. Only by SQL/HQL.
 * 2) Custom persister has to be manually declared on all entities that directly or indirectly contain references;
 *    otherwise the performance when getting/merging such entities will be (silently) degraded.
 * 3) When declaring the persister, one has to correctly determine its type (Joined/SingleTable one).
 *    One can help himself by looking at which persister is used by hibernate itself
 *    (e.g. sessionFactoryImpl.getEntityPersisters).
 * 4) Current implementation is quite simple minded in that it expect that the name of association is
 *    "target". This might collide with other ManyToOne associations with that name.
 * 5) It is unclear if this solution would work with persisters with batch loading enabled. Fortunately,
 *    we currently don't use this feature in midPoint.
 *
 * Alternative solutions:
 * 1) "no-proxy" as mentioned above (results in useless SELECTs + requires build time instrumentation)
 * 2) Elimination of associations altogether. It possible to fetch targets in HQL even without them by
 *    simply listing more (unrelated) classes in FROM clause, but this leads to inner, not left outer joins.
 *    The effect is that targets with null or non-existent OIDs are not included in the result.
 *    Explicit joins for unrelated classes cannot be used for now (https://hibernate.atlassian.net/browse/HHH-16).
 * 3) Using "target" associations only for references that need them. This feature is currently used only
 *    for certifications, so it would be possible to create RResolvableEmbeddedReference and use it for
 *    certification case entity. (Plus at some other places.) The performance degradation would be limited
 *    to these cases only. Also, other modules would not be able to employ displaying/selecting/ordering by
 *    e.g. referenced object names (or other properties).
 *
 * If this hack would work, it could be kept here. If not, alternative #3 should be employed. After HHH-16 is
 * provided, we should implement alternative #2.
 *
 * @author mederly
 */
public class MidpointPersisterUtil {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointPersisterUtil.class);

    public static void killUnwantedAssociationValues(String[] propertyNames, Type[] propertyTypes, Object[] values) {
        killUnwantedAssociationValues(propertyNames, propertyTypes, values, 0);
    }

    private static void killUnwantedAssociationValues(String[] propertyNames, Type[] propertyTypes, Object[] values, int depth) {
        if (values == null) {
            return;
        }

        for (int i = 0; i < propertyTypes.length; i++) {
            String name = propertyNames[i];
            Type type = propertyTypes[i];
            Object value = values[i];
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{}- killUnwantedAssociationValues processing #{}: {} (type={}, value={})",
                        StringUtils.repeat("  ", depth), i, name, type, value);
            }
            if (type instanceof ComponentType) {
                ComponentType componentType = (ComponentType) type;
                killUnwantedAssociationValues(componentType.getPropertyNames(), componentType.getSubtypes(), (Object[]) value, depth+1);
            } else if (type instanceof ManyToOneType) {
                if ("target".equals(name)) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("{}- killUnwantedAssociationValues KILLED #{}: {} (type={}, value={})",
                                StringUtils.repeat("  ", depth), i, name, type, value);
                    }
                    values[i] = null;
                }
            }
        }
    }
}
