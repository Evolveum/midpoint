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
 *
 * @author mederly
 */
public class DefinitionPath {

    private List<Definition> definitions = new ArrayList<>();

    public void add(Definition definition) {
        definitions.add(definition);
    }

    public List<Definition> getDefinitions() {
        return definitions;
    }

    public int size() {
        return definitions.size();
    }

    public Definition get(int i) {
        return definitions.get(i);
    }

    public AnyDefinition getAnyDefinition() {
        for (Definition definition : definitions) {
            if (definition instanceof AnyDefinition) {
                return (AnyDefinition) definition;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;

        for (Definition definition : definitions) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (definition != null) {
                sb.append(definition.getShortInfo());
            } else {
                sb.append("null");      // Just to catch errors in translations
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
