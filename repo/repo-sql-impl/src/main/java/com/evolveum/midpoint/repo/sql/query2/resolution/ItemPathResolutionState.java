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

package com.evolveum.midpoint.repo.sql.query2.resolution;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

/**
 * Describes current state in ItemPath resolution.
 *
 * We know what remains to be resolved.
 * We know the HQL item we are pointing to.
 * We know last transition - how we got here.
 *
 * This object is unmodifiable.
 *
 * @author mederly
 */
public class ItemPathResolutionState implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ItemPathResolutionState.class);

    final private ItemPath remainingItemPath;
    final private HqlDataInstance hqlDataInstance;
    final private JpaLinkDefinition lastTransition;                   // how we got here (optional)

    final private ItemPathResolver itemPathResolver;                // provides auxiliary functionality

    public ItemPathResolutionState(ItemPath pathToResolve, HqlDataInstance hqlDataInstance, ItemPathResolver itemPathResolver) {
        Validate.notNull(pathToResolve, "pathToResolve");
        Validate.notNull(hqlDataInstance, "hqlDataInstance");
        Validate.notNull(itemPathResolver, "itemPathResolver");
        this.remainingItemPath = pathToResolve;
        this.hqlDataInstance = hqlDataInstance;
        this.lastTransition = null;
        this.itemPathResolver = itemPathResolver;
    }

    public ItemPath getRemainingItemPath() {
        return remainingItemPath;
    }

    public HqlDataInstance getHqlDataInstance() {
        return hqlDataInstance;
    }

    public JpaLinkDefinition getLastTransition() {
        return lastTransition;
    }

    public ItemPathResolver getItemPathResolver() {
        return itemPathResolver;
    }

    public boolean isFinal() {
        return ItemPath.isNullOrEmpty(remainingItemPath);
    }

    /**
     * Executes transition to next state. Modifies query context by adding joins as necessary.
     *
     * Precondition: !isFinal()
     * Precondition: adequate transition exists
     *
     * @param itemDefinition Target item definition (used/required only for "any" properties)
     * @param singletonOnly Collections are forbidden
     * @return destination state - always not null
     */
    public ItemPathResolutionState nextState(ItemDefinition itemDefinition, boolean singletonOnly, PrismContext prismContext) throws QueryException {

        // special case - ".." when having previous state means returning to that state
        // used e.g. for Exists (some-path, some-conditions AND Equals(../xxx, yyy))
        //
        // This is brutal hack, to be thought again.
        if (remainingItemPath.startsWith(ParentPathSegment.class) && hqlDataInstance.getParentItem() != null) {
            return new ItemPathResolutionState(
                    remainingItemPath.tail(),
                    hqlDataInstance.getParentItem(),
                    itemPathResolver);

        }
        DataSearchResult<?> result = hqlDataInstance.getJpaDefinition().nextLinkDefinition(remainingItemPath, itemDefinition, prismContext);
        LOGGER.trace("nextLinkDefinition on '{}' returned '{}'", remainingItemPath, result != null ? result.getLinkDefinition() : "(null)");
        if (result == null) {       // sorry we failed (however, this should be caught before -> so IllegalStateException)
            throw new IllegalStateException("Couldn't find '" + remainingItemPath + "' in " + hqlDataInstance.getJpaDefinition());
        }
        JpaLinkDefinition linkDefinition = result.getLinkDefinition();
        String newHqlPath = hqlDataInstance.getHqlPath();
        if (linkDefinition.hasJpaRepresentation()) {
            if (singletonOnly && linkDefinition.isMultivalued()) {
                throw new QueryException("Collections are not allowable for right-side paths nor for dereferencing");     // TODO better message + context
            }
            if (!linkDefinition.isEmbedded() || linkDefinition.isMultivalued()) {
                LOGGER.trace("Adding join for '{}' to context", linkDefinition);
                newHqlPath = itemPathResolver.addJoin(linkDefinition, hqlDataInstance.getHqlPath());
            } else {
                newHqlPath += "." + linkDefinition.getJpaName();
            }
        }
        HqlDataInstance<?> parentDataInstance;
		if (!remainingItemPath.startsWith(ParentPathSegment.class)) {
			// TODO what about other special cases? (@, ...)
			parentDataInstance = hqlDataInstance;
		} else {
			parentDataInstance = null;
		}
		return new ItemPathResolutionState(
                result.getRemainder(),
                new HqlDataInstance<>(newHqlPath, result.getTargetDefinition(), parentDataInstance),
                itemPathResolver);
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    public String debugDumpNoParent() {
        return debugDump(0, false);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, true);
    }

    public String debugDump(int indent, boolean showParent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ItemPathResolutionState:\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Remaining path: ").append(remainingItemPath).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Last transition: ").append(lastTransition).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("HQL data item:\n").append(hqlDataInstance.debugDump(indent + 2, showParent));
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ItemPathResolutionState{" +
                "remainingItemPath=" + remainingItemPath +
                ", hqlDataInstance='" + hqlDataInstance + '\'' +
                '}';
    }

}
