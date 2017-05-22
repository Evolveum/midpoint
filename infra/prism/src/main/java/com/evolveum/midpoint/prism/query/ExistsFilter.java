/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * TODO think about creating abstract ItemFilter (ItemRelatedFilter) for this filter and ValueFilter.
 *
 * @author lazyman
 * @author mederly
 */
public class ExistsFilter extends ObjectFilter implements ItemFilter {

	@NotNull private final ItemPath fullPath;
    private ItemDefinition definition;
    private ObjectFilter filter;

    public ExistsFilter(@NotNull ItemPath fullPath, ItemDefinition definition, ObjectFilter filter) {
        this.fullPath = fullPath;
        this.definition = definition;
        this.filter = filter;
        checkConsistence(true);
    }

    @NotNull
    @Override
	public ItemPath getFullPath() {
        return fullPath;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public ObjectFilter getFilter() {
        return filter;
    }

	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}

	public static <C extends Containerable> ExistsFilter createExists(ItemPath itemPath, PrismContainerDefinition<C> containerDef,
                                                                      ObjectFilter filter) throws SchemaException {
        ItemDefinition itemDefinition = FilterUtils.findItemDefinition(itemPath, containerDef);
        return new ExistsFilter(itemPath, itemDefinition, filter);
    }

    public static <C extends Containerable> ExistsFilter createExists(ItemPath itemPath, Class<C> clazz, PrismContext prismContext,
                                                                      ObjectFilter filter) {
        ItemDefinition itemDefinition = FilterUtils.findItemDefinition(itemPath, clazz, prismContext);
        return new ExistsFilter(itemPath, itemDefinition, filter);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
	@Override
    public ObjectFilter clone() {
        ObjectFilter f = filter != null ? filter.clone() : null;
        return new ExistsFilter(fullPath, definition, f);
    }

	public ExistsFilter cloneEmpty() {
		return new ExistsFilter(fullPath, definition, null);
	}

	@Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        throw new UnsupportedOperationException();
    }
    
    @Override
	public void checkConsistence(boolean requireDefinitions) {
		if (fullPath.isEmpty()) {
			throw new IllegalArgumentException("Null or empty path in "+this);
		}
        if (requireDefinitions && definition == null) {
            throw new IllegalArgumentException("Null definition in "+this);
        }
		// null subfilter is legal. It means "ALL".
		if (filter != null) {
			filter.checkConsistence(requireDefinitions);
		}
	}

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("EXISTS: ");
        sb.append(fullPath);
        sb.append('\n');
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("DEF: ");
        if (getDefinition() != null) {
            sb.append(getDefinition().toString());
        } else {
            sb.append("null");
        }
        if (filter != null) {
            sb.append(filter.debugDump(indent + 1));
        }

        return sb.toString();
    }

	@Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
		sb.append("EXISTS(");
		sb.append(PrettyPrinter.prettyPrint(fullPath));
		sb.append(",");
		sb.append(filter);
		sb.append(")");
		return sb.toString();
    }

    @Override
    public void accept(Visitor visitor) {
        super.accept(visitor);
        if (filter != null) {
            visitor.visit(filter);
        }
    }

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ExistsFilter that = (ExistsFilter) o;

		if (!fullPath.equals(that.fullPath, exact))
			return false;
		if (exact) {
			if (definition != null ? !definition.equals(that.definition) : that.definition != null)
				return false;
		}
		return filter != null ? filter.equals(that.filter, exact) : that.filter == null;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + (definition != null ? definition.hashCode() : 0);
		result = 31 * result + (filter != null ? filter.hashCode() : 0);
		return result;
	}
}
