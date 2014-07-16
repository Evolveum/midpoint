/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
        ItemPath fullPath = valFilter.getFullPath();

        List<Definition> defPath = createDefinitionPath(fullPath);
        return containsAnyDefinition(defPath)
                || (fullPath.first().equivalent(new NameItemPathSegment(ObjectType.F_EXTENSION)))
                || (fullPath.first().equivalent(new NameItemPathSegment(ShadowType.F_ATTRIBUTES)));
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
            addNewCriteriaToContext(anyItemPath, null, anyTypeName.getLocalPart());
        }
        String propertyNamePrefix = getContext().getAlias(anyItemPath) + '.';

        Conjunction conjunction = Restrictions.conjunction();

        RObjectExtensionType ownerType = filter.getFullPath().first().equivalent(new NameItemPathSegment(ObjectType.F_EXTENSION)) ?
                RObjectExtensionType.EXTENSION : RObjectExtensionType.ATTRIBUTES;
        conjunction.add(Restrictions.eq(propertyNamePrefix + "ownerType", ownerType));

        conjunction.add(Restrictions.eq(propertyNamePrefix + RAnyValue.F_NAME, RUtil.qnameToString(name)));

        Object testedValue = getValue(((PropertyValueFilter) filter).getValues());
        Object value = RAnyConverter.getAggregatedRepoObject(testedValue);
        conjunction.add(createCriterion(propertyNamePrefix + RAnyValue.F_VALUE, value, filter));

        return conjunction;
    }

    private ItemPath createAnyItemPath(ItemPath path, ItemDefinition itemDef) throws QueryException {
        try {
            List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
//            segments.addAll(path.getSegments());
            // get any type name (e.g. clobs, strings, dates,...) based on definition
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
