/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

/**
 * A resource schema that is certain to contain no refinements. It's created from the native schema alone.
 * Normally, it should not be used. Use {@link CompleteResourceSchema} instead.
 */
public interface BareResourceSchema extends ResourceSchema {

}
