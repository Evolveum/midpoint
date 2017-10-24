/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.tools.ninja;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lazyman
 */
public abstract class BaseNinjaAction {

    public static final String[] CONTEXTS = {
            "classpath:ctx-ninja.xml",
            "classpath:ctx-common.xml",
            "classpath:ctx-configuration.xml",
            "classpath*:ctx-repository.xml",
            "classpath:ctx-repo-cache.xml",
            "classpath:ctx-audit.xml",
            "classpath:ctx-security.xml"
    };

    protected void destroyContext(ClassPathXmlApplicationContext context) {
        if (context != null) {
            try {
                context.destroy();
            } catch (Exception ex) {
                System.out.println("Exception occurred during context shutdown, reason: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
