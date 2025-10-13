/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
