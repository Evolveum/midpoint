/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringStrictMatchingRule;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public class R_AtomicFilter implements S_ConditionEntry, S_MatchingRuleEntry, S_RightHandItemEntry {

    final ItemPath itemPath;
    final PrismPropertyDefinition propertyDefinition;
    final PrismReferenceDefinition referenceDefinition;
    final PropertyValueFilter filter;
    final R_Filter owner;
    final boolean expectingRightSide;

    R_AtomicFilter(ItemPath itemPath, ItemDefinition itemDefinition, R_Filter owner) {
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

    R_AtomicFilter(R_AtomicFilter original, PropertyValueFilter filter, boolean expectingRightSide) {
        Validate.notNull(original);
        Validate.notNull(filter);
        this.itemPath = original.itemPath;
        this.propertyDefinition = original.propertyDefinition;
        this.referenceDefinition = original.referenceDefinition;
        this.filter = filter;
        this.owner = original.owner;
        this.expectingRightSide = expectingRightSide;
    }

    public R_AtomicFilter(R_AtomicFilter original, PropertyValueFilter filter) {
        this(original, filter, false);
    }

    static R_AtomicFilter create(ItemPath itemPath, ItemDefinition itemDefinition, R_Filter owner) {
        return new R_AtomicFilter(itemPath, itemDefinition, owner);
    }

    @Override
    public S_AtomicFilterExit item(QName... names) throws SchemaException {
        return item(new ItemPath(names), null);
    }

    @Override
    public S_AtomicFilterExit item(ItemPath itemPath, ItemDefinition itemDefinition) throws SchemaException {
        if (!expectingRightSide) {
            throw new IllegalStateException("Unexpected item() call");
        }
        if (filter == null) {
            throw new IllegalStateException("item() call with no filter");
        }
        PropertyValueFilter newFilter = filter.clone();
        newFilter.setRightHandSidePath(itemPath);
        newFilter.setRightHandSideDefinition(itemDefinition);
        return new R_AtomicFilter(this, newFilter);
    }

    @Override
    public S_MatchingRuleEntry eq(Object... values) {
        return new R_AtomicFilter(this, EqualFilter.createEqualMultiple(itemPath, propertyDefinition, null, values));
    }

    @Override
    public S_RightHandItemEntry eq() {
        return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, propertyDefinition, null, (Object) null), true);
    }

    @Override
    public S_MatchingRuleEntry eqPoly(String orig, String norm) {
        return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, propertyDefinition, null, new PolyString(orig, norm)));
    }

    @Override
    public S_MatchingRuleEntry eqPoly(String orig) {
        return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, propertyDefinition, null, new PolyString(orig)));
    }

    @Override
    public S_MatchingRuleEntry gt(Object value) throws SchemaException {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, value, false));
    }

    @Override
    public S_RightHandItemEntry gt() {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, null, false), true);
    }

    @Override
    public S_MatchingRuleEntry ge(Object value) throws SchemaException {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, value, true));
    }

    @Override
    public S_RightHandItemEntry ge() {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, null, true), true);
    }

    @Override
    public S_MatchingRuleEntry lt(Object value) throws SchemaException {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, value, false));
    }

    @Override
    public S_RightHandItemEntry lt() {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, null, false), true);
    }

    @Override
    public S_MatchingRuleEntry le(Object value) throws SchemaException {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, value, true));
    }

    @Override
    public S_RightHandItemEntry le() {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, null, true), true);
    }

    @Override
    public S_MatchingRuleEntry startsWith(Object value) {
        return new R_AtomicFilter(this, SubstringFilter.createSubstring(itemPath, propertyDefinition, null, value, true, false));
    }

	@Override
	public S_MatchingRuleEntry startsWithPoly(String orig, String norm) {
		return startsWith(new PolyString(orig, norm));
	}

	@Override
    public S_MatchingRuleEntry endsWith(Object value) {
        return new R_AtomicFilter(this, SubstringFilter.createSubstring(itemPath, propertyDefinition, null, value, false, true));
    }

	@Override
	public S_MatchingRuleEntry endsWithPoly(String orig, String norm) {
		return endsWith(new PolyString(orig, norm));
	}

    @Override
    public S_MatchingRuleEntry contains(Object value) {
        return new R_AtomicFilter(this, SubstringFilter.createSubstring(itemPath, propertyDefinition, null, value, false, false));
    }

	@Override
	public S_MatchingRuleEntry containsPoly(String orig, String norm) {
		return contains(new PolyString(orig, norm));
	}

	@Override
    public S_AtomicFilterExit ref(PrismReferenceValue value) {
        return new R_AtomicFilter(this, RefFilter.createReferenceEqual(itemPath, referenceDefinition, value));
    }

	@Override
	public S_AtomicFilterExit ref(Collection<PrismReferenceValue> values) {
		return new R_AtomicFilter(this, RefFilter.createReferenceEqual(itemPath, referenceDefinition,
				values.toArray(new PrismReferenceValue[values.size()])));
	}

	@Override
    public S_AtomicFilterExit ref(String oid) {
        return new R_AtomicFilter(this, RefFilter.createReferenceEqual(itemPath, referenceDefinition, oid));
    }

    @Override
    public S_AtomicFilterExit ref(String oid, QName targetTypeName) {
        return ref(new PrismReferenceValue(oid, targetTypeName));
    }

    @Override
    public S_AtomicFilterExit isNull() {
        if (propertyDefinition != null) {
            return new R_AtomicFilter(this, EqualFilter.createNullEqual(itemPath, propertyDefinition, null));
        } else if (referenceDefinition != null) {
            return new R_AtomicFilter(this, RefFilter.createNullRefFilter(itemPath, referenceDefinition));
        } else {
            throw new IllegalStateException("No definition");
        }
    }

    @Override
    public S_AtomicFilterExit matching(QName matchingRuleName) {
        PropertyValueFilter clone = filter.clone();
        clone.setMatchingRule(matchingRuleName);
        return new R_AtomicFilter(this, clone);
    }

    @Override
    public S_AtomicFilterExit matchingOrig() {
        return matching(PolyStringOrigMatchingRule.NAME);
    }

    @Override
    public S_AtomicFilterExit matchingNorm() {
        return matching(PolyStringNormMatchingRule.NAME);
    }

    @Override
    public S_AtomicFilterExit matchingStrict() {
        return matching(PolyStringStrictMatchingRule.NAME);
    }

    @Override
    public S_AtomicFilterExit matchingCaseIgnore() {
        return matching(StringIgnoreCaseMatchingRule.NAME);
    }

    // ==============================================================
    // Methods which implement actions common with R_Filter

    @Override
    public S_FilterEntry or() throws SchemaException {
        return finish().or();
    }

    @Override
    public S_FilterEntry and() throws SchemaException {
        return finish().and();
    }

    @Override
    public ObjectQuery build() throws SchemaException {
        return finish().build();
    }

    @Override
    public ObjectFilter buildFilter() throws SchemaException {
        return build().getFilter();
    }

    @Override
    public S_FilterExit asc(QName... names) throws SchemaException {
        return finish().asc(names);
    }

    @Override
    public S_FilterExit asc(ItemPath path) throws SchemaException {
        return finish().asc(path);
    }

    @Override
    public S_FilterExit desc(QName... names) throws SchemaException {
        return finish().desc(names);
    }

    @Override
    public S_FilterExit desc(ItemPath path) throws SchemaException {
        return finish().desc(path);
    }

    @Override
    public S_AtomicFilterExit endBlock() throws SchemaException {
        return finish().endBlock();
    }

    private R_Filter finish() throws SchemaException {
        if (filter == null) {
            throw new IllegalStateException("Filter is not yet created!");
        }
        return owner.addSubfilter(filter);
    }


}
