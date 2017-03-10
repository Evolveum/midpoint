/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

/**
 * Defines piece of JPA data - entity, property, reference, or "any" container. Used to convert ItemPath to HQL query,
 * or, specifically, to a property path with left outer joins where appropriate.
 *
 * The conversion works like running state machine where data definitions are states, and transitions are labeled
 * with non-empty ItemPaths. Input paths are used to navigate through states until all of input path is consumed.
 *
 * In addition to recognize input paths, this automaton produces HQL path and property joins. That's why
 * each possible transition is labeled with (ItemPath prefix, JPA name, other transition data) tuple.
 * ItemPath prefix is used to match the input path, while JPA name + other transition data, along
 * with target state information (potentially) are used to generate HQL property path with appropriate join,
 * if necessary.
 *
 * Note that some transitions may have empty JPA name - when the data is contained directly in owner entity
 * (e.g. object extension, shadow attributes). Most transitions have single item paths. However, some have two,
 * e.g. construction/resourceRef, owner/id, metadata/*.
 *
 * By other transition data we currently mean: collection specification, or "embedded" flag.
 *
 * Terminology:
 *  - state ~ data node (JpaDataNodeDefinition -> JpaEntityDefinition, JpaPropertyDefinition, ...)
 *  - transition ~ link node (JpaLinkDefinition)
 *
 * @author mederly
 */
public abstract class JpaDataNodeDefinition implements DebugDumpable, Visitable {

    private static final Trace LOGGER = TraceManager.getTrace(JpaDataNodeDefinition.class);

    /**
     * JPA class - either "composite" (RObject, RUser, RAssignment, ...) or "primitive" (String, Integer, int, ...)
     */
    private Class jpaClass;

    /**
     * JAXB class - either "composite" (ObjectType, UserType, AssignmentType, ...) or "primitive" (String, Integer, int, ...)
     * Null if not known.
     */
    private Class jaxbClass;

    public JpaDataNodeDefinition(Class jpaClass, Class jaxbClass) {
        Validate.notNull(jpaClass, "jpaClass");
        this.jpaClass = jpaClass;
        this.jaxbClass = jaxbClass;
    }

    public Class getJpaClass() {
        return jpaClass;
    }

    public String getJpaClassName() {
        return jpaClass.getSimpleName();
    }

    public Class getJaxbClass() {
        return jaxbClass;
    }

    /**
     * Tries to find "next step" in the translation process for a given ItemPath.
     *
     * @param path A path to be resolved. Always non-null and non-empty. Should produce at least one transition.
     * @param itemDefinition Item definition for the item being sought. Needed only for "any" items.
     * @param prismContext
	 * @return
     * - Normally it returns the search result containing next item definition (entity, collection, ...) in the chain
     *   and the unresolved remainder of the path. The transition may be empty ("self") e.g. for metadata or construction.
     * - If the search was not successful, returns null.
     *
     */
    public abstract DataSearchResult<JpaDataNodeDefinition> nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition,
			PrismContext prismContext) throws QueryException;

    public String toString() {
        return getShortInfo();
    }

    protected abstract String getDebugDumpClassName();

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    public String getShortInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDebugDumpClassName()).append(':').append(getJpaClassName());
        if (jaxbClass != null) {
            sb.append(" (jaxb=").append(jaxbClass.getSimpleName()).append(")");
        }
        return sb.toString();
    }
}
