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
import com.evolveum.midpoint.repo.sql.query2.DataSearchResult;

/**
 * Special placeholder to allow for cross-references: entity definition that points to another entity.
 * (TODO)
 *
 * @author mederly
 */
public class JpaEntityPointerDefinition extends JpaDataNodeDefinition {

    private JpaEntityDefinition resolvedEntityDefinition;           // lazily evaluated

    public JpaEntityPointerDefinition(Class jpaClass) {
        super(jpaClass, null);
    }

    public JpaEntityDefinition getResolvedEntityDefinition() {
        return resolvedEntityDefinition;
    }

    public void setResolvedEntityDefinition(JpaEntityDefinition resolvedEntityDefinition) {
        this.resolvedEntityDefinition = resolvedEntityDefinition;
    }

    @Override
    public DataSearchResult<JpaDataNodeDefinition> nextLinkDefinition(ItemPath path) throws QueryException {
        return resolvedEntityDefinition.nextLinkDefinition(path);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "EntPtr";
    }

    @Override
    public String debugDump(int indent) {
        if (resolvedEntityDefinition == null) {
            return getShortInfo();
        } else {
            return getDebugDumpClassName() + ":" + resolvedEntityDefinition.getShortInfo();
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
