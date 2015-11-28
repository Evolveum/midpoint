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

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class JpaReferenceDefinition extends JpaItemDefinition {

    private JpaRootEntityDefinition referencedEntityDefinition;                // lazily evaluated
    private Class referencedEntityJpaClass;
    private boolean embedded;

    public JpaReferenceDefinition(QName jaxbName, String jpaName, CollectionSpecification collectionSpecification,
                                  Class jpaClass, Class referencedEntityJpaClass, boolean embedded) {
        super(jaxbName, jpaName, collectionSpecification, jpaClass);
        this.embedded = embedded;
        Validate.notNull(referencedEntityJpaClass, "referencedEntityJpaClass");
        this.referencedEntityJpaClass = referencedEntityJpaClass;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    @Override
    protected void debugDumpExtended(StringBuilder builder, int indent) {
        builder.append(", embedded=").append(isEmbedded()).append(", referencedEntity: ").append(referencedEntityDefinition);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ref";
    }

    @Override
    public DefinitionSearchResult nextDefinition(ItemPath path) {
        if (path.first() instanceof ObjectReferencePathSegment) {
            // returning artificially created item definition, used to allow dereferencing target object in a generic way
            return new DefinitionSearchResult(
                    new JpaEntityItemDefinition(PrismConstants.T_OBJECT_REFERENCE, "target", null,
                            referencedEntityDefinition.getJpaClass(), referencedEntityDefinition.getContent(), false),
                    path.tail());
        } else {
            return null;
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public void setReferencedEntityDefinition(JpaRootEntityDefinition realEntDef) {
        this.referencedEntityDefinition = realEntDef;
    }

    public JpaRootEntityDefinition getReferencedEntityDefinition() {
        return referencedEntityDefinition;
    }

    public Class getReferencedEntityJpaClass() {
        return referencedEntityJpaClass;
    }
}
