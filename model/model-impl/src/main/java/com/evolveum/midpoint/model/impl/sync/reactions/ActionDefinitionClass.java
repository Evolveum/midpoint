/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
