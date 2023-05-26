/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class FullTextRestriction extends Restriction<FullTextFilter> {

    public FullTextRestriction(InterpretationContext context, FullTextFilter filter,
            JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        // TODO implement multiple values
        if (filter.getValues().size() != 1) {
            throw new QueryException("FullText filter currently supports only a single string");
        }
        String text = filter.getValues().iterator().next();
        String normalized = getContext().getPrismContext().getDefaultPolyStringNormalizer().normalize(text);
        String[] words = StringUtils.split(normalized);
        List<Condition> conditions = new ArrayList<>(words.length);
        for (String word : words) {
            conditions.add(createWordQuery(word));
        }
        if (conditions.isEmpty()) {
            return createWordQuery("");                     // original behavior -> match all records (TODO return something like 'empty condition')
        } else if (conditions.size() == 1) {
            return conditions.get(0);
        } else {
            return getContext().getHibernateQuery().createAnd(conditions);
        }
    }

    private Condition createWordQuery(String word) throws QueryException {
        String textInfoItemsAlias = getItemPathResolver().addTextInfoJoin(getBaseHqlEntity().getHqlPath());
        String textPath = textInfoItemsAlias + "." + RObjectTextInfo.F_TEXT;
        return getContext().getHibernateQuery().createLike(textPath, word, MatchMode.ANYWHERE, false);
    }
}
