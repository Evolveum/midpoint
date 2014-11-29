/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
