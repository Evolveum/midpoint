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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface S_ConditionEntry {
    S_MatchingRuleEntry eq(Object... values);
    <T> S_MatchingRuleEntry eq(PrismProperty<T> property);			// TODO implement something like itemAs(property) to copy the property definition, path, and values into filter
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
	S_AtomicFilterExit ref(QName relation);
    S_AtomicFilterExit ref(PrismReferenceValue... value);
    S_AtomicFilterExit ref(Collection<PrismReferenceValue> values);
    S_AtomicFilterExit ref(String... oid);
    S_AtomicFilterExit ref(String oid, QName targetTypeName);
    S_AtomicFilterExit isNull();

}
