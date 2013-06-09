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

package com.evolveum.midpoint.repo.sql.query.definition;

import java.util.Comparator;

/**
 * @author lazyman
 */
public class DefinitionComparator implements Comparator<Definition> {

    @Override
    public int compare(Definition o1, Definition o2) {
        if (o1.getClass().equals(o2.getClass())) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getJaxbName().getLocalPart(),
                    o2.getJaxbName().getLocalPart());
        }

        return getType(o1) - getType(o2);
    }

    private int getType(Definition def) {
        if (def == null) {
            return 0;
        }

        if (def instanceof PropertyDefinition) {
            return 1;
        } else if (def instanceof ReferenceDefinition) {
            return 2;
        } else if (def instanceof CollectionDefinition) {
            return 3;
        } else if (def instanceof AnyDefinition) {
            return 4;
        } else if (def instanceof EntityDefinition) {
            return 5;
        }

        return 0;
    }
}