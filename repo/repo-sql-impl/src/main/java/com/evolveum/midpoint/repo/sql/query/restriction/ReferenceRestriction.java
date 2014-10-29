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

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.Definition;
import com.evolveum.midpoint.repo.sql.query.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.ReferenceDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class ReferenceRestriction extends ItemRestriction<RefFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) {
        if (filter instanceof RefFilter) {
            return true;
        }
        return false;
    }

    // modelled after PropertyRestriction.interpretInternal, with some differences
    @Override
    public Criterion interpretInternal(RefFilter filter) throws QueryException {

        // let's check the value (null is not supported yet)
        List<? extends PrismValue> values = filter.getValues();
        if (values.size() > 1) {
            throw new QueryException("Ref filter '" + filter + "' contain more than one reference value (which is not supported for now).");
        }
        PrismReferenceValue refValue = null;
        if (values != null && !values.isEmpty()) {
            refValue = (PrismReferenceValue) values.get(0);
        }
        if (refValue == null) {
            throw new QueryException("Ref filter '" + filter + "' doesn't contain reference value.");
        }

        QueryContext context = getContext();

        ItemPath fullPath = filter.getFullPath();
        // actually, we cannot look for ReferenceDefinition here, because e.g. linkRef has a CollectionDefinition
        Definition def = findProperDefinition(fullPath, Definition.class);
        if (def == null) {
            throw new QueryException("Definition for " + fullPath + " couldn't be found.");
        }

        // ugly hacking, todo refactor!
        StringBuilder sb = new StringBuilder();
        if (def instanceof ReferenceDefinition) {
            String alias = context.getAlias(filter.getParentPath());
            if (StringUtils.isNotEmpty(alias)) {
                sb.append(alias);
                sb.append('.');
            }
            sb.append(createPropertyOrReferenceNamePrefix(filter.getPath()));          // i'm not sure about this [mederly]
            String referenceName = def.getJpaName();
            sb.append(referenceName);
            sb.append(".");
        } else {
            String alias = context.getAlias(filter.getPath());
            if (StringUtils.isNotEmpty(alias)) {
                sb.append(alias);
                sb.append('.');
            }
            sb.append(createPropertyOrReferenceNamePrefix(filter.getPath()));          // i'm not sure about this [mederly]
        }
        String prefix = sb.toString();

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(handleEqOrNull(prefix + ObjectReference.F_TARGET_OID, refValue.getOid()));

        if (refValue.getRelation() != null) {
            QName relation = refValue.getRelation();

            conjunction.add(handleEqOrNull(prefix + ObjectReference.F_RELATION, RUtil.qnameToString(relation)));
        }

        if (refValue.getTargetType() != null) {
            conjunction.add(handleEqOrNull(prefix + ObjectReference.F_TYPE,
                    ClassMapper.getHQLTypeForQName(refValue.getTargetType())));
        }

        return conjunction;
    }

    @Override
    public ReferenceRestriction cloneInstance() {
        return new ReferenceRestriction();
    }

    private Criterion handleEqOrNull(String propertyName, Object value) {
        if (value == null) {
            return Restrictions.isNull(propertyName);
        }

        return Restrictions.eq(propertyName, value);
    }
}
