/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * Temporary class to hold the default Java method reference library.
 *
 * This is a placeholder for the actual implementation that will be provided via MEL in the future.
 *
 * For Evolveum internal use only!
 *
 * NOTE: Do not change parameter names! They are bound to variable names at the place of use.
 */
@SuppressWarnings("unused") // called from the outside
@NullMarked
@Experimental
@Deprecated
public class DefaultJavaMethodReferenceLibrary {

    /**
     * Used by {@code 100-report-reconciliation}.
     */
    public static @Nullable QName qualifyObjectClassName(@Nullable String objectClass) {
        return objectClass != null ? new QName(SchemaConstants.NS_RI, objectClass) : null;
    }
}
