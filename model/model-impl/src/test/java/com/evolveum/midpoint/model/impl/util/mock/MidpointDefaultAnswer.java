/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util.mock;

import com.evolveum.midpoint.schema.SearchResultList;
import org.mockito.internal.stubbing.defaultanswers.GloballyConfiguredAnswer;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;

/**
 * @author mederly
 */
public class MidpointDefaultAnswer extends GloballyConfiguredAnswer {
    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Object answer = super.answer(invocation);
        if (answer != null) {
            return null;
        }

        Class returnType = invocation.getMethod().getReturnType();
        if (SearchResultList.class.isAssignableFrom(returnType)) {
            return new SearchResultList(new ArrayList<>(0));
        }

        return null;
    }
}
