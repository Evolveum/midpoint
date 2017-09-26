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

import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.MatchMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author mederly
 */
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
	    if (words.length == 0) {
	    	throw new QueryException("No words to query for using full-text search filter");
	    }
	    List<Condition> conditions = new ArrayList<>(words.length);
	    for (String word : words) {
		    conditions.add(createWordQuery(word));
	    }
	    if (conditions.size() == 1) {
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
