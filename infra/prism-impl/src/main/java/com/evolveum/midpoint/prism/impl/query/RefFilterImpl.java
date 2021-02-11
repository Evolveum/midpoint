/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class RefFilterImpl extends ValueFilterImpl<PrismReferenceValue, PrismReferenceDefinition> implements RefFilter {
    private static final long serialVersionUID = 1L;

    /**
     * By default null OID means to match any value (no additional condition).
     * Value {@code false} means to match only refs where OID is {@code null}.
     * False is ignored by legacy SQL repo, but supported by Sqale repo.
     */
    private boolean oidNullAsAny = true;

    /**
     * By default null target type means to match any value (no additional condition).
     * Value {@code false} means to match only refs where type is {@code null}.
     * False is ignored by legacy SQL repo, but supported by Sqale repo.
     */
    private boolean targetTypeNullAsAny = true;

    private RefFilterImpl(@NotNull ItemPath fullPath, @Nullable PrismReferenceDefinition definition,
            @Nullable List<PrismReferenceValue> values, @Nullable ExpressionWrapper expression) {
        super(fullPath, definition, null, values, expression, null, null);
    }

    public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, Collection<PrismReferenceValue> values) {
        return new RefFilterImpl(path, definition, values != null ? new ArrayList<>(values) : null, null);
    }

    public static RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression) {
        return new RefFilterImpl(path, definition, null, expression);
    }

    @Override
    public RefFilterImpl clone() {
        return new RefFilterImpl(getFullPath(), getDefinition(), getClonedValues(), getExpression());
    }

    @Override
    protected String getFilterName() {
        return "REF";
    }

    @Override
    public boolean match(PrismContainerValue objectValue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
        Collection<PrismValue> objectItemValues = getObjectItemValues(objectValue);
        Collection<? extends PrismValue> filterValues = emptyIfNull(getValues());
        if (objectItemValues.isEmpty()) {
            return filterValues.isEmpty();
        }
        for (PrismValue filterValue : filterValues) {
            checkPrismReferenceValue(filterValue);
            for (PrismValue objectItemValue : objectItemValues) {
                checkPrismReferenceValue(objectItemValue);
                if (valuesMatch(((PrismReferenceValue) filterValue), (PrismReferenceValue) objectItemValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkPrismReferenceValue(Object value) {
        if (!(value instanceof PrismReferenceValue)) {
            throw new IllegalArgumentException("Not supported prism value for ref filter."
                    + " It must be an instance of PrismReferenceValue but it is " + value.getClass());
        }
    }

    private boolean valuesMatch(PrismReferenceValue filterValue, PrismReferenceValue objectValue) {
        if (!matchOid(filterValue.getOid(), objectValue.getOid())) {
            return false;
        }
        if (!QNameUtil.match(PrismConstants.Q_ANY, filterValue.getRelation())) {
            // similar to relation-matching code in PrismReferenceValue (but awkward to unify, so keeping separate)
            PrismContext prismContext = getPrismContext();
            QName objectRelation = objectValue.getRelation();
            QName filterRelation = filterValue.getRelation();
            if (prismContext != null) {
                if (objectRelation == null) {
                    objectRelation = prismContext.getDefaultRelation();
                }
                if (filterRelation == null) {
                    filterRelation = prismContext.getDefaultRelation();
                }
            }
            if (!QNameUtil.match(filterRelation, objectRelation)) {
                return false;
            }
        }
        return matchTargetType(filterValue.getTargetType(), objectValue.getTargetType());
    }

    private boolean matchOid(String filterOid, String objectOid) {
        return oidNullAsAny && filterOid == null || Objects.equals(objectOid, filterOid);
    }

    private boolean matchTargetType(QName filterType, QName objectType) {
        return targetTypeNullAsAny && filterType == null || QNameUtil.match(objectType, filterType);

    }

    @Override
    public boolean equals(Object obj, boolean exact) {
        return obj instanceof RefFilter && super.equals(obj, exact);
    }

    @Override
    public void setOidNullAsAny(boolean oidNullAsAny) {
        checkMutable();
        this.oidNullAsAny = oidNullAsAny;
    }

    @Override
    public void setTargetTypeNullAsAny(boolean targetTypeNullAsAny) {
        checkMutable();
        this.targetTypeNullAsAny = targetTypeNullAsAny;
    }

    @Override
    public boolean isOidNullAsAny() {
        return oidNullAsAny;
    }

    @Override
    public boolean isTargetTypeNullAsAny() {
        return targetTypeNullAsAny;
    }

    @Override
    protected void debugDump(int indent, StringBuilder sb) {
        super.debugDump(indent, sb);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "Null OID means any", oidNullAsAny, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Null target type means any", targetTypeNullAsAny, indent + 1);
        // relationNullAsAny is currently ignored anyway
    }
}
