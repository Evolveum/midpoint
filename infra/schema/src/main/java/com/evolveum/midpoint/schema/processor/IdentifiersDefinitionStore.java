/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Provides information about primary and secondary identifiers.
 */
public interface IdentifiersDefinitionStore {

    /**
     * Returns the definition of primary identifier attributes of a resource object.
     *
     * May return empty set if there are no identifier attributes. Must not
     * return null.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of identifier attributes
     */
    @NotNull Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers();

    default @NotNull ResourceAttributeDefinition<?> getPrimaryIdentifierRequired() {
        Collection<? extends ResourceAttributeDefinition<?>> primaryIdentifiers = getPrimaryIdentifiers();
        return MiscUtil.extractSingletonRequired(
                primaryIdentifiers,
                () -> new IllegalStateException("No primary identifier in " + this),
                () -> new IllegalStateException("Multiple primary identifiers in " + this + ": " + primaryIdentifiers));
    }

    /**
     * Returns names of primary identifiers.
     *
     * @see #getPrimaryIdentifiers()
     */
    @NotNull Collection<QName> getPrimaryIdentifiersNames();

    /**
     * Returns true if the attribute with a given name is among primary identifiers.
     * Matching is done using namespace-approximate method (testing only local part if
     * no namespace is provided), so beware of incidental matching (e.g. ri:uid vs icfs:uid).
     */
    default boolean isPrimaryIdentifier(QName attrName) {
        return getPrimaryIdentifiersNames().stream()
                .anyMatch(idDef -> QNameUtil.match(idDef, attrName));
    }

    /**
     * Returns the definition of secondary identifier attributes of a resource
     * object.
     *
     * May return empty set if there are no secondary identifier attributes.
     * Must not return null.
     *
     * The exception should be never thrown unless there is some bug in the
     * code. The validation of model consistency should be done at the time of
     * schema parsing.
     *
     * @return definition of secondary identifier attributes
     */
    @NotNull Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers();

    /**
     * Returns names of secondary identifiers.
     *
     * @see #getSecondaryIdentifiers() ()
     */
    @NotNull Collection<QName> getSecondaryIdentifiersNames();

    /**
     * Returns true if the attribute with a given name is among secondary identifiers.
     * Matching is done using namespace-approximate method (testing only local part if
     * no namespace is provided), so beware of incidental matching (e.g. ri:uid vs icfs:uid).
     */
    default boolean isSecondaryIdentifier(QName attrName) {
        return getSecondaryIdentifiersNames().stream()
                .anyMatch(idDef -> QNameUtil.match(idDef, attrName));
    }

    /**
     * Returns true if the attribute is either primary or secondary identifier.
     */
    default boolean isIdentifier(QName attrName) {
        return isPrimaryIdentifier(attrName) || isSecondaryIdentifier(attrName);
    }

    /**
     * Returns both primary and secondary identifiers.
     */
    default Collection<? extends ResourceAttributeDefinition<?>> getAllIdentifiers() {
        return MiscUtil.unionExtends(
                getPrimaryIdentifiers(), getSecondaryIdentifiers());
    }
}
