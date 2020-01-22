/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

/**
 * Object that can provide short, human-readable description. The description should
 * be suitable for logging and presentation to users (non-developers).
 *
 * @author semancik
 */
@FunctionalInterface
public interface HumanReadableDescribable {

    String toHumanReadableDescription();

}
