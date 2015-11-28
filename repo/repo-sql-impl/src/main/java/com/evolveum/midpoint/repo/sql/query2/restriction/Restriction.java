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
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpreterHelper;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityItemDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;

/**
 * An image of an ObjectFilter, forming a restriction tree.
 * Preserves some state related to the interpretation (translation).
 * Provides functionality related to the translation.
 *
 * As for the state, we maintain (or, more precisely, we are able to determine) the following:
 *  - base ItemPath for the restriction
 *  - base HQL property path for the restriction
 *  - base EntityDefinition for the restriction
 *  (plus, as a convenience method, the same for children of this restriction)
 *
 *  Most restrictions do not change the above properties when propagating them to their children.
 *  However, Type and Exists restrictions do.
 *  The former changes EntityDefinition, the latter all the three.
 *
 *  Also, item-related restrictions may, at their own discretion, use an EntityDefinition that
 *  points to a subclass of the one provided by the parent restriction. They do that if the item
 *  referenced (e.g. location) is not available in the provided entity (e.g. RObject or RFocus or RAbstractRole),
 *  but has to be found deeper.
 *
 *  There is one known problem in this respect, though: locality attribute. It is present in UserType as well as
 *  in OrgType. So, if having query like ObjectType: Equals(locality, 'abc') it might be narrowed
 *  as o.localityUser or o.locality. (Maybe there are other attributes like that, I don't know.)
 *
 * @author lazyman
 * @author mederly
 */
public abstract class Restriction<T extends ObjectFilter> {

    protected InterpretationContext context;
    protected Restriction parent;
    protected T filter;

    /**
     * Entity definition to be used as "root" by this restriction and its children.
     *
     * Looks a bit weird, but entity definition is provided to the restriction by the caller (query interpreter).
     * Actually, the caller has to determine it, because it needs it to know what restriction to instantiate.
     * (TODO reconsider this)
     */
    protected JpaEntityDefinition baseEntityDefinition;

    public Restriction(InterpretationContext context, T filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        Validate.notNull(context, "context");
        Validate.notNull(filter, "filter");
        Validate.notNull(baseEntityDefinition, "baseEntityDefinition");
        this.context = context;
        this.filter = filter;
        this.parent = parent;
        this.baseEntityDefinition = baseEntityDefinition;
    }

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
     * HQL property path of the object to which this restriction is to be applied.
     *
     * Usually, e.g. for simple cases like UserType: name, UserType: activation/administrativeStatus etc. the root
     * is the query primary entity alias (e.g. u in "RUser u").
     *
     * For "Exists" filters children the base corresponds to the base item pointed to by the filter.
     * E.g.
     *  - in "UserType: Exists (assignment)" it is "a" (provided that there is
     *    "RUser u left join u.assignments a with ..." already defined).
     *  - in "UserType: Exists (assignment/activation)" it is "a.activation"
     *    [although note that using single-valued properties as last path item
     *    in "Exists" filter is only a syntactic sugar]
     */
    public String getBaseHqlPath() {
        if (parent == null) {
            return context.getHibernateQuery().getPrimaryEntityAlias();
        } else {
            return parent.getBaseHqlPathForChildren();
        }
    }

    /**
     * HQL property path for children of this restriction.
     * By default it is the same as base path for this one; however subclasses (namely Exists) may change that.
     */
    public String getBaseHqlPathForChildren() {
        return getBaseHqlPath();
    }

    public ItemPath getBaseItemPath() {
        if (parent == null) {
            return ItemPath.EMPTY_PATH;
        } else {
            return parent.getBaseItemPathForChildren();
        }
    }

    /**
     * Returns the item path that is the "base" path for any child restrictions.
     * Default is the empty path.
     *
     * Currently, only the ForValue filter/restriction changes this path. E.g. for UserType: ForValue (assignment)
     * changes the base path for its children to "assignment".
     */
    public ItemPath getBaseItemPathForChildren() {
        return getBaseItemPath();
    }

    /**
     * Given the (relative) path, returns full path of an item to be used in a restriction.
     *
     * @param path
     * @return
     */
    protected ItemPath getFullPath(ItemPath path) {
       return new ItemPath(parent.getBaseItemPath(), path);
    }

    public JpaEntityDefinition getBaseEntityDefinition() {
        return baseEntityDefinition;
    }

    public JpaEntityDefinition getBaseEntityDefinitionForChildren() {
        return getBaseEntityDefinition();
    }

    protected InterpreterHelper getHelper() {
        return getContext().getHelper();
    }

//    /**
//     * Finds TypeFilter that is applicable in a current context.
//     * Simple algorithm is to check all the parents.
//     * More complex one (not yet implemented) would be to check also sibling restrictions in an AndRestriction, if
//     * there's any.
//     *
//     * @return
//     */
//    public TypeFilter findApplicableTypeFilter() {
//        if (parent != null) {
//            return parent.findApplicableTypeFilter();
//        } else {
//            return null;
//        }
//    }

}
