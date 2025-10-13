/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
