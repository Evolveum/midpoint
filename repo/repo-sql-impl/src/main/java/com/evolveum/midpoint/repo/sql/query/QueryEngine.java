package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import java.util.Collection;

/**
 * @author lazyman
 */
public class QueryEngine {

    private PrismContext prismContext;

    public QueryEngine(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends ObjectType> type,
                              Collection<SelectorOptions<GetOperationOptions>> options,
                              boolean countingObjects, Session session) throws QueryException {

        //todo search some query library for query filter match


        QueryInterpreter interpreter = new QueryInterpreter();
        Criteria criteria = interpreter.interpret(query, type, options, prismContext, countingObjects, session);
        if (countingObjects) {
            criteria.setProjection(Projections.rowCount());
        } else {
            criteria.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        }

        return new RQueryCriteriaImpl(criteria);
    }
}
