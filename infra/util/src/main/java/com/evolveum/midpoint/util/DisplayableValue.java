/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

/**
 * @author Radovan Semancik
 *
 */
public interface DisplayableValue<T> {

    /**
     * Retuns actual value. This may not be user-friendly.
     */
    T getValue();

    /**
     * Returns short user-friendly label.
     * Catalog key may be returned instead of actual text.
     */
    String getLabel();

    /**
     * Returns longer description that can be used as a help text,
     * tooltip or for similar purpose.
     * Catalog key may be returned instead of actual text.
     */
    String getDescription();

}
