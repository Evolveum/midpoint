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

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class JpaEntityItemDefinition extends JpaItemDefinition implements JpaEntityDefinition {

    private boolean embedded;
    private JpaEntityContentDefinition content;

    public JpaEntityItemDefinition(QName jaxbName, String jpaName, CollectionSpecification collectionSpecification, Class jpaClass, JpaEntityContentDefinition content, boolean embedded) {
        super(jaxbName, jpaName, collectionSpecification, jpaClass);
        Validate.notNull(content, "content");
        this.content = content;
        this.embedded = embedded;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public JpaEntityContentDefinition getContent() {
        return content;
    }

    @Override
    protected void debugDumpExtended(StringBuilder sb, int indent) {
        sb.append(", embedded=").append(embedded).append("\n");
        sb.append(content.debugDump(indent + 1));
    }

    @Override
    public DefinitionSearchResult nextDefinition(ItemPath path) throws QueryException {
        return content.nextDefinition(this, path);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Ent";
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        content.accept(visitor);
    }

    // the following methods are duplicate with JpaRootEntityDefinition
    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(getJpaClass().getModifiers());
    }

    @Override
    public boolean isAssignableFrom(JpaEntityDefinition specificEntityDefinition) {
        return getJpaClass().isAssignableFrom(specificEntityDefinition.getJpaClass());
    }
}
