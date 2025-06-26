/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.error.ConfigErrorReporter;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Parsed form of {@link ObjectSelectorType} and its subtypes.
 *
 * It was created to allow easy manipulation and (eventually) better performance due to optimized data structures.
 *
 * Immutable.
 */
public class ValueSelector implements DebugDumpable, Serializable {

    /** This one is a prominent one, so we'll pull it from among other {@link #clauses}. */
    @Nullable private final TypeClause typeClause;

    /**
     * Either {@link #typeClause} or an artificial one pointing to {@link ObjectType}.
     *
     * TODO shouldn't we simply use it as a regular type clause?
     */
    @NotNull private final TypeClause effectiveTypeClause;

    /** Again, a prominent one. */
    @Nullable private final ParentClause parentClause;

    /**
     * Individual clauses, like `type`, `owner`, `filter`, and so on. Immutable.
     * To improve performance, we suggest putting easily evaluated clauses (like the {@link TypeClause}) first.
     */
    @NotNull private final List<SelectorClause> clauses;

    /** For additional information, like name, description, and so on. May be missing for generated selectors. Immutable. */
    @Nullable private final ObjectSelectorType bean;

    private ValueSelector(
            @Nullable TypeClause typeClause,
            @Nullable ParentClause parentClause,
            @NotNull List<SelectorClause> clauses,
            @Nullable ObjectSelectorType bean) {
        this.typeClause = typeClause;
        this.effectiveTypeClause =
                typeClause != null ?
                        typeClause :
                        TypeClause.ofQualified(ObjectType.COMPLEX_TYPE);
        this.parentClause = parentClause;
        this.clauses = Collections.unmodifiableList(clauses);
        this.bean = bean;
        if (bean != null) {
            bean.freeze();
        }
    }

    /** Covers all values. */
    public static @NotNull ValueSelector empty() {
        return new ValueSelector(null, null, List.of(), null);
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

        ParentClause parentClause;
        var parent = bean.getParent();
        if (parent != null) {
            parentClause = ParentClause.of(
                    ValueSelector.parse(parent),
                    configNonNull(parent.getPath(), "No path in parent selector %s", parent)
                            .getItemPath());
            clauses.add(parentClause);
        } else {
            parentClause = null;
        }

        QName type = bean.getType();
        if (type != null) {
            typeClause = TypeClause.of(type);
            checkParentPresentForSubObject(typeClause, parentClause, bean);
        } else if (parentClause != null) {
            // Here we derive type from parent clause. The (parent) type + path must uniquely identify a (child) type.
            TypeDefinition parentTypeDef = parentClause.getParentSelector()
                    .getEffectiveTypeClause()
                    .getTypeDefinitionRequired();
            if (parentTypeDef instanceof ComplexTypeDefinition parentCtd) {
                ItemPath childPath = parentClause.getPath();
                ItemDefinition<?> childDef = parentCtd.findItemDefinition(childPath);
                configNonNull(childDef, "No definition of '%s' in %s", childPath, parentTypeDef);
                typeClause = TypeClause.of(childDef.getTypeName());
            } else {
                throw new ConfigurationException("Parent type " + parentTypeDef + " is not a complex type");
            }
        } else {
            typeClause = null;
        }
        if (typeClause != null) {
            clauses.add(typeClause);
        }

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

        if (bean instanceof SubjectedObjectSelectorType sBean) {

            var orgRelation = sBean.getOrgRelation();
            if (orgRelation != null) {
                clauses.add(OrgRelationClause.of(orgRelation));
            }

            var roleRelation = sBean.getRoleRelation();
            if (roleRelation != null) {
                clauses.add(RoleRelationClause.of(roleRelation));
            }

            for (SpecialObjectSpecificationType special : new HashSet<>(sBean.getSpecial())) {
                clauses.add(
                        switch (special) {
                            case SELF -> SelfClause.object();
                            case SELF_DEPUTY_ASSIGNMENT -> SelfClause.deputyAssignment();
                            case SELF_DEPUTY_REF -> SelfClause.deputyReference();
                        });

                if (filter != null
                        || orgRef != null
                        || orgRelation != null
                        || roleRelation != null
                        || (bean instanceof OwnedObjectSelectorType oBean && oBean.getTenant() != null)
                        || !archetypeRefList.isEmpty()) {
                    throw new ConfigurationException(String.format(
                            "Both filter/org/role/archetype/tenant and special clause specified in %s",
                            ConfigErrorReporter.describe(bean)));
                }
            }
        }

        if (bean instanceof OwnedObjectSelectorType oBean) {

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

            var candidateAssignee = oBean.getCandidateAssignee();
            if (candidateAssignee != null) {
                clauses.add(
                        CandidateAssigneeClause.of(
                                ValueSelector.parse(candidateAssignee)));
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

        return new ValueSelector(typeClause, parentClause, clauses, bean);
    }

    /** We don't allow sub-object types without exact `parent` clause. */
    private static void checkParentPresentForSubObject(
            @NotNull TypeClause typeClause, ParentClause parentClause, ObjectSelectorType bean) throws ConfigurationException {
        if (ObjectType.class.isAssignableFrom(typeClause.getTypeClass())) {
            return;
        }
        if (parentClause == null) {
            throw new ConfigurationException(
                    "No parent clause for sub-object type %s in %s".formatted(
                            typeClause.getTypeName(), ConfigErrorReporter.describe(bean)));
        }
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

    public @Nullable QName getTypeName() {
        return typeClause != null ? typeClause.getTypeName() : null;
    }

    @NotNull TypeClause getEffectiveTypeClause() {
        return effectiveTypeClause;
    }

    public @NotNull List<SelectorClause> getClauses() {
        return clauses;
    }

    public @Nullable ObjectSelectorType getBean() {
        return bean;
    }

    /** Returns `true` if the `value` matches this selector. */
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ctx.traceMatchingStart(this, value);
        for (SelectorClause clause : clauses) {
            if (!ctx.isFullInformationAvailable() && clause.requiresFullInformation()) {
                ctx.traceClauseNotApplicable(clause, "requires full information but it's (potentially) not available");
                continue;
            }
            if (!clause.matches(value, ctx)) {
                ctx.traceMatchingEnd(this, value, false);
                return false;
            }
        }
        ctx.traceMatchingEnd(this, value, true);
        return true;
    }

    /**
     * Converts the clause into {@link ObjectFilter}. If not applicable, returns `none` filter.
     */
    public ObjectFilter computeFilter(@NotNull FilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (toFilter(ctx)) {
            return ctx.getFilterCollector().getFilter();
        } else {
            return FilterCreationUtil.createNone();
        }
    }

    /**
     * Converts the selector into {@link ObjectFilter} (passed to {@link FilteringContext#filterCollector}).
     * Returns `false` if the selector is not applicable to given situation.
     */
    public boolean toFilter(@NotNull FilteringContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ctx.traceFilterProcessingStart(this);
        for (SelectorClause clause : clauses) {
            if (!clause.toFilter(ctx)) {
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

    public @NotNull Class<?> getEffectiveType() {
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

    public boolean isSubObject() {
        return !ObjectTypeUtil.isObjectable(getEffectiveType());
    }

    @SuppressWarnings({ "WeakerAccess", "BooleanMethodIsAlwaysInverted" })
    public boolean isPureSelf() {
        return clauses.size() == 1 && clauses.get(0) instanceof SelfClause;
    }

    public boolean isParentLess() {
        return parentClause == null;
    }
}
