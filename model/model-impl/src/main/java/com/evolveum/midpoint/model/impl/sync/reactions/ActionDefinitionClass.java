/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.reactions;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationActionType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * What XSD-derived class provides the configuration of this synchronization action?
 *
 * Used to find action implementation for given configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionDefinitionClass {
    Class<? extends AbstractSynchronizationActionType> value();
}
