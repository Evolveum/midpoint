/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query.builder;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.impl.query.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;

import com.evolveum.midpoint.prism.query.builder.*;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class R_AtomicFilter implements S_ConditionEntry, S_MatchingRuleEntry, S_RightHandItemEntry {

    private final ItemPath itemPath;
    private final PrismPropertyDefinition propertyDefinition;
    private final PrismReferenceDefinition referenceDefinition;
    private final ValueFilter filter;
    private final R_Filter owner;
    private final boolean expectingRightSide;

    private R_AtomicFilter(ItemPath itemPath, ItemDefinition itemDefinition, R_Filter owner) {
        Validate.notNull(itemPath);
        Validate.notNull(itemDefinition);
        Validate.notNull(owner);
        this.itemPath = itemPath;
        if (itemDefinition instanceof PrismPropertyDefinition) {
            propertyDefinition = (PrismPropertyDefinition) itemDefinition;
            referenceDefinition = null;
        } else if (itemDefinition instanceof PrismReferenceDefinition) {
            propertyDefinition = null;
            referenceDefinition = (PrismReferenceDefinition) itemDefinition;
        } else {
            throw new IllegalStateException("Unsupported item definition: " + itemDefinition);
        }
        this.filter = null;
        this.owner = owner;
        this.expectingRightSide = false;
    }

    private R_AtomicFilter(R_AtomicFilter original, ValueFilter filter, boolean expectingRightSide) {
        Validate.notNull(original);
        Validate.notNull(filter);
        this.itemPath = original.itemPath;
        this.propertyDefinition = original.propertyDefinition;
        this.referenceDefinition = original.referenceDefinition;
        this.filter = filter;
        this.owner = original.owner;
        this.expectingRightSide = expectingRightSide;
    }

    public R_AtomicFilter(R_AtomicFilter original, ValueFilter filter) {
        this(original, filter, false);
    }

    static R_AtomicFilter create(ItemPath itemPath, ItemDefinition itemDefinition, R_Filter owner) {
        return new R_AtomicFilter(itemPath, itemDefinition, owner);
    }

    @Override
    public S_AtomicFilterExit item(QName... names) {
        return item(ItemPath.create((Object[]) names), null);
    }

    @Override
    public S_AtomicFilterExit item(ItemPath itemPath, ItemDefinition itemDefinition) {
        if (!expectingRightSide) {
            throw new IllegalStateException("Unexpected item() call");
        }
        if (filter == null) {
            throw new IllegalStateException("item() call with no filter");
        }
        ValueFilter newFilter = filter.clone();
        newFilter.setRightHandSidePath(itemPath);
        newFilter.setRightHandSideDefinition(itemDefinition);
        return new R_AtomicFilter(this, newFilter);
    }

    @Override
    public <T> S_MatchingRuleEntry eq(PrismProperty<T> property) {
        List<PrismPropertyValue<T>> clonedValues = (List<PrismPropertyValue<T>>) PrismValueCollectionsUtil.cloneCollection(property.getValues());
        //noinspection unchecked
        PrismPropertyDefinition<T> definition =
                this.propertyDefinition != null ?
                        this.propertyDefinition : property.getDefinition();
        return new R_AtomicFilter(this, EqualFilterImpl
                .createEqual(itemPath, definition, null, owner.getPrismContext(), clonedValues));
    }

    @Override
    public S_MatchingRuleEntry eq(Object... values) {
        return new R_AtomicFilter(this, EqualFilterImpl.createEqual(itemPath, propertyDefinition, null, owner.getPrismContext(), values));
    }

    @Override
    public S_RightHandItemEntry eq() {
        return new R_AtomicFilter(this, EqualFilterImpl.createEqual(itemPath, propertyDefinition, null), true);
    }

    @Override
    public S_MatchingRuleEntry eqPoly(String orig, String norm) {
        return eq(new PolyString(orig, norm));
    }

    @Override
    public S_MatchingRuleEntry eqPoly(String orig) {
        return eq(new PolyString(orig));
    }

    @Override
    public S_MatchingRuleEntry gt(Object value) {
        return new R_AtomicFilter(this, GreaterFilterImpl.createGreater(itemPath, propertyDefinition, null, value,
                false, owner.getPrismContext()));
    }

    @Override
    public S_RightHandItemEntry gt() {
        return new R_AtomicFilter(this, GreaterFilterImpl.createGreater(itemPath, propertyDefinition, false), true);
    }

    @Override
    public S_MatchingRuleEntry ge(Object value) {
        return new R_AtomicFilter(this, GreaterFilterImpl.createGreater(itemPath, propertyDefinition, null, value,
                true, owner.getPrismContext()));
    }

    @Override
    public S_RightHandItemEntry ge() {
        return new R_AtomicFilter(this, GreaterFilterImpl.createGreater(itemPath, propertyDefinition, true), true);
    }

    @Override
    public S_MatchingRuleEntry lt(Object value) {
        return new R_AtomicFilter(this, LessFilterImpl.createLess(itemPath, propertyDefinition, null, value, false, owner.getPrismContext()));
    }

    @Override
    public S_RightHandItemEntry lt() {
        return new R_AtomicFilter(this, LessFilterImpl.createLess(itemPath, propertyDefinition, false), true);
    }

    @Override
    public S_MatchingRuleEntry le(Object value) {
        return new R_AtomicFilter(this, LessFilterImpl.createLess(itemPath, propertyDefinition, null, value, true, owner.getPrismContext()));
    }

    @Override
    public S_RightHandItemEntry le() {
        return new R_AtomicFilter(this, LessFilterImpl.createLess(itemPath, propertyDefinition, true), true);
    }

    @Override
    public S_MatchingRuleEntry startsWith(Object value) {
        return new R_AtomicFilter(this, SubstringFilterImpl
                .createSubstring(itemPath, propertyDefinition, owner.getPrismContext(), null, value, true, false));
    }

    @Override
    public S_MatchingRuleEntry startsWithPoly(String orig, String norm) {
        return startsWith(new PolyString(orig, norm));
    }

    @Override
    public S_MatchingRuleEntry startsWithPoly(String orig) {
        return startsWith(new PolyString(orig));
    }

    @Override
    public S_MatchingRuleEntry endsWith(Object value) {
        return new R_AtomicFilter(this, SubstringFilterImpl.createSubstring(itemPath, propertyDefinition, owner.getPrismContext(), null, value, false, true));
    }

    @Override
    public S_MatchingRuleEntry endsWithPoly(String orig, String norm) {
        return endsWith(new PolyString(orig, norm));
    }

    @Override
    public S_MatchingRuleEntry endsWithPoly(String orig) {
        return endsWith(new PolyString(orig));
    }

    @Override
    public S_MatchingRuleEntry contains(Object value) {
        return new R_AtomicFilter(this, SubstringFilterImpl.createSubstring(itemPath, propertyDefinition, owner.getPrismContext(), null, value, false, false));
    }

    @Override
    public S_MatchingRuleEntry containsPoly(String orig, String norm) {
        return contains(new PolyString(orig, norm));
    }

    @Override
    public S_MatchingRuleEntry containsPoly(String orig) {
        return contains(new PolyString(orig));
    }

    @Override
    public S_AtomicFilterExit ref(QName... relations) {
        List<PrismReferenceValue> values = new ArrayList<>();
        for (QName relation : relations) {
            PrismReferenceValue ref = new PrismReferenceValueImpl();
            ref.setRelation(relation);
            values.add(ref);
        }
        RefFilter filter = RefFilterImpl.createReferenceEqual(itemPath, referenceDefinition, values);
        filter.setOidNullAsAny(true);
        filter.setTargetTypeNullAsAny(true);
        return new R_AtomicFilter(this, filter);
    }


    @Override
    public S_AtomicFilterExit ref(PrismReferenceValue... values) {
        if (values.length == 1 && values[0] == null) {
            return ref(Collections.emptyList());
        } else {
            return ref(Arrays.asList(values));
        }
    }

    @Override
    public S_AtomicFilterExit ref(Collection<PrismReferenceValue> values) {
        return ref(values, true);
    }

    @Override
    public S_AtomicFilterExit ref(Collection<PrismReferenceValue> values, boolean nullTypeAsAny) {
        RefFilter filter = RefFilterImpl.createReferenceEqual(itemPath, referenceDefinition, values);
        filter.setTargetTypeNullAsAny(nullTypeAsAny);
        return new R_AtomicFilter(this, filter);
    }

    @Override
    public S_AtomicFilterExit ref(String... oids) {
        if (oids.length == 1 && oids[0] == null) {
            return ref(Collections.emptyList());
        } else {
            // when OIDs are specified, we allow any type
            return ref(Arrays.stream(oids).map(oid -> new PrismReferenceValueImpl(oid)).collect(Collectors.toList()), true);
        }
    }

    @Override
    public S_AtomicFilterExit ref(String oid, QName targetTypeName) {
        if (oid != null) {
            return ref(new PrismReferenceValueImpl(oid, targetTypeName));
        } else {
            return ref(Collections.emptyList());
        }
    }

    @Override
    public S_AtomicFilterExit isNull() {
        if (propertyDefinition != null) {
            return new R_AtomicFilter(this, EqualFilterImpl.createEqual(itemPath, propertyDefinition, null, owner.getPrismContext()));
        } else if (referenceDefinition != null) {
            return new R_AtomicFilter(this, RefFilterImpl.createReferenceEqual(itemPath, referenceDefinition, Collections.emptyList()));
        } else {
            throw new IllegalStateException("No definition");
        }
    }

    @Override
    public S_AtomicFilterExit matching(QName matchingRuleName) {
        ValueFilter clone = filter.clone();
        clone.setMatchingRule(matchingRuleName);
        return new R_AtomicFilter(this, clone);
    }

    @Override
    public S_AtomicFilterExit matchingOrig() {
        return matching(PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME);
    }

    @Override
    public S_AtomicFilterExit matchingNorm() {
        return matching(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME);
    }

    @Override
    public S_AtomicFilterExit matchingStrict() {
        return matching(PrismConstants.POLY_STRING_STRICT_MATCHING_RULE_NAME);
    }

    @Override
    public S_AtomicFilterExit matchingCaseIgnore() {
        return matching(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME);
    }

    // ==============================================================
    // Methods which implement actions common with R_Filter

    @Override
    public S_FilterEntry or() {
        return finish().or();
    }

    @Override
    public S_FilterEntry and() {
        return finish().and();
    }

    @Override
    public ObjectQuery build() {
        return finish().build();
    }

    @Override
    public ObjectFilter buildFilter() {
        return build().getFilter();
    }

    @Override
    public S_FilterExit asc(QName... names) {
        return finish().asc(names);
    }

    @Override
    public S_FilterExit asc(ItemPath path) {
        return finish().asc(path);
    }

    @Override
    public S_FilterExit desc(QName... names) {
        return finish().desc(names);
    }

    @Override
    public S_FilterExit desc(ItemPath path) {
        return finish().desc(path);
    }

    @Override
    public S_FilterExit group(QName... names) {
        return finish().group(names);
    }

    @Override
    public S_FilterExit group(ItemPath path) {
        return finish().group(path);
    }

    @Override
    public S_FilterExit offset(Integer n) {
        return finish().offset(n);
    }

    @Override
    public S_FilterExit maxSize(Integer n) {
        return finish().maxSize(n);
    }

    @Override
    public S_AtomicFilterExit endBlock() {
        return finish().endBlock();
    }

    private R_Filter finish() {
        if (filter == null) {
            throw new IllegalStateException("Filter is not yet created!");
        }
        return owner.addSubfilter(filter);
    }


}
