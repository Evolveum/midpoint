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

import java.util.Comparator;

/**
 * @author lazyman
 */
public class LinkDefinitionComparator implements Comparator<JpaLinkDefinition> {

    @Override
    public int compare(JpaLinkDefinition o1, JpaLinkDefinition o2) {

        // longer paths have to come first, in order for matching to work

        int sizeDiff = o1.getItemPath().size() - o2.getItemPath().size();
        if (sizeDiff != 0) {
            return -sizeDiff;
        }

        JpaDataNodeDefinition target1 = o1.getTargetDefinition();
        JpaDataNodeDefinition target2 = o2.getTargetDefinition();

        if (target1.equals(target2)) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getItemPathSegment().toString(),
                    o2.getItemPathSegment().toString());
        }

        return getType(target1) - getType(target2);
    }

    private int getType(JpaDataNodeDefinition def) {
        if (def == null) {
            return 0;
        }
        if (def instanceof JpaPropertyDefinition) {
            return 1;
        } else if (def instanceof JpaReferenceDefinition) {
            return 2;
        } else if (def instanceof JpaAnyContainerDefinition) {
            return 4;
        } else if (def instanceof JpaEntityDefinition) {
            return 5;
        }
        return 0;
    }
}