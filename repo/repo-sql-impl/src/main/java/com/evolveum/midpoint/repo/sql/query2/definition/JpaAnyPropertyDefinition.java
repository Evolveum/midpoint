/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.query2.definition;

/**
 * Specifies "any" property. In contrast to other JPA definitions, it is not derived by analyzing R-class
 * structure, but created on demand in the process if ItemPath translation.
 *
 * It was created to ensure consistency of resolution mechanism, which should provide
 * HQL property + JPA definition for any item path provided.
 *
 * @author mederly
 */
public class JpaAnyPropertyDefinition extends JpaPropertyDefinition {

    // enumerated extension items are not supported
    public JpaAnyPropertyDefinition(Class jpaClass, Class jaxbClass) {
        super(jpaClass, jaxbClass, false, false, false, false);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "AnyProperty";
    }
}
