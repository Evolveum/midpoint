/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.ninja.impl.NinjaContext;

/**
 * Created by Viliam Repan (lazyman).
 */
@FunctionalInterface
public interface ExecutionValidator {

    void validate(NinjaContext context) throws Exception;
}
