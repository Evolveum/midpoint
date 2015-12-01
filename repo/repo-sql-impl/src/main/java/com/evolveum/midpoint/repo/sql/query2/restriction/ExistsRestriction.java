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
import com.evolveum.midpoint.repo.sql.query2.ItemPathResolutionState;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class ExistsRestriction extends ItemRestriction<ExistsFilter> {

    public ExistsRestriction(InterpretationContext context, ExistsFilter filter, JpaEntityDefinition baseEntityDefinition,
                             Restriction parent) {
        super(context, filter, filter.getFullPath(), baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        ItemPathResolutionState resolutionState = getItemPathResolver().resolveItemPath(filter.getFullPath(), getBaseHqlPath(), baseEntityDefinition, false);
        if (!(resolutionState.getCurrentJpaNode() instanceof JpaEntityDefinition)) {
            // should be checked when instantiating this restriction
            throw new IllegalStateException("Internal error - resolutionState for ExistsRestriction points to non-entity node: " + resolutionState.getCurrentJpaNode());
        }
        setItemResolutionState(resolutionState);

        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();
        return interpreter.interpretFilter(context, filter.getFilter(), this);
    }

    @Override
    public String getBaseHqlPathForChildren() {
        return getItemResolutionState().getCurrentHqlPath();
    }

    @Override
    public ItemPath getBaseItemPathForChildren() {
        return new ItemPath(getBaseItemPath(), filter.getFullPath());
    }

    @Override
    public JpaEntityDefinition getBaseEntityDefinitionForChildren() {
        return (JpaEntityDefinition) getItemResolutionState().getCurrentJpaNode();
    }

    @Override
    public ItemPathResolutionState getItemPathResolutionStateForChildren() {
        return getItemResolutionState();
    }
}
