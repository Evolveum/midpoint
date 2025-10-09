/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.io.Serializable;

/**
 * A generalization of a source for synchronization and inbounds processing purposes:
 * Either an {@link AbstractShadow} or a {@link ShadowAssociationValue}.
 */
@Experimental
public interface ShadowLikeValue extends Serializable, DebugDumpable {

}
