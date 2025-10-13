/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

/**
 * Used in connection with initializable objects. Informs that this object is not applicable for further processing.
 *
 * The exception is used as a shortcut to simplify processing: we do not want to check for all possible corner cases
 * in the code that follows after the point(s) where we determine there is nothing we can do with the object.
 */
public class NotApplicableException extends Exception {
}
