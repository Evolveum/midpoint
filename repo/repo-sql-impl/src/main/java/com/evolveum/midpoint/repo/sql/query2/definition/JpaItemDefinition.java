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

import com.evolveum.midpoint.util.DebugUtil;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * Specifies an item - something that is included in an entity.
 *
 * @author mederly
 */
public abstract class JpaItemDefinition extends JpaDefinition {

    /**
     * Name in ItemPath by which is this item to be retrieved.
     */
    private QName jaxbName;

    /**
     * Name that is used when constructing HQL queries. Must correspond to the name of Rxxx class property.
     * Some virtual items (namely, object extension and shadow attributes; in future maybe metadata and construction)
     * have no JPA name. They are accessed directly from their base entity.
     */
    private String jpaName;

    /**
     * Multiplicity and (optionally) parameters used to construct a join to retrieve the values.
     */
    private CollectionSpecification collectionSpecification;

    public JpaItemDefinition(QName jaxbName, String jpaName, CollectionSpecification collectionSpecification, Class jpaClass) {
        super(jpaClass);
        Validate.notNull(jaxbName, "jaxbName");
        this.jaxbName = jaxbName;
        this.jpaName = jpaName;
        this.collectionSpecification = collectionSpecification;
    }

    public QName getJaxbName() {
        return jaxbName;
    }

    public String getJpaName() {
        return jpaName;
    }

    public CollectionSpecification getCollectionSpecification() {
        return collectionSpecification;
    }

    public boolean isMultivalued() {
        return collectionSpecification != null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpClassName()).append(": ").append(getJaxbName());
        if (collectionSpecification != null) {
            sb.append(collectionSpecification.getShortInfo());
        }
        sb.append(" => ").append(jpaName).append(" (").append(getJpaClass().getSimpleName()).append(")");
        debugDumpExtended(sb, indent);
        return sb.toString();
    }

    protected abstract void debugDumpExtended(StringBuilder sb, int indent);
}
