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

import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.resolution.ItemPathResolver;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.jetbrains.annotations.NotNull;

/**
 *  An image of an ObjectFilter, forming a restriction tree.
 *  Preserves some state related to the interpretation (translation).
 *  Provides functionality related to the translation.
 *
 *  As for the state, we maintain (or, more precisely, we are able to determine) the following:
 *   - base HQL property path for the restriction
 *   - base EntityDefinition for the restriction
 *   - chain of translation states that led to the starting HQL property path (if applicable)
 *  This is stored in baseHqlEntity.
 *
 *  Most restrictions do not change the above properties when propagating them to their children.
 *  However, Type and Exists restrictions do.
 *  The former changes EntityDefinition, the latter all three.
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

    @NotNull final protected InterpretationContext context;
    final protected Restriction parent;
    @NotNull final protected T filter;

    /**
     * Content of ItemPathResolutionStartInformation:
     *
     * (1) Entity definition to be used as "root" by this restriction and its children.
     *
     * Looks a bit weird, but entity definition is provided to the restriction by the caller (query interpreter).
     * Actually, the caller has to determine it, because it needs it to know what restriction to instantiate.
     *
     * (2) HQL property path of the object to which this restriction is to be applied.
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
     *
     * (3) List of translation steps that had led to the HQL property path. Useful for following ".."
     * segments in item paths.
     *
     */
    private final HqlEntityInstance baseHqlEntity;

    public Restriction(@NotNull InterpretationContext context, @NotNull T filter, @NotNull JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        this.context = context;
        this.filter = filter;
        this.parent = parent;
        if (parent != null) {
            baseHqlEntity = parent.getBaseHqlEntityForChildren().narrowFor(baseEntityDefinition);
        } else {
            baseHqlEntity = new HqlEntityInstance(context.getPrimaryEntityAlias(), baseEntityDefinition, null);
        }
    }

    @NotNull
    public T getFilter() {
        return filter;
    }

    @NotNull
    public InterpretationContext getContext() {
        return context;
    }

    public Restriction getParent() {
        return parent;
    }

    public HqlEntityInstance getBaseHqlEntity() {
        return baseHqlEntity;
    }

    public abstract Condition interpret() throws QueryException;

    protected boolean isNegated() {
        return filter instanceof NotFilter || (parent != null && parent.isNegated());
    }

    protected String nameOf(Enum e) {
        return e.getClass().getName() + "." + e.name();
    }

    public HqlEntityInstance getBaseHqlEntityForChildren() {
        return getBaseHqlEntity();
    }

    protected ItemPathResolver getItemPathResolver() {
        return getContext().getItemPathResolver();
    }

}
