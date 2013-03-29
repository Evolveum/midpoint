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
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.type.QNameType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends ItemRestriction<ValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(AnyPropertyRestriction.class);

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        ValueFilter valFilter = (ValueFilter) filter;
        ItemPath fullPath = createFullPath(valFilter);

        List<Definition> defPath = createDefinitionPath(fullPath, context);
        return containsAnyDefinition(defPath);
    }

    private boolean containsAnyDefinition(List<Definition> definitions) {
        for (Definition definition : definitions) {
            if (definition instanceof AnyDefinition) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Criterion interpretInternal(ValueFilter filter) throws QueryException {

        ItemDefinition itemDefinition = filter.getDefinition();
        QName name = itemDefinition.getName();
        QName type = itemDefinition.getTypeName();

        if (name == null || type == null) {
            throw new QueryException("Couldn't get name or type for queried item '" + itemDefinition + "'");
        }

        ItemPath anyItemPath = createAnyItemPath(filter.getParentPath(), filter.getDefinition());
        if (!getContext().hasAlias(anyItemPath)) {
            QName anyTypeName = ((NameItemPathSegment) anyItemPath.last()).getName();
            LOGGER.trace("Condition item is from 'any' container, adding new criteria based on any type '{}'",
                    new Object[]{anyTypeName.getLocalPart()});
            addNewCriteriaToContext(anyItemPath, anyTypeName.getLocalPart());
        }
        String propertyNamePrefix = getContext().getAlias(anyItemPath) + '.';

        Conjunction conjunction = Restrictions.conjunction();

        Object testedValue = getValue(((PropertyValueFilter) filter).getValues());
        Object value = RAnyConverter.getAggregatedRepoObject(testedValue);
        conjunction.add(createCriterion(propertyNamePrefix + RAnyValue.F_VALUE, value, filter));

        conjunction.add(Restrictions.eq(propertyNamePrefix + RAnyValue.F_NAME, QNameType.optimizeQName(name)));
        conjunction.add(Restrictions.eq(propertyNamePrefix + RAnyValue.F_TYPE, QNameType.optimizeQName(type)));

        return conjunction;
    }

    private ItemPath createAnyItemPath(ItemPath path, ItemDefinition itemDef) throws QueryException {
        try {
            List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
            segments.addAll(path.getSegments());
            // get any type name (e.g. clobs, strings, dates,...) based
            // on definition
            String anyTypeName = RAnyConverter.getAnySetType(itemDef);
            segments.add(new NameItemPathSegment(new QName(RUtil.NS_SQL_REPO, anyTypeName)));

            return new ItemPath(segments);
        } catch (SchemaException ex) {
            throw new QueryException(ex.getMessage(), ex);
        }
    }

    @Override
    public AnyPropertyRestriction cloneInstance() {
        return new AnyPropertyRestriction();
    }
}
