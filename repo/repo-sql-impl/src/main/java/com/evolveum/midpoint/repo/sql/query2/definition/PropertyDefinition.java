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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class PropertyDefinition extends Definition {

    //jpa special types
    private boolean lob;
    private boolean enumerated;
    //jpa special things
    private boolean indexed;

    public PropertyDefinition(QName jaxbName, Class jaxbType, String propertyName, Class propertyType, CollectionSpecification collectionSpecification) {
        super(jaxbName, jaxbType, propertyName, propertyType, collectionSpecification);
    }

    public boolean isLob() {
        return lob;
    }

    public boolean isPolyString() {
        return RPolyString.class.equals(getJpaType());
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
    protected void debugDumpExtended(StringBuilder builder, int indent) {
        builder.append(", lob=").append(isLob());
        builder.append(", enumerated=").append(isEnumerated());
        builder.append(", indexed=").append(isIndexed());
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Prop";
    }

    @Override
    public DefinitionSearchResult nextDefinition(ItemPath path) {
        // nowhere to come from here
        return null;
    }
}
