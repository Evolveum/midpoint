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
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.repo.sql.query2.resolution.DataSearchResult;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class JpaReferenceDefinition extends JpaDataNodeDefinition {

    private JpaEntityPointerDefinition referencedEntityDefinition;          // lazily evaluated

    public JpaReferenceDefinition(Class jpaClass, Class referencedEntityJpaClass) {
        super(jpaClass, null);          // JAXB class not important here
        Validate.notNull(referencedEntityJpaClass, "referencedEntityJpaClass");
        this.referencedEntityDefinition = new JpaEntityPointerDefinition(referencedEntityJpaClass);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ref";
    }

    @Override
    public DataSearchResult nextLinkDefinition(ItemPath path, ItemDefinition itemDefinition, PrismContext prismContext) {
        if (path.first() instanceof ObjectReferencePathSegment) {
            // returning artificially created transition definition, used to allow dereferencing target object in a generic way
            return new DataSearchResult(
                    new JpaLinkDefinition(new ObjectReferencePathSegment(), "target", null, false, referencedEntityDefinition.getResolvedEntityDefinition()),
                    path.tail());
        } else {
            return null;
        }
    }

    public JpaEntityPointerDefinition getReferencedEntityDefinition() {
        return referencedEntityDefinition;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        referencedEntityDefinition.accept(visitor);
    }

    @Override
    public String debugDump(int indent) {
        return super.getShortInfo() + ", target=" + getReferencedEntityDefinition();
    }

    @Override
    public String getShortInfo() {
        return super.getShortInfo() + "<" + referencedEntityDefinition.getJpaClassName() + ">";
    }
}
