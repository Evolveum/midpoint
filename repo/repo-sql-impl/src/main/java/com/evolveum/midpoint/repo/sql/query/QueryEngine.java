package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.query.custom.CustomQuery;
import com.evolveum.midpoint.repo.sql.query.custom.ShadowQueryWithDisjunction;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class QueryEngine {

    private static final List<CustomQuery> queryLibrary = new ArrayList<>();

    static {
        queryLibrary.add(new ShadowQueryWithDisjunction());
//        queryLibrary.add(new OrgFilterQuery());
    }

    private SqlRepositoryConfiguration repoConfiguration;
    private PrismContext prismContext;

    public QueryEngine(SqlRepositoryConfiguration config, PrismContext prismContext) {
        this.repoConfiguration = config;
        this.prismContext = prismContext;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends ObjectType> type,
                            Collection<SelectorOptions<GetOperationOptions>> options,
                            boolean countingObjects, Session session) throws QueryException {

        for (CustomQuery custom : queryLibrary) {
            if (custom.match(query, type, options, countingObjects)) {
                custom.init(repoConfiguration, prismContext);

                RQuery rQuery = custom.createQuery(query, type, options, countingObjects, session);
                if (rQuery != null) {
                    return rQuery;
                }
            }
        }

        QueryInterpreter interpreter = new QueryInterpreter(repoConfiguration);
        Criteria criteria = interpreter.interpret(query, type, options, prismContext, countingObjects, session);
        if (countingObjects) {
            criteria.setProjection(Projections.rowCount());
        } else {
            criteria.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        }

        return new RQueryCriteriaImpl(criteria);
    }
}
