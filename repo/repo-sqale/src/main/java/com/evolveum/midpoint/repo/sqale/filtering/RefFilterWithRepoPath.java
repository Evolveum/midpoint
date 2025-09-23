/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.TypedItemPath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Special private implementation of RefFilter intended to reuse all already existing
 * logic in {@link RefItemFilterProcessor} for use of {@link ReferencedByFilterProcessor}.
 *
 * This implementation only supports relation, item path and instead of value, it carries
 * QueryDSL coordinates of referenced object.
 */
class RefFilterWithRepoPath implements RefFilter {

    private static final long serialVersionUID = 1L;
    private final ItemPath path;
    private final QName relation;
    private final UuidPath oidPath;

    public RefFilterWithRepoPath(ItemPath path, QName relation, UuidPath oid) {
        this.path = path;
        this.relation = relation;
        this.oidPath = oid;
    }

    public QName getRelation() {
        return relation;
    }

    public UuidPath getOidPath() {
        return oidPath;
    }

    @Override
    public @NotNull ItemPath getFullPath() {
        return path;
    }

    @Override
    public @NotNull ItemPath getParentPath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ItemName getElementName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PrismReferenceDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefinition(@Nullable PrismReferenceDefinition definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable QName getMatchingRule() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMatchingRule(@Nullable QName matchingRule) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable List<PrismReferenceValue> getValues() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PrismReferenceValue getSingleValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValue(PrismReferenceValue value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ExpressionWrapper getExpression() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExpression(@Nullable ExpressionWrapper expression) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ItemPath getRightHandSidePath() {
        return null; // this must be implemented, null is OK
    }

    @Override
    public void setRightHandSidePath(@Nullable ItemPath rightHandSidePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ItemDefinition<?> getRightHandSideDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRightHandSideDefinition(@Nullable ItemDefinition<?> rightHandSideDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemPath getPath() {
        return path;
    }

    @Override
    public boolean isRaw() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean match(PrismContainerValue<?> cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(Visitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revive(PrismContext prismContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String debugDump(int indent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImmutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void freeze() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContext getPrismContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RefFilter clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOidNullAsAny(boolean oidNullAsAny) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetTypeNullAsAny(boolean targetTypeNullAsAny) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOidNullAsAny() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTargetTypeNullAsAny() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void collectUsedPaths(TypedItemPath base, Consumer<TypedItemPath> pathConsumer, boolean expandReferences) {
        // NOOP - never used outside repo.
    }
}
