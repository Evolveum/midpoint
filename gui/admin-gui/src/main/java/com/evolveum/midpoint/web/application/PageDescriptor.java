/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.application;

import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author lazyman
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PageDescriptor {

    /**
     * Please use {@link PageDescriptor#urls()}
     * @return
     */
    @Deprecated
    String[] url() default {};

    Url[] urls() default {};

    Class<? extends IPageParametersEncoder> encoder() default PageParametersEncoder.class;

    AuthorizationAction[] action() default {};
    
    /**
     * Permit access to all users (even non-authenticated users)
     */
    boolean permitAll() default false;
}
