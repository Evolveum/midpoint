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

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class ExistsRestriction extends ItemRestriction<ExistsFilter> {

    /**
     * Definition of the entity when this restriction starts. It is usually the same as rootEntityDefinition,
     * but for Exists children it is the entity pointed to by Exists restriction.
     *
     * TODO think out the process of refinement of entity definition e.g. RObject->RUser
     */
    private EntityDefinition baseEntityDefinitionForChildren;

    /**
     * HQL path to be used for child restrictions.
     */
    private String baseHqlPathForChildren;

    public ExistsRestriction(InterpretationContext context, ExistsFilter filter, EntityDefinition baseEntityDefinition,
                             Restriction parent, EntityDefinition baseEntityDefinitionForChildren) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(baseEntityDefinitionForChildren, "baseEntityDefinitionForChildren");
        this.baseEntityDefinitionForChildren = baseEntityDefinitionForChildren;
    }

    @Override
    public Condition interpret() throws QueryException {
        baseHqlPathForChildren = getHelper().prepareJoins(filter.getFullPath(), getBaseHqlPath(), baseEntityDefinition);

        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();
        return interpreter.interpretFilter(context, filter.getFilter(), this);
    }

    @Override
    public String getBaseHqlPathForChildren() {
        return baseHqlPathForChildren;
    }

    @Override
    public ItemPath getBaseItemPathForChildren() {
        return new ItemPath(getBaseItemPath(), filter.getFullPath());
    }

    @Override
    public EntityDefinition getBaseEntityDefinitionForChildren() {
        return baseEntityDefinitionForChildren;
    }
}
