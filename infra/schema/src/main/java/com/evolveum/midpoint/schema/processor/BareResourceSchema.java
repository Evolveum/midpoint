/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

/**
 * A resource schema that is certain to contain no refinements. It's created from the native schema alone.
 * Normally, it should not be used. Use {@link CompleteResourceSchema} instead.
 */
public interface BareResourceSchema extends ResourceSchema {

}
