/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.PrettyPrinter;

import static com.evolveum.midpoint.util.MiscUtil.*;

public class TypeClause extends SelectorClause {

    /** Qualified. */
    @NotNull private final QName typeName;

    transient private TypeDefinition typeDefinition;

    /** Lazily evaluated. */
    private Class<?> typeClass;

    private TypeClause(@NotNull QName typeName) {
        this.typeName = typeName;
    }

    static @NotNull TypeClause of(@NotNull QName typeName) throws ConfigurationException {
        try {
            var clause = new TypeClause(
                    PrismContext.get().getSchemaRegistry().qualifyTypeName(typeName));
            configCheck(
                    clause.getTypeDefinition() != null,
                    "Unknown object type %s",
                    clause.typeName); // TODO context
            return clause;
        } catch (SchemaException e) {
            throw new ConfigurationException("Couldn't resolve unqualified type name in object selector: " + typeName, e);
        }
    }

    static @NotNull TypeClause ofQualified(@NotNull QName typeName) {
        Preconditions.checkArgument(QNameUtil.isQualified(typeName));
        return new TypeClause(typeName);
    }

    public static @NotNull TypeClause of(@NotNull Class<?> clazz) {
        return ofQualified(
                PrismContext.get().getSchemaRegistry().determineTypeForClassRequired(clazz));
    }

    public @NotNull QName getTypeName() {
        return typeName;
    }

    @Override
    public @NotNull String getName() {
        return "type";
    }

    private synchronized TypeDefinition getTypeDefinition() {
        if (typeDefinition == null) {
            typeDefinition = PrismContext.get().getSchemaRegistry().findTypeDefinitionByType(typeName);
        }
        return typeDefinition;
    }

    private @NotNull TypeDefinition getTypeDefinitionRequired() {
        return stateNonNull(getTypeDefinition(), () -> "No type definition in " + this);
    }

    @Override
    public boolean matches(
            @NotNull PrismValue value,
            @NotNull MatchingContext ctx) {
        if (!value.isOfType(typeName)) {
            if (ctx.tracer.isEnabled()) {
                traceNotApplicable(ctx, "type mismatch, expected {}, was {}",
                        PrettyPrinter.prettyPrint(typeName),
                        PrettyPrinter.prettyPrint(value.getTypeName()));
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) throws ConfigurationException {
        var filterType = ctx.getFilterType();
        var restrictedType = ctx.getRestrictedType();
        if (restrictedType.equals(filterType)) {
            traceApplicable(
                    ctx,
                    "exact match of prescribed and actual type: %s (no filter needed)",
                    restrictedType.getSimpleName());
            return true;
        } else if (filterType.isAssignableFrom(restrictedType)) {
            addConjunct(
                    ctx,
                    PrismContext.get().queryFactory().createType(typeName, null),
                    "subtype match: adding more specific type filter of %s to %s",
                    restrictedType.getSimpleName(), filterType.getSimpleName());
            return true;
        } else {
            traceNotApplicable(
                    ctx,
                    "type mismatch: constructing filter for %s, but the 'type' clause references %s",
                    filterType.getSimpleName(), restrictedType.getSimpleName());
            return false;
        }
    }

    synchronized @NotNull Class<?> getTypeClass() {
        if (typeClass == null) {
            typeClass = requireNonNull(
                    getTypeDefinitionRequired().getCompileTimeClass(),
                    () -> new UnsupportedOperationException(String.format(
                            "Only statically defined types are supported in authorizations."
                                    + "Type '%s' is not statically defined", typeName))); // TODO context
        }
        return typeClass;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append(" ").append(typeName);
    }
}
