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
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNotNullCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNullCondition;
import org.hibernate.Session;

import java.util.List;

/**
 * An image of an ObjectFilter, forming a restriction tree.
 * Preserves some state related to the interpretation (translation).
 * Provides functionality related to the translation.
 *
 * @author lazyman
 * @author mederly
 */
public abstract class Restriction<T extends ObjectFilter> {

    protected InterpretationContext context;
    protected Restriction parent;
    protected T filter;

    public T getFilter() {
        return filter;
    }

    public void setFilter(T filter) {
        this.filter = filter;
    }

    public InterpretationContext getContext() {
        return context;
    }

    public void setContext(InterpretationContext context) {
        this.context = context;
    }

    public Session getSession() {
        return context.getSession();
    }

    public Restriction getParent() {
        return parent;
    }

    public void setParent(Restriction parent) {
        this.parent = parent;
    }

    public abstract Condition interpret() throws QueryException;

    protected boolean isNegated() {
        return filter instanceof NotFilter || (parent != null && parent.isNegated());
    }

    protected String nameOf(Enum e) {
        return e.getClass().getName() + "." + e.name();
    }

    /**
     * Returns the item path that is the "base" path for any child restrictions.
     * Default is the empty path.
     *
     * Currently, only the ForValue filter/restriction changes this path. E.g. for UserType: ForValue (assignment)
     * changes the base path for its children to "assignment".
     */
    public ItemPath getItemPathForChildren() {
        if (parent != null) {
            return parent.getItemPathForChildren();         // by default, restrictions don't change the reference path
        } else {
            return ItemPath.EMPTY_PATH;
        }
    }

    /**
     * Given the (relative) path, returns full path of an item to be used in a restriction.
     *
     * @param path
     * @return
     */
    protected ItemPath getFullPath(ItemPath path) {
       if (parent != null) {
           return new ItemPath(parent.getItemPathForChildren(), path);
       } else {
           return path;
       }
   }
}
