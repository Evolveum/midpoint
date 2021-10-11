/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.Visitor;
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
public class TypeFilterImpl extends ObjectFilterImpl implements TypeFilter {

    private static final Trace LOGGER = TraceManager.getTrace(TypeFilter.class);

    @NotNull private final QName type;
    private ObjectFilter filter;

    public TypeFilterImpl(@NotNull QName type, ObjectFilter filter) {
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
        if (filter == this) {
            throw new IllegalArgumentException("Type filte has itself as a subfilter");
        }
        this.filter = filter;
    }

    public static TypeFilter createType(QName type, ObjectFilter filter) {
        return new TypeFilterImpl(type, filter);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public TypeFilterImpl clone() {
        ObjectFilter f = filter != null ? filter.clone() : null;
        return new TypeFilterImpl(type, f);
    }

    public TypeFilter cloneEmpty() {
        return new TypeFilterImpl(type, null);
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
            PrismContainer<?> container = (PrismContainer<?>) value.getParent();
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

        TypeFilterImpl that = (TypeFilterImpl) o;

        if (!type.equals(that.type)) return false;
        if (filter != null ? !filter.equals(that.filter, exact) : that.filter != null) return false;

        return true;
    }

    // Just to make checkstyle happy
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
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
