/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class TypeFilter extends ObjectFilter {

    private static final Trace LOGGER = TraceManager.getTrace(TypeFilter.class);

    @NotNull private final QName type;
    private ObjectFilter filter;

    public TypeFilter(@NotNull QName type, ObjectFilter filter) {
        this.type = type;
        this.filter = filter;
    }

    @NotNull
    public QName getType() {
        return type;
    }

    public ObjectFilter getFilter() {
        return filter;
    }

    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    public static TypeFilter createType(QName type, ObjectFilter filter) {
        return new TypeFilter(type, filter);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public ObjectFilter clone() {
        ObjectFilter f = filter != null ? filter.clone() : null;
        return new TypeFilter(type, f);
    }

	public TypeFilter cloneEmpty() {
		return new TypeFilter(type, null);
	}

	// untested; TODO test this method
    @Override
    public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        if (value == null) {
            return false;           // just for safety
        }
        ComplexTypeDefinition definition = value.getComplexTypeDefinition();
        if (definition == null) {
            if (!(value.getParent() instanceof PrismContainer)) {
                LOGGER.trace("Parent of {} is not a PrismContainer, returning false; it is {}", value, value.getParent());
                return false;
            }
            PrismContainer container = (PrismContainer) value.getParent();
            PrismContainerDefinition pcd = container.getDefinition();
            if (pcd == null) {
                LOGGER.trace("Parent of {} has no definition, returning false", value);
                return false;
            }
            definition = pcd.getComplexTypeDefinition();
        }
        // TODO TODO TODO subtypes!!!!!!!!
        if (!QNameUtil.match(definition.getTypeName(), type)) {
            return false;
        }
        if (filter == null) {
            return true;
        } else {
            return filter.match(value, matchingRuleRegistry);
        }
    }

    @Override
	public void checkConsistence(boolean requireDefinitions) {
		if (type == null) {
			throw new IllegalArgumentException("Null type in "+this);
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
        sb.append("TYPE: ");
        sb.append(type.getLocalPart());
        sb.append('\n');
        if (filter != null) {
            sb.append(filter.debugDump(indent + 1));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeFilter that = (TypeFilter) o;

        if (!type.equals(that.type)) return false;
        if (filter != null ? !filter.equals(that.filter, exact) : that.filter != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
		sb.append("TYPE(");
		sb.append(PrettyPrinter.prettyPrint(type));
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
}
