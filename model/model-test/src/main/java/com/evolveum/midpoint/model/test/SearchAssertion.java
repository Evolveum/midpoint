/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.test;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public interface SearchAssertion<O extends ObjectType> {

    void assertObjects(String message, List<PrismObject<O>> objects) throws Exception;

    void assertCount(int count) throws Exception;

}
