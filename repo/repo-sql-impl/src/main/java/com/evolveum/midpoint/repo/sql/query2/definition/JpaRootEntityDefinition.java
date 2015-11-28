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
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.DefinitionSearchResult;
import com.evolveum.midpoint.util.DebugUtil;
import org.apache.commons.lang.Validate;

import java.lang.reflect.Modifier;

/**
 * @author mederly
 */
public class JpaRootEntityDefinition extends JpaDefinition implements JpaEntityDefinition {

    private JpaEntityContentDefinition content;

    public JpaRootEntityDefinition(Class jpaClass, JpaEntityContentDefinition content) {
        super(jpaClass);
        Validate.notNull(content, "content");
        this.content = content;
    }

    public JpaEntityContentDefinition getContent() {
        return content;
    }

    public boolean isAssignableFrom(JpaEntityDefinition specificEntityDefinition) {
        return getJpaClass().isAssignableFrom(specificEntityDefinition.getJpaClass());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getJpaClass().getModifiers());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        content.accept(visitor);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "RootEnt";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpClassName()).append(":").append(getJpaClass().getSimpleName()).append(":\n");
        sb.append(content.debugDump(indent + 1));
        return sb.toString();
    }

    @Override
    public DefinitionSearchResult nextDefinition(ItemPath path) throws QueryException {
        return content.nextDefinition(this, path);
    }
}
