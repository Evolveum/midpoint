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
 * A path of JPA names. Analogous to ItemPath; however, expresses the path in JPA terms, not prism terms.
 *
 * @author mederly
 */
public class JpaNamePath {

    private List<String> names = new ArrayList<>();

    public JpaNamePath(List<String> names) {
        this.names = names;
    }

    public void add(String jpaName) {
        names.add(jpaName);
    }

    public List<String> getNames() {
        return names;
    }

    @Override
    public String toString() {
        return "JpaNamePath{" +
                "names=" + names +
                '}';
    }

    public JpaNamePath allExceptLast() {
        return new JpaNamePath(names.subList(0, names.size()-1));
    }
}
