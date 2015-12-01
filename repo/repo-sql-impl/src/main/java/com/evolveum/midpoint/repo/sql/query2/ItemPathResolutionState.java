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

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaAnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

/**
 * Describes current state in ItemPath resolution.
 *
 * We know what we've already resolved, and what remains to be done.
 * We know which data element we're pointing to (entity, property, reference, any) and which JPA expression we can use to address it.
 * We know last transition - how we got here.
 *
 * We also remember previous resolution state, in order to be able to go back via ".." path segment.
 *
 * @author mederly
 */
public class ItemPathResolutionState implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ItemPathResolutionState.class);

    private ItemPath remainingItemPath;

    private String currentHqlPath;
    private JpaDataNodeDefinition currentJpaNode;               // where we are
    private JpaLinkDefinition lastTransition;                   // how we got here (optional)

    private ItemPathResolutionState previousState;              // from which state we got here (optional)

    private ItemPathResolver itemPathResolver;                // provides auxiliary functionality

    public ItemPathResolutionState(ItemPath pathToResolve, String startingHqlPath, JpaEntityDefinition baseEntityDefinition, ItemPathResolver itemPathResolver) {
        Validate.notNull(pathToResolve, "pathToResolve");
        Validate.notNull(startingHqlPath, "startingHqlPath");
        Validate.notNull(baseEntityDefinition, "baseEntityDefinition");
        Validate.notNull(itemPathResolver, "itemPathResolver");
        this.remainingItemPath = pathToResolve;
        this.currentHqlPath = startingHqlPath;
        this.currentJpaNode = baseEntityDefinition;
        this.lastTransition = null;
        this.previousState = null;
        this.itemPathResolver = itemPathResolver;
    }

    // no validation as this is private
    private ItemPathResolutionState(ItemPath remainingItemPath, String currentHqlPath, JpaDataNodeDefinition currentJpaNode, JpaLinkDefinition lastTransition, ItemPathResolutionState previousState, ItemPathResolver itemPathResolver) {
        this.remainingItemPath = remainingItemPath;
        this.currentHqlPath = currentHqlPath;
        this.currentJpaNode = currentJpaNode;
        this.lastTransition = lastTransition;
        this.previousState = previousState;
        this.itemPathResolver = itemPathResolver;
    }

    public ItemPath getRemainingItemPath() {
        return remainingItemPath;
    }

    public String getCurrentHqlPath() {
        return currentHqlPath;
    }

    public JpaDataNodeDefinition getCurrentJpaNode() {
        return currentJpaNode;
    }

    public JpaLinkDefinition getLastTransition() {
        return lastTransition;
    }

    public ItemPathResolutionState getPreviousState() {
        return previousState;
    }

    public boolean hasPreviousState() {
        return previousState != null;
    }

    public boolean isFinal() {
        return ItemPath.isNullOrEmpty(remainingItemPath) || currentJpaNode instanceof JpaAnyDefinition;
    }

    /**
     * Precondition: !isFinal()
     * Postcondition: non-null state
     * @param singletonOnly Collections are forbidden
     */
    public ItemPathResolutionState nextState(boolean singletonOnly) throws QueryException {
        DataSearchResult<JpaDataNodeDefinition> result = currentJpaNode.nextLinkDefinition(remainingItemPath);
        LOGGER.trace("nextLinkDefinition on '{}' returned '{}'", remainingItemPath, result != null ? result.getLinkDefinition() : "(null)");
        if (result == null) {       // sorry we failed (however, this should be caught before -> so IllegalStateException)
            throw new IllegalStateException("Couldn't find " + remainingItemPath + " in " + currentJpaNode);
        }
        JpaLinkDefinition linkDefinition = result.getLinkDefinition();
        String newHqlPath = currentHqlPath;
        if (linkDefinition.hasJpaRepresentation()) {
            if (singletonOnly && linkDefinition.isMultivalued()) {
                throw new QueryException("Collections are not allowable for right-side paths");     // TODO better message + context
            }
            if (!linkDefinition.isEmbedded() || linkDefinition.isMultivalued()) {
                LOGGER.trace("Adding join for '{}' to context", linkDefinition);
                newHqlPath = itemPathResolver.addJoin(linkDefinition, currentHqlPath);
            } else {
                newHqlPath += "." + linkDefinition.getJpaName();
            }
        }
        ItemPathResolutionState next = new ItemPathResolutionState(
                result.getRemainder(),
                newHqlPath,
                result.getTargetDefinition(),
                result.getLinkDefinition(),
                this,
                itemPathResolver);
        return next;
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
        sb.append("ItemPathResolutionState: Remaining path: ").append(remainingItemPath).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Current HQL path: ").append(currentHqlPath).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Current JPA data node: ").append(currentJpaNode).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Last transition: ").append(lastTransition).append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Previous state: ");
        if (previousState != null) {
            if (showParent) {
                sb.append("\n");
                sb.append(previousState.debugDump(indent + 2));
            } else {
                sb.append(previousState).append("\n");
            }
        } else {
            sb.append("(null)\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ItemPathResolutionState{" +
                "remainingItemPath=" + remainingItemPath +
                ", currentHqlPath='" + currentHqlPath + '\'' +
                ", currentJpaNode=" + currentJpaNode +
                '}';
    }

}
