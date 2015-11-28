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

import java.util.ArrayList;
import java.util.List;

/**
 * A path of JPA definitions. Analogous to ItemPath; however, expresses the path in JPA terms, not prism terms.
 * Auxiliary definitions (e.g. real EntityDefinition referenced by EntityReferenceDefinition) are NOT in such
 * paths. I.e. path corresponds to the path of JPA (hibernate) names.
 *
 * Example:
 *
 * base entity = RUser
 * prism path = assignment/target/link/activation/administrativeStatus (i.e. looks for admin status of a projection of e.g. assigned role)
 * def path =
 *   Ent: assignment (RAssignment)
 *     Ref: target (real entity = RObject)
 *       Ref: link (real entity = RObject)
 *         Ent: activation
 *           Prop: administrativeStatus
 *
 * @author mederly
 */
public class DefinitionPath {

    private List<JpaItemDefinition> definitions = new ArrayList<>();

    public void add(JpaItemDefinition definition) {
        definitions.add(definition);
    }

    public List<JpaItemDefinition> getDefinitions() {
        return definitions;
    }

    public int size() {
        return definitions.size();
    }

    public JpaItemDefinition get(int i) {
        return definitions.get(i);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;

        for (JpaItemDefinition definition : definitions) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (definition != null) {
                sb.append(definition);
            } else {
                sb.append("null");      // Just to catch errors in translations
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
