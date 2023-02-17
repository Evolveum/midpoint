/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Customizes an object.
 */
@FunctionalInterface
public interface ObjectCustomizer<O extends ObjectType> {
    void customize(O object) throws CommonException;
}
