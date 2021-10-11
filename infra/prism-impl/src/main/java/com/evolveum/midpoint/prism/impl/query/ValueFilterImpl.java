/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class ValueFilterImpl<V extends PrismValue, D extends ItemDefinition> extends ObjectFilterImpl implements
        ValueFilter<V, D> {
    private static final long serialVersionUID = 1L;

    @NotNull private final ItemPath fullPath;
    // This is a definition of the item pointed to by "fullPath"
    // (not marked as @NotNull, because it can be filled-in after creation of the filter - e.g. in provisioning)
    @Nullable private D definition;
    @Nullable private QName matchingRule;
    @Nullable private List<V> values;
    @Nullable private ExpressionWrapper expression;
    @Nullable private ItemPath rightHandSidePath;                            // alternative to values/expression; can be provided later
    @Nullable private ItemDefinition rightHandSideDefinition;                // optional (needed only if path points to dynamically defined item)

    // At most one of values, expression, rightHandSidePath can be non-null.
    // It is a responsibility of the client to ensure it.

    /**
     * TODO decide whether to make these fields final. It makes the code simpler, but maybe not that much
     * that it is worth the discomfort of the clients (they cannot change they if the would wish).
     * Some of them like definition, matchingRule, and right-hand things are filled-in later in some cases (provisioning, query builder).
     */
    protected ValueFilterImpl(@NotNull ItemPath fullPath, @Nullable D definition) {
        this(fullPath, definition, null, null, null, null, null);
    }

    protected ValueFilterImpl(@NotNull ItemPath fullPath, @Nullable D definition, @Nullable QName matchingRule,
            @Nullable List<V> values, @Nullable ExpressionWrapper expression,
            @Nullable ItemPath rightHandSidePath, @Nullable ItemDefinition rightHandSideDefinition) {
        Validate.isTrue(!ItemPath.isEmpty(fullPath), "path in filter is null or empty");
        this.fullPath = fullPath;
        this.definition = definition;
        this.matchingRule = matchingRule;
        this.expression = expression;
        this.values = values;
        this.rightHandSidePath = rightHandSidePath;
        this.rightHandSideDefinition = rightHandSideDefinition;
        if (values != null) {
            for (V value : values) {
                value.setParent(this);
            }
        }
        checkConsistence(false);
    }

    @NotNull
    @Override
    public ItemPath getFullPath() {
        return fullPath;
    }

    @NotNull
    public ItemPath getParentPath() {
        return fullPath.allExceptLast();
    }

    @NotNull
    public ItemName getElementName() {
        if (definition != null) {
            return definition.getItemName();        // this is more precise, as the name in path can be unqualified
        }
        if (fullPath.isEmpty()) {
            throw new IllegalStateException("Empty full path in filter " + this);
        }
        Object last = fullPath.last();
        if (ItemPath.isName(last)) {
            return ItemPath.toName(last);
        } else {
            throw new IllegalStateException("Got " + last + " as a last path segment in value filter " + this);
        }
    }

    @Nullable
    public D getDefinition() {
        return definition;
    }

    public void setDefinition(@Nullable D definition) {
        this.definition = definition;
        checkConsistence(false);
    }

    @Nullable
    public QName getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(@Nullable QName matchingRule) {
        this.matchingRule = matchingRule;
    }

    @NotNull
    MatchingRule getMatchingRuleFromRegistry(MatchingRuleRegistry matchingRuleRegistry) {
        if (definition == null) {
            throw new IllegalArgumentException("No definition in item " + fullPath);
        }
        try {
            return matchingRuleRegistry.getMatchingRule(matchingRule, definition.getTypeName());
        } catch (SchemaException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    @Nullable
    public List<V> getValues() {
        return values;
    }

    @Nullable
    List<V> getClonedValues() {
        if (values == null) {
            return null;
        } else {
            List<V> clonedValues = new ArrayList<>(values.size());
            for (V value : values) {
                @SuppressWarnings("unchecked")
                V cloned = (V) value.clone();
                clonedValues.add(cloned);
            }
            return clonedValues;
        }
    }

    @Nullable
    V getClonedValue() {
        V value = getSingleValue();
        if (value == null) {
            return null;
        } else {
            @SuppressWarnings("unchecked")
            V cloned = (V) value.clone();
            return cloned;
        }
    }

    @Nullable
    public V getSingleValue() {
        if (values == null || values.isEmpty()) {
            return null;
        } else if (values.size() > 1) {
            throw new IllegalArgumentException("Filter '" + this + "' should contain at most one value, but it has " + values.size() + " of them.");
        } else {
            return values.iterator().next();
        }
    }

    /**
     * @param value value, has to be parent-less
     */
    public void setValue(V value) {
        this.values = new ArrayList<>();
        if (value != null) {
            value.setParent(this);
            values.add(value);
        }
    }

    @Nullable
    public ExpressionWrapper getExpression() {
        return expression;
    }

    public void setExpression(@Nullable ExpressionWrapper expression) {
        this.expression = expression;
    }

    @Nullable
    public ItemPath getRightHandSidePath() {
        return rightHandSidePath;
    }

    public void setRightHandSidePath(@Nullable ItemPath rightHandSidePath) {
        this.rightHandSidePath = rightHandSidePath;
    }

    @Nullable
    public ItemDefinition getRightHandSideDefinition() {
        return rightHandSideDefinition;
    }

    public void setRightHandSideDefinition(@Nullable ItemDefinition rightHandSideDefinition) {
        this.rightHandSideDefinition = rightHandSideDefinition;
    }

    @Override
    public PrismContext getPrismContext() {
        if (super.getPrismContext() != null) {
            return super.getPrismContext();
        }
        D def = getDefinition();
        if (def != null && def.getPrismContext() != null) {
            PrismContext prismContext = def.getPrismContext();
            super.setPrismContext(prismContext);
            return prismContext;
        } else {
            return null;
        }
    }

    @Override
    public ItemPath getPath() {
        return getFullPath();
    }

    public boolean isRaw() {
        if (values != null) {
            for (V value : values) {
                if (value.isRaw()) {
                    return true;
                }
            }
        }
        return false;
    }

    // TODO revise
    @Override
    public boolean match(PrismContainerValue cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {

        Collection<PrismValue> objectItemValues = getObjectItemValues(cvalue);

        boolean filterItemIsEmpty = getValues() == null || getValues().isEmpty();
        boolean objectItemIsEmpty = objectItemValues.isEmpty();

        if (filterItemIsEmpty && !objectItemIsEmpty) {
            return false;
        }

        if (!filterItemIsEmpty && objectItemIsEmpty) {
            return false;
        }

        return true;
    }

    @NotNull
    Collection<PrismValue> getObjectItemValues(PrismContainerValue value) {
        return value.getAllValues(getFullPath());
    }

    // TODO revise
    @NotNull
    Item getFilterItem() throws SchemaException {
        if (getDefinition() == null) {
            throw new SchemaException("Could not find definition for item " + getPath());
        }
        Item filterItem = getDefinition().instantiate();
        if (getValues() != null && !getValues().isEmpty()) {
            try {
                for (PrismValue v : getValues()) {
                    filterItem.add(v.clone());
                }
            } catch (SchemaException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        return filterItem;
    }

    @NotNull
    Collection<PrismValue> getFilterItemValues() throws SchemaException {
        //noinspection unchecked
        return getFilterItem().getValues();
    }

    @Override
    public abstract ValueFilterImpl clone();

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return equals(o, true);
    }

    @Override
    public boolean equals(Object o, boolean exact) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValueFilterImpl<?, ?> that = (ValueFilterImpl<?, ?>) o;
        return fullPath.equals(that.fullPath, exact) &&
                (!exact || Objects.equals(definition, that.definition)) &&
                Objects.equals(matchingRule, that.matchingRule) &&
                MiscUtil.nullableCollectionsEqual(values, that.values) &&
                Objects.equals(expression, that.expression) &&
                (rightHandSidePath == null && that.rightHandSidePath == null ||
                        rightHandSidePath != null && rightHandSidePath.equals(that.rightHandSidePath, exact)) &&
                (!exact || Objects.equals(rightHandSideDefinition, that.rightHandSideDefinition));
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullPath, matchingRule, values, expression, rightHandSidePath);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getFilterName()).append(":");
        debugDump(indent, sb);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFilterName()).append(": ");
        return toString(sb);
    }

    protected abstract String getFilterName();

    protected void debugDump(int indent, StringBuilder sb) {
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("PATH: ");
        sb.append(getFullPath().toString());

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("DEF: ");
        if (getDefinition() != null) {
            sb.append(getDefinition().toString());
        } else {
            sb.append("null");
        }

        List<V> values = getValues();
        if (values != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("VALUE:");
            for (PrismValue val : getValues()) {
                sb.append("\n");
                sb.append(DebugUtil.debugDump(val, indent + 2));
            }
        }

        ExpressionWrapper expression = getExpression();
        if (expression != null && expression.getExpression() != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("EXPRESSION:");
            sb.append("\n");
            sb.append(DebugUtil.debugDump(expression.getExpression(), indent + 2));
        }

        if (getRightHandSidePath() != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("RIGHT SIDE PATH: ");
            sb.append(getFullPath().toString());
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("RIGHT SIDE DEF: ");
            if (getRightHandSideDefinition() != null) {
                sb.append(getRightHandSideDefinition().toString());
            } else {
                sb.append("null");
            }
        }

        QName matchingRule = getMatchingRule();
        if (matchingRule != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("MATCHING: ");
            sb.append(matchingRule);
        }
    }

    protected String toString(StringBuilder sb) {
        sb.append(getFullPath().toString());
        sb.append(",");
        if (getValues() != null) {
            for (int i = 0; i < getValues().size(); i++) {
                PrismValue value = getValues().get(i);
                if (value == null) {
                    sb.append("null");
                } else {
                    sb.append(value.toString());
                }
                if (i != getValues().size() - 1) {
                    sb.append(",");
                }
            }
        }
        if (getRightHandSidePath() != null) {
            sb.append(getRightHandSidePath());
        }
        return sb.toString();
    }

    @Override
    public void checkConsistence(boolean requireDefinitions) {
        if (requireDefinitions && definition == null) {
            throw new IllegalArgumentException("Null definition in " + this);
        }
        if (fullPath.isEmpty()) {
            throw new IllegalArgumentException("Empty path in " + this);
        }
        Object last = fullPath.last();
        if (!ItemPath.isName(last) && !ItemPath.isIdentifier(last)) {
            throw new IllegalArgumentException("Last segment of item path is not a name or identifier segment: " + fullPath + " (it is " + last + ")");
        }
        if (rightHandSidePath != null && rightHandSidePath.isEmpty()) {
            throw new IllegalArgumentException("Not-null but empty right side path in " + this);
        }
        int count = 0;
        if (values != null) {
            count++;
        }
        if (expression != null) {
            count++;
        }
        if (rightHandSidePath != null) {
            count++;
        }
        if (count > 1) {
            throw new IllegalStateException("Two or more of the following are non-null: values (" + values
                    + "), expression (" + expression + "), rightHandSidePath (" + rightHandSidePath + ") in " + this);
        }
        if (values != null) {
            for (V value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("Null value in " + this);
                }
                if (value.getParent() != this) {
                    throw new IllegalArgumentException("Value " + value + " in " + this + " has a bad parent " + value.getParent());
                }
                if (value.isEmpty() && !value.isRaw()) {
                    throw new IllegalArgumentException("Empty value in " + this);
                }
            }
        }
        if (definition != null && ItemPath.isName(last)) {
            if (!QNameUtil.match(definition.getItemName(), ItemPath.toName(last))) {
                throw new IllegalArgumentException("Last segment of item path (" + fullPath.lastName() + ") "
                        + "does not match item name from the definition: " + definition);
            }
            // todo check consistence for ID-based filters
        }
    }
}
