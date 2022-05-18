/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.List;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 *
 * Special private implementation of RefFilter intended to reuse all already existing
 * logic in {@link RefItemFilterProcessor} for use of {@link ReferencedByFilterProcessor}.
 *
 * This implementation only supports relation, item path and instead of value, it carries
 * QueryDSL coordinates of referenced object.
 *
 */
class RefFilterWithRepoPath implements RefFilter {

    private static final long serialVersionUID = 1L;
    private ItemPath path;
    private QName relation;
    private UuidPath oidPath;

    public RefFilterWithRepoPath(ItemPath path2, QName relation, UuidPath oid) {
        this.path = path2;
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
        return null;
    }

    @Override
    public @NotNull ItemName getElementName() {
        return null;
    }

    @Override
    public @Nullable PrismReferenceDefinition getDefinition() {
        return null;
    }

    @Override
    public void setDefinition(@Nullable PrismReferenceDefinition definition) {
        // TODO Auto-generated method stub

    }

    @Override
    public @Nullable QName getMatchingRule() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMatchingRule(@Nullable QName matchingRule) {
        // TODO Auto-generated method stub

    }

    @Override
    public @Nullable List<PrismReferenceValue> getValues() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @Nullable PrismReferenceValue getSingleValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setValue(PrismReferenceValue value) {
        // TODO Auto-generated method stub

    }

    @Override
    public @Nullable ExpressionWrapper getExpression() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setExpression(@Nullable ExpressionWrapper expression) {
        // TODO Auto-generated method stub

    }

    @Override
    public @Nullable ItemPath getRightHandSidePath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRightHandSidePath(@Nullable ItemPath rightHandSidePath) {
        // TODO Auto-generated method stub

    }

    @Override
    public @Nullable ItemDefinition getRightHandSideDefinition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRightHandSideDefinition(@Nullable ItemDefinition rightHandSideDefinition) {
        // TODO Auto-generated method stub

    }

    @Override
    public ItemPath getPath() {
        return path;
    }

    @Override
    public boolean isRaw() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean match(PrismContainerValue cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        return false;
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        return false;
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {

    }

    @Override
    public void accept(Visitor visitor) {

    }

    @Override
    public void revive(PrismContext prismContext) {

    }

    @Override
    public String debugDump(int indent) {
        return null;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public void freeze() {

    }

    @Override
    public PrismContext getPrismContext() {
        return null;
    }

    @Override
    public RefFilter clone() {
        return null;
    }

    @Override
    public void setOidNullAsAny(boolean oidNullAsAny) {

    }

    @Override
    public void setTargetTypeNullAsAny(boolean targetTypeNullAsAny) {

    }

    @Override
    public boolean isOidNullAsAny() {
        return false;
    }

    @Override
    public boolean isTargetTypeNullAsAny() {
        return true;
    }




}
