/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
