/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.util.MiscUtil.getDiagInfo;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.SelectorProcessingContext;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class FilterClause extends SelectorClause {

    /** Immutable. */
    @NotNull private final ObjectFilter filter;

    private FilterClause(@NotNull ObjectFilter filter) {
        this.filter = filter;
        filter.freeze();
    }

    static @NotNull FilterClause of(@Nullable QName typeName, @NotNull SearchFilterType filter) throws ConfigurationException {
        ItemDefinition<?> itemDef = getOrCreateItemDefinition(
                Objects.requireNonNullElse(typeName, ObjectType.COMPLEX_TYPE));
        ObjectFilter parsedFilter;
        try {
            parsedFilter = PrismContext.get().getQueryConverter().parseFilter(filter, itemDef);
        } catch (SchemaException e) {
            throw new ConfigurationException("Couldn't parse filter in object selector: " + e.getMessage(), e);
        }
        if (parsedFilter != null) {
            ObjectTypeUtil.normalizeFilter(parsedFilter, SchemaService.get().relationRegistry());
            ObjectQueryUtil.assertPropertyOnly(parsedFilter, "Filter is not property-only filter");
            return new FilterClause(parsedFilter);
        } else {
            // Should not occur but ...
            return new FilterClause(FilterCreationUtil.createAll());
        }
    }

    /** Ugly hacking. */
    private static @NotNull ItemDefinition<?> getOrCreateItemDefinition(@NotNull QName typeName) throws ConfigurationException {
        var itemDef = PrismContext.get().getSchemaRegistry().findItemDefinitionByType(typeName);
        if (itemDef != null) {
            return itemDef;
        }
        if (QNameUtil.match(ObjectReferenceType.COMPLEX_TYPE, typeName)) {
            return PrismContext.get().definitionFactory().createReferenceDefinition(SchemaConstants.C_VALUE, typeName);
        }
        var typeDef = PrismContext.get().getSchemaRegistry().findTypeDefinitionByType(typeName);
        if (typeDef != null) {
            if (typeDef instanceof ComplexTypeDefinition) {
                return PrismContext.get().definitionFactory().createContainerDefinition(
                        SchemaConstants.C_VALUE, (ComplexTypeDefinition) typeDef);
            } else {
                return PrismContext.get().definitionFactory().createPropertyDefinition(SchemaConstants.C_VALUE, typeName);
            }
        } else {
            throw new ConfigurationException("No definition for type name '" + typeName + "'");
        }
    }

    @Override
    public @NotNull String getName() {
        return "filter";
    }

    @Override
    public boolean matches(
            @NotNull PrismValue value,
            @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!(value instanceof PrismContainerValue<?>)) {
            // This is because of filter limitations;
            // TODO we should support application of filters to reference values (and probably property values as well)
            throw new UnsupportedOperationException(String.format(
                    "Object selector with filter cannot be used for values other than container ones: %s",
                    getDiagInfo(value)));
        }
        PrismContainerValue<?> pcv = (PrismContainerValue<?>) value;
        try {
            ObjectFilter evaluatedFilter = getEvaluatedFilter(ctx);
            if (evaluatedFilter.match(pcv, SchemaService.get().matchingRuleRegistry())) {
                return true;
            }
            if (ctx.tracer.isEnabled()) {
                traceNotApplicable(ctx, "object %s", getDiagInfo(value));
            }
            return false;
        } catch (SchemaException ex) {
            throw new SchemaException(String.format(
                    "Could not apply a filter to %s: %s", getDiagInfo(value), ex.getMessage()), ex);
        }
    }

    private ObjectFilter getEvaluatedFilter(@NotNull SelectorProcessingContext ctx)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return ctx.filterEvaluator != null ? ctx.filterEvaluator.evaluate(filter) : filter;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ObjectFilter conjunct = getEvaluatedFilter(ctx);
        if (conjunct != null) {
            ObjectQueryUtil.assertNotRaw(
                    conjunct,
                    "Filter in authorization object has undefined items."
                            + " Maybe a 'type' specification is missing in the authorization?");
            ObjectQueryUtil.assertPropertyOnly(
                    conjunct, "Filter in authorization object is not property-only filter");
        } else {
            // TODO what to do here?
        }
        addConjunct(ctx, conjunct);
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "filter", filter, indent + 1);
    }
}
