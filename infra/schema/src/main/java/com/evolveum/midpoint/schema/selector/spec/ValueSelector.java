/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Parsed form of {@link ObjectSelectorType} and its subtypes.
 *
 * It was created to allow easy manipulation and (eventually) better performance due to optimized data structures.
 */
public class ValueSelector implements DebugDumpable {

    /** This one is a prominent one, so we'll pull it from among other {@link #clauses}. */
    @Nullable private final TypeClause typeClause;

    @Nullable private final ParentClause parentClause;

    /**
     * Individual clauses, like `type`, `owner`, `filter`, and so on. Immutable.
     * To improve performance, we suggest putting easily evaluated clauses (like the {@link TypeClause}) first.
     */
    @NotNull private final List<SelectorClause> clauses;

    /** For additional information, like name, description, and so on. Immutable. */
    @Nullable private final ObjectSelectorType bean;

    private ValueSelector(
            @Nullable TypeClause typeClause,
            @Nullable ParentClause parentClause,
            @NotNull List<SelectorClause> clauses,
            @Nullable ObjectSelectorType bean) {
        this.typeClause = typeClause;
        this.parentClause = parentClause;
        this.clauses = Collections.unmodifiableList(clauses);
        this.bean = bean;
        if (bean != null) {
            bean.freeze();
        }
    }

    public static @NotNull ValueSelector empty() {
        return new ValueSelector(null, null, List.of(), null);
    }

    /** Type name must be qualified. */
    public static @NotNull ValueSelector emptyWithType(@NotNull QName typeName) {
        TypeClause typeClause = TypeClause.ofQualified(typeName);
        return new ValueSelector(typeClause, null, List.of(typeClause), null);
    }

    public static ValueSelector of(@NotNull TypeClause typeClause, @NotNull ParentClause parentClause) {
        return new ValueSelector(
                typeClause,
                parentClause,
                List.of(typeClause, parentClause),
                null);
    }

    public static ValueSelector of(SelectorClause... clauses) {
        var clausesList = List.of(clauses);
        return new ValueSelector(
                SelectorClause.getSingle(clausesList, TypeClause.class),
                SelectorClause.getSingle(clausesList, ParentClause.class),
                clausesList,
                null);
    }

    /** The bean is frozen during the parsing process. */
    public static @NotNull ValueSelector parse(@NotNull ObjectSelectorType bean) throws ConfigurationException {

        TypeClause typeClause;
        var clauses = new ArrayList<SelectorClause>();

        QName type = bean.getType();
        if (type != null) {
            typeClause = TypeClause.of(type);
            clauses.add(typeClause);
        } else {
            typeClause = null;
        }

        // Temporarily allowed, just to make tests pass
        //configCheck(subtype == null, "Subtype specification is not allowed");
        String subtype = bean.getSubtype();
        if (subtype != null) {
            clauses.add(SubtypeClause.of(subtype));
        }

        List<ObjectReferenceType> archetypeRefList = bean.getArchetypeRef();
        if (!archetypeRefList.isEmpty()) {
            clauses.add(ArchetypeRefClause.of(archetypeRefList));
        }

        ObjectReferenceType orgRef = bean.getOrgRef();
        if (orgRef != null) {
            clauses.add(OrgRefClause.of(orgRef));
        }

        SearchFilterType filter = bean.getFilter();
        if (filter != null) {
            QName qTypeName = typeClause != null ? typeClause.getTypeName() : null;
            clauses.add(FilterClause.of(qTypeName, filter));
        }

        if (bean instanceof SubjectedObjectSelectorType) {
            SubjectedObjectSelectorType sBean = (SubjectedObjectSelectorType) bean;

            var orgRelation = sBean.getOrgRelation();
            if (orgRelation != null) {
                clauses.add(OrgRelationClause.of(orgRelation));
            }

            var roleRelation = sBean.getRoleRelation();
            if (roleRelation != null) {
                clauses.add(RoleRelationClause.of(roleRelation));
            }

            for (SpecialObjectSpecificationType special : new HashSet<>(sBean.getSpecial())) {
                if (special == SpecialObjectSpecificationType.SELF) {
                    clauses.add(new SelfClause());

                    if (filter != null
                            || orgRef != null
                            || orgRelation != null
                            || roleRelation != null
                            || (bean instanceof OwnedObjectSelectorType && ((OwnedObjectSelectorType) bean).getTenant() != null)
                            || !archetypeRefList.isEmpty()) {
                        throw new ConfigurationException(String.format( // TODO error location
                                "Both filter/org/role/archetype/tenant and special clause specified in %s", bean));
                    }

                } else {
                    throw new ConfigurationException("Unsupported special clause: " + special);
                }
            }
        }

        if (bean instanceof OwnedObjectSelectorType) {
            OwnedObjectSelectorType oBean = (OwnedObjectSelectorType) bean;

            var owner = oBean.getOwner();
            if (owner != null) {
                clauses.add(
                        OwnerClause.of(
                                ValueSelector.parse(owner)));
            }

            var delegator = oBean.getDelegator();
            if (delegator != null) {
                clauses.add(
                        DelegatorClause.of(
                                ValueSelector.parse(delegator),
                                Boolean.TRUE.equals(delegator.isAllowInactive())));
            }

            var requester = oBean.getRequester();
            if (requester != null) {
                clauses.add(
                        RequesterClause.of(
                                ValueSelector.parse(requester)));
            }

            var assignee = oBean.getAssignee();
            if (assignee != null) {
                clauses.add(
                        AssigneeClause.of(
                                ValueSelector.parse(assignee)));
            }

            var relatedObject = oBean.getRelatedObject();
            if (relatedObject != null) {
                clauses.add(
                        RelatedObjectClause.of(
                                ValueSelector.parse(relatedObject)));
            }

            var tenant = oBean.getTenant();
            if (tenant != null) {
                clauses.add(
                        TenantClause.of(tenant));
            }
        }

        ParentClause parentClause;
        if (bean instanceof AuthorizationObjectSelectorType) {
            var aBean = (AuthorizationObjectSelectorType) bean;

            var parent = aBean.getParent();
            if (parent != null) {
                parentClause = ParentClause.of(
                        ValueSelector.parse(parent),
                        configNonNull(parent.getPath(), "No path in parent selector %s", parent)
                                .getItemPath());
                clauses.add(parentClause);
            } else {
                parentClause = null;
            }
        } else {
            parentClause = null;
        }

        return new ValueSelector(typeClause, parentClause, clauses, bean);
    }

    public static ValueSelector forType(@NotNull QName typeName) {
        Preconditions.checkArgument(QNameUtil.isQualified(typeName));
        TypeClause typeClause;
        try {
            typeClause = TypeClause.of(typeName);
        } catch (ConfigurationException e) {
            throw SystemException.unexpected(e);
        }
        return new ValueSelector(
                typeClause,
                null,
                List.of(typeClause),
                null);
    }

    public @Nullable TypeClause getTypeClause() {
        return typeClause;
    }

    public @Nullable QName getTypeName() {
        return typeClause != null ? typeClause.getTypeName() : null;
    }

    public @NotNull List<SelectorClause> getClauses() {
        return clauses;
    }

    public @Nullable ObjectSelectorType getBean() {
        return bean;
    }

    public boolean matches(@NotNull PrismValue value, @NotNull ClauseMatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ctx.traceMatchingStart(this, value);
        for (SelectorClause clause : clauses) {
            if (!clause.matches(value, ctx)) {
                ctx.traceMatchingEnd(this, value, false);
                return false;
            }
        }
        ctx.traceMatchingEnd(this, value, true);
        return true;
    }

    public boolean applyFilters(@NotNull ClauseFilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ctx.traceFilterProcessingStart(this);
        for (SelectorClause clause : clauses) {
            if (!ctx.isClauseApplicable(clause) || !clause.applyFilter(ctx)) {
                ctx.traceFilterProcessingEnd(this, false);
                return false;
            }
        }
        ctx.getFilterCollector().nullToAll();
        ctx.traceFilterProcessingEnd(this, true);
        return true;
    }

    public @Nullable ParentClause getParentClause() {
        return parentClause;
    }

    public @NotNull Class<?> getTypeOrDefault() throws ConfigurationException {
        if (typeClause != null) {
            return typeClause.getTypeClass();
        } else {
            return ObjectType.class;
        }
    }

    public @NotNull Class<?> getTypeClass(@NotNull Class<?> superType) throws ConfigurationException {
        if (typeClause != null) {
            return typeClause.getTypeClass();
        } else {
            return superType;
        }
    }

    @Override
    public String debugDump(int indent) {
        String name = getName();
        String label = getClass().getSimpleName() + (name != null ? " " + name : "");
        var sb = DebugUtil.createTitleStringBuilder(label, indent);
        sb.append("\n");

        var description = getDescription();
        if (description != null) {
            DebugUtil.debugDumpWithLabelLn(sb, "description", description, indent + 1);
        }

        var documentation = getDocumentation();
        if (documentation != null) {
            DebugUtil.debugDumpWithLabelLn(sb, "documentation", documentation, indent + 1);
        }

        DebugUtil.debugDumpWithLabel(sb, "clauses", clauses, indent + 1);

        return sb.toString();
    }

    private Long getId() {
        return bean != null ? bean.getId() : null;
    }

    private String getName() {
        return bean != null ? bean.getName() : null;
    }

    private String getDescription() {
        return bean != null ? bean.getDescription() : null;
    }

    private String getDocumentation() {
        return bean != null ? bean.getDocumentation() : null;
    }

    public String getHumanReadableDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append("selector");
        Long id = getId();
        if (id != null) {
            sb.append(" #").append(id);
        }
        String name = getName();
        if (name != null) {
            sb.append(" '").append(name).append("'");
        }
        return sb.toString();
    }

    /**
     * Returns a selector that is the same as this one, except that the type is as specified.
     * May return the same object if types are equal.
     *
     * The type must be qualified.
     */
    public ValueSelector sameWithType(@NotNull QName newType) {
        Preconditions.checkArgument(QNameUtil.isQualified(newType));
        if (newType.equals(getTypeName())) {
            return this;
        } else {
            TypeClause newTypeClause = TypeClause.ofQualified(newType);
            return new ValueSelector(
                    newTypeClause,
                    parentClause,
                    replaceTypeClause(newTypeClause),
                    null);
        }
    }

    private List<SelectorClause> replaceTypeClause(@NotNull TypeClause newTypeClause) {
        List<SelectorClause> newList = new ArrayList<>();
        newList.add(newTypeClause); // should go first
        for (SelectorClause existingClause : clauses) {
            if (!(existingClause instanceof TypeClause)) {
                newList.add(existingClause);
            }
        }
        return newList;
    }

    /**
     * Returns a selector that is the same as this one (or even this one), except that it has no parent clause.
     */
    public ValueSelector withParentRemoved() {
        if (parentClause == null) {
            return this;
        } else {
            return new ValueSelector(
                    typeClause,
                    null,
                    clauses.stream()
                            .filter(c -> !(c instanceof ParentClause))
                            .collect(Collectors.toList()),
                    null);
        }
    }
}
