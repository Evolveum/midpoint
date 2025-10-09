/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.api;

import java.util.Collection;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface AuthorizationTransformer {

    Collection<Authorization> transform(Authorization authorization);

}
