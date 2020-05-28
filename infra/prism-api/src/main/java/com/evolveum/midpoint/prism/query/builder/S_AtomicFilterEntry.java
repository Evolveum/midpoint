/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface S_AtomicFilterEntry {
    S_AtomicFilterExit all();
    S_AtomicFilterExit none();
    S_AtomicFilterExit undefined();
    S_AtomicFilterExit filter(ObjectFilter filter); // not much tested, use with care
    S_ConditionEntry item(QName... names);
    S_ConditionEntry item(String... names);
    S_ConditionEntry item(ItemPath path);
    S_ConditionEntry item(ItemPath itemPath, ItemDefinition itemDefinition);
    S_ConditionEntry itemWithDef(ItemDefinition itemDefinition, QName... names);        // experimental
    S_ConditionEntry item(PrismContainerDefinition containerDefinition, QName... names);
    S_ConditionEntry item(PrismContainerDefinition containerDefinition, ItemPath itemPath);
    S_MatchingRuleEntry itemAs(PrismProperty<?> property);              // experimental; TODO choose better name for this method
    S_AtomicFilterExit id(String... identifiers);
    S_AtomicFilterExit id(long... identifiers);
    S_AtomicFilterExit ownerId(String... identifiers);
    S_AtomicFilterExit ownerId(long... identifiers);
    S_AtomicFilterExit isDirectChildOf(PrismReferenceValue value);
    S_AtomicFilterExit isChildOf(PrismReferenceValue value);
    S_AtomicFilterExit isDirectChildOf(String oid);
    S_AtomicFilterExit isChildOf(String oid);
    S_AtomicFilterExit isParentOf(PrismReferenceValue value);            // reference should point to OrgType
    S_AtomicFilterExit isParentOf(String oid);                           // oid should be of an OrgType
    S_AtomicFilterExit isInScopeOf(String oid, OrgFilter.Scope scope);
    S_AtomicFilterExit isInScopeOf(PrismReferenceValue value, OrgFilter.Scope scope);
    S_AtomicFilterExit isRoot() ;
    S_AtomicFilterExit fullText(String... words);
    S_FilterEntryOrEmpty block();
    S_FilterEntryOrEmpty type(Class<? extends Containerable> type);
    S_FilterEntryOrEmpty type(QName type);
    S_FilterEntryOrEmpty exists(Object... components);
}
