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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.match.PolyStringStrictMatchingRule;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;

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

    final ItemPath itemPath;
    final PrismPropertyDefinition propertyDefinition;
    final PrismReferenceDefinition referenceDefinition;
    final ValueFilter filter;
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

    R_AtomicFilter(R_AtomicFilter original, ValueFilter filter, boolean expectingRightSide) {
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
        return item(new ItemPath(names), null);
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
        List<PrismPropertyValue<T>> clonedValues = (List<PrismPropertyValue<T>>) PrismPropertyValue.cloneCollection(property.getValues());
        PrismPropertyDefinition<T> definition =
                this.propertyDefinition != null ?
                        this.propertyDefinition : property.getDefinition();
        return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, definition, null, owner.getPrismContext(), clonedValues));
    }

    @Override
    public S_MatchingRuleEntry eq(Object... values) {
        return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, propertyDefinition, null, owner.getPrismContext(), values));
    }

    @Override
    public S_RightHandItemEntry eq() {
        return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, propertyDefinition, null), true);
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
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, false, owner.getPrismContext(), value));
    }

    @Override
    public S_RightHandItemEntry gt() {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, false), true);
    }

    @Override
    public S_MatchingRuleEntry ge(Object value) {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, true, owner.getPrismContext(), value));
    }

    @Override
    public S_RightHandItemEntry ge() {
        return new R_AtomicFilter(this, GreaterFilter.createGreater(itemPath, propertyDefinition, true), true);
    }

    @Override
    public S_MatchingRuleEntry lt(Object value) {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, owner.getPrismContext(), value, false));
    }

    @Override
    public S_RightHandItemEntry lt() {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, false), true);
    }

    @Override
    public S_MatchingRuleEntry le(Object value) {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, owner.getPrismContext(), value, true));
    }

    @Override
    public S_RightHandItemEntry le() {
        return new R_AtomicFilter(this, LessFilter.createLess(itemPath, propertyDefinition, true), true);
    }

    @Override
    public S_MatchingRuleEntry startsWith(Object value) {
        return new R_AtomicFilter(this, SubstringFilter.createSubstring(itemPath, propertyDefinition, owner.getPrismContext(), null, value, true, false));
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
        return new R_AtomicFilter(this, SubstringFilter.createSubstring(itemPath, propertyDefinition, owner.getPrismContext(), null, value, false, true));
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
        return new R_AtomicFilter(this, SubstringFilter.createSubstring(itemPath, propertyDefinition, owner.getPrismContext(), null, value, false, false));
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
    public S_AtomicFilterExit ref(QName relation) {
		PrismReferenceValue ref = new PrismReferenceValue();
		ref.setRelation(relation);
		List<PrismReferenceValue> values = new ArrayList<>();
		values.add(ref);
		RefFilter filter = RefFilter.createReferenceEqual(itemPath, referenceDefinition, values);
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
		return ref(values, false);
	}

	@Override
	public S_AtomicFilterExit ref(Collection<PrismReferenceValue> values, boolean nullTypeAsAny) {
        RefFilter filter = RefFilter.createReferenceEqual(itemPath, referenceDefinition, values);
        filter.setTargetTypeNullAsAny(nullTypeAsAny);
        return new R_AtomicFilter(this, filter);
	}

	@Override
    public S_AtomicFilterExit ref(String... oids) {
    	if (oids.length == 1 && oids[0] == null) {
    		return ref(Collections.emptyList());
		} else {
    	    // when OIDs are specified, we allow any type
    		return ref(Arrays.stream(oids).map(oid -> new PrismReferenceValue(oid)).collect(Collectors.toList()), true);
		}
    }

    @Override
    public S_AtomicFilterExit ref(String oid, QName targetTypeName) {
        if (oid != null) {
            return ref(new PrismReferenceValue(oid, targetTypeName));
        } else {
            return ref(Collections.emptyList());
        }
    }

    @Override
    public S_AtomicFilterExit isNull() {
        if (propertyDefinition != null) {
            return new R_AtomicFilter(this, EqualFilter.createEqual(itemPath, propertyDefinition, null, owner.getPrismContext()));
        } else if (referenceDefinition != null) {
            return new R_AtomicFilter(this, RefFilter.createReferenceEqual(itemPath, referenceDefinition, Collections.emptyList()));
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
