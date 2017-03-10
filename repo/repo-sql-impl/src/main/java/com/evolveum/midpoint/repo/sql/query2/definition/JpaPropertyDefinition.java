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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;

/**
 * @author lazyman
 */
public class JpaPropertyDefinition extends JpaDataNodeDefinition {

    //jpa special types
    private boolean lob;
    private boolean enumerated;
    //jpa special things
    private boolean indexed;            // currently unused

    public JpaPropertyDefinition(Class jpaClass, Class jaxbClass, boolean lob, boolean enumerated, boolean indexed) {
        super(jpaClass, jaxbClass);
        this.lob = lob;
        this.enumerated = enumerated;
        this.indexed = indexed;
    }

    public boolean isLob() {
        return lob;
    }

    public boolean isPolyString() {
        return RPolyString.class.equals(getJpaClass());
    }

    public boolean isEnumerated() {
        return enumerated;
    }

    public boolean isIndexed() {
        return indexed;
    }

    void setLob(boolean lob) {
        this.lob = lob;
    }

    void setEnumerated(boolean enumerated) {
        this.enumerated = enumerated;
    }

    void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Prop";
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) {
        // nowhere to come from here
        return null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getShortInfo());
        if (isLob()) {
            sb.append(", lob");
        }
        if (isEnumerated()) {
            sb.append(", enumerated");
        }
        if (isIndexed()) {
            sb.append(", indexed");
        }
        return sb.toString();
    }
}
