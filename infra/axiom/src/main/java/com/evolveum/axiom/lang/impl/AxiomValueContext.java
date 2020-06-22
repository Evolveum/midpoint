/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition.Scope;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.lang.impl.AxiomStatementRule.ActionBuilder;
import java.util.Optional;

public interface AxiomValueContext<V> {

    void replace(AxiomValue<?> axiomItemValue);

    default <T> AxiomItemContext<T> childItem(AxiomItemDefinition def) {
        return childItem(def.name());
    }

    <T> AxiomItemContext<T> childItem(AxiomName name);

    V currentValue();

    AxiomItemContext<V> parent();

    void mergeItem(AxiomItem<?> axiomItem);

    void register(AxiomName space, Scope scope, AxiomValueIdentifier key);

    AxiomRootContext root();

    ActionBuilder<?> newAction(String name);

    default AxiomValueContext<?> parentValue() {
        return parent().parent();
    }

    void replaceValue(V object);

    <V> AxiomValueReference<V> asReference();

    void valueIdentifier(AxiomValueIdentifier key);

    void mergeCompletedIfEmpty(Optional<AxiomItem<?>> item);

    /*V requireValue() throws AxiomSemanticException;

    AxiomItemDefinition definition();

    <V> AxiomValueContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value);

    Optional<V> optionalValue();

    void replace(Dependency<AxiomItemValue<?>> statement);

    AxiomValueContext<?> parent();

    void register(AxiomIdentifier space, Scope scope, AxiomValueIdentifier key);

    V requireValue(Class<V> type);


    void importIdentifierSpace(NamespaceContext namespaceContext);

    void exportIdentifierSpace(AxiomValueIdentifier namespace);

    void mergeItem(AxiomItem<?> children);

    AxiomStatementRule.Context<?> newAction(String actionName);

    AxiomStatementRule.Context<?> modify(AxiomValueContext<?> target, String actionName);

    void mergeEffectiveItemValues(AxiomItem<?> axiomItem); */

}
