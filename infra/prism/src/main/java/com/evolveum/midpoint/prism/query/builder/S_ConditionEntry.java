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

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.nio.file.attribute.AclEntry;

/**
 * @author mederly
 */
public interface S_ConditionEntry {
    S_MatchingRuleEntry eq(Object value);
    S_RightHandItemEntry eq();
    S_MatchingRuleEntry eqPoly(String orig, String norm);
    S_MatchingRuleEntry gt(Object value) throws SchemaException;
    S_RightHandItemEntry gt();
    S_MatchingRuleEntry ge(Object value) throws SchemaException;
    S_RightHandItemEntry ge();
    S_MatchingRuleEntry lt(Object value) throws SchemaException;
    S_RightHandItemEntry lt();
    S_MatchingRuleEntry le(Object value) throws SchemaException;
    S_RightHandItemEntry le();
    S_AtomicFilterExit startsWith(String value);
    S_AtomicFilterExit endsWith(String value);
    S_AtomicFilterExit contains(String value);
    S_AtomicFilterExit ref(PrismReferenceValue value);
    S_AtomicFilterExit ref(String oid);
    S_AtomicFilterExit ref(String oid, QName targetTypeName);
    S_AtomicFilterExit isNull();

}
