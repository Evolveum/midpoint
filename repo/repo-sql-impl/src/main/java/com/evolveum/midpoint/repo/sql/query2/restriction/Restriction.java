/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import org.hibernate.criterion.Criterion;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class Restriction<T extends ObjectFilter> {

    private QueryContext context;
    private Restriction parent;
    private ObjectQuery query;

    public QueryContext getContext() {
        return context;
    }

    public void setContext(QueryContext context) {
        this.context = context;
    }

    public Restriction getParent() {
        return parent;
    }

    public void setParent(Restriction parent) {
        this.parent = parent;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public abstract Criterion interpret(T filter) throws QueryException;

    public abstract boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException;

    /**
     * This method creates full {@link ItemPath} from {@link ValueFilter} created from
     * main item path and last element, which is now definition.
     * <p/>
     * Will be deleted after query api update (that will be after query v2 interpreter release)
     *
     * @param filter
     * @return
     */
    @Deprecated
    protected ItemPath createFullPath(ValueFilter filter) {
        ItemDefinition def = filter.getDefinition();
        ItemPath parentPath = filter.getParentPath();

        List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
        if (parentPath != null) {
            for (ItemPathSegment segment : parentPath.getSegments()) {
                if (!(segment instanceof NameItemPathSegment)) {
                    continue;
                }

                NameItemPathSegment named = (NameItemPathSegment) segment;
                segments.add(new NameItemPathSegment(named.getName()));
            }
        }
        segments.add(new NameItemPathSegment(def.getName()));

        return new ItemPath(segments);
    }

    // todo don't know if cloning is necessary.. [lazyman]
    public abstract Restriction cloneInstance();
}
