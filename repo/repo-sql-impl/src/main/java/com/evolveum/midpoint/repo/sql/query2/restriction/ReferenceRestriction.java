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

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.ReferenceDefinition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
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

    @Override
    public Criterion interpretInternal(RefFilter filter) throws QueryException {
        List<? extends PrismValue> values = filter.getValues();
        PrismReferenceValue refValue = null;
        if (values != null && !values.isEmpty()) {
            refValue = (PrismReferenceValue) values.get(0);
        }

        if (refValue == null) {
            throw new QueryException("Ref filter '" + filter + "' doesn't contain reference value.");
        }

        String prefix = createPropertyNamePrefix(filter);

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(handleEqOrNull(prefix + ObjectReference.F_TARGET_OID, refValue.getOid()));

        if (refValue.getRelation() != null) {
            QName relation = refValue.getRelation();

            conjunction.add(handleEqOrNull(prefix + ObjectReference.F_RELATION_NAMESPACE, relation.getNamespaceURI()));
            conjunction.add(handleEqOrNull(prefix + ObjectReference.F_RELATION_LOCAL_PART, relation.getLocalPart()));
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

    private String createPropertyNamePrefix(RefFilter filter) throws QueryException {
        QueryContext context = getContext();

        StringBuilder sb = new StringBuilder();
        String alias = context.getAlias(createFullPath(filter));
        if (StringUtils.isNotEmpty(alias)) {
            sb.append(alias);
            sb.append('.');
        }

        ItemPath path = createFullPath(filter);
        List<Definition> definitions = createDefinitionPath(path, context);
        ReferenceDefinition refDefinition = getReferenceDefinition(definitions, filter);
        if (refDefinition.isEmbedded()) {
            sb.append(refDefinition.getJpaName());
            sb.append('.');
        }

        return sb.toString();
    }

    private ReferenceDefinition getReferenceDefinition(List<Definition> definitions, RefFilter filter)
            throws QueryException {

        if (definitions.isEmpty()) {
            throw new QueryException("Can't find reference definition in empty definitions path, filter '"
                    + filter + "'.");
        }

        Definition definition = definitions.get(definitions.size() - 1);
        if (definition instanceof CollectionDefinition) {
            CollectionDefinition colDef = (CollectionDefinition) definition;
            definition = colDef.getDefinition();
        }

        if (!(definition instanceof ReferenceDefinition)) {
            throw new QueryException("Definition '" + definition + "' is not reference definition.");
        }

        return (ReferenceDefinition) definition;
    }

    private Criterion handleEqOrNull(String propertyName, Object value) {
        if (value == null) {
            return Restrictions.isNull(propertyName);
        }

        return Restrictions.eq(propertyName, value);
    }
}
