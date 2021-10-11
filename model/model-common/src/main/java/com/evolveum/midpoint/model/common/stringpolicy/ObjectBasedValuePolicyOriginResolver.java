/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * ValuePolicyOriginResolver that resolves origin based on some context object (that can be retrieved).
 */
public interface ObjectBasedValuePolicyOriginResolver<O extends ObjectType> extends ValuePolicyOriginResolver {

    /**
     * Returns the object in context of which we are resolving the origin.
     */
    PrismObject<O> getObject();
}
