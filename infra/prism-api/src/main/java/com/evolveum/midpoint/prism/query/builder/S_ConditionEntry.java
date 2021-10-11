/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.RefFilter;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface S_ConditionEntry {
    S_MatchingRuleEntry eq(Object... values);
    <T> S_MatchingRuleEntry eq(PrismProperty<T> property);            // TODO implement something like itemAs(property) to copy the property definition, path, and values into filter
    S_RightHandItemEntry eq();
    S_MatchingRuleEntry eqPoly(String orig, String norm);
    S_MatchingRuleEntry eqPoly(String orig);
    S_MatchingRuleEntry gt(Object value);
    S_RightHandItemEntry gt();
    S_MatchingRuleEntry ge(Object value);
    S_RightHandItemEntry ge();
    S_MatchingRuleEntry lt(Object value);
    S_RightHandItemEntry lt();
    S_MatchingRuleEntry le(Object value);
    S_RightHandItemEntry le();
    S_MatchingRuleEntry startsWith(Object value);
    S_MatchingRuleEntry startsWithPoly(String orig, String norm);
    S_MatchingRuleEntry startsWithPoly(String orig);
    S_MatchingRuleEntry endsWith(Object value);
    S_MatchingRuleEntry endsWithPoly(String orig, String norm);
    S_MatchingRuleEntry endsWithPoly(String orig);
    S_MatchingRuleEntry contains(Object value);
    S_MatchingRuleEntry containsPoly(String orig, String norm);
    S_MatchingRuleEntry containsPoly(String orig);
    S_AtomicFilterExit refRelation(QName... relations);
    S_AtomicFilterExit refType(QName... targetTypeName);
    S_AtomicFilterExit ref(PrismReferenceValue... value);
    S_AtomicFilterExit ref(Collection<PrismReferenceValue> values);
    S_AtomicFilterExit ref(Collection<PrismReferenceValue> values, boolean nullTypeAsAny);                          // beware, nullTypeAsAny=false is supported only by built-in match(..) method
    S_AtomicFilterExit ref(Collection<PrismReferenceValue> values, boolean nullOidAsAny, boolean nullTypeAsAny);    // beware, nullTypeAsAny=false and nullOidAsAny=false are supported only by built-in match(..) method
    S_AtomicFilterExit ref(RefFilter filter);
    S_AtomicFilterExit ref(String... oid);          // TODO define semantics for oid == null
    S_AtomicFilterExit ref(String oid, QName targetTypeName);       // TODO define semantics for oid == null
    S_AtomicFilterExit isNull();

}
