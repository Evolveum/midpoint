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

import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaNamePath;

import java.util.ArrayList;
import java.util.List;

/**
 * A path of JPA definitions. Analogous to ItemPath; however, expresses the path in JPA terms, not prism terms.
 *
 * @author mederly
 */
public class JpaDefinitionPath {

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

    public boolean containsAnyDefinition() {
        for (Definition definition : definitions) {
            if (definition instanceof AnyDefinition) {
                return true;
            }
        }
        return false;
    }
}
