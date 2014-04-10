package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import java.util.Collection;

/**
 * @author lazyman
 */
public class QueryEngine {

    private SqlRepositoryConfiguration repoConfiguration;
    private PrismContext prismContext;

    public QueryEngine(SqlRepositoryConfiguration config, PrismContext prismContext) {
        this.repoConfiguration = config;
        this.prismContext = prismContext;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends ObjectType> type,
                              Collection<SelectorOptions<GetOperationOptions>> options,
                              boolean countingObjects, Session session) throws QueryException {

        //todo search some query library for query filter match

        //todo implement as query library search, this is just a proof of concept [lazyman]
        if (query != null && query.getFilter() != null && query.getFilter() instanceof OrgFilter) {
            // http://stackoverflow.com/questions/10515391/oracle-equivalent-of-postgres-distinct-on
            // select distinct col1, first_value(col2) over (partition by col1 order by col2 asc) from tmp
            RQuery q =  createOrgQuery((OrgFilter) query.getFilter(), type, countingObjects, session);
            if (q != null) {
                return q;
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

    private RQueryImpl createOrgQuery(OrgFilter filter, Class<? extends ObjectType> type, boolean countingObjects,
                                      Session session) {
        if (filter.isRoot()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        if (countingObjects) {
            sb.append("count(*) ");
        } else {
            sb.append("o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount ");
        }
        sb.append("from ").append(ClassMapper.getHQLType(type)).append(" as o left join o.descendants as d ");
        sb.append("where d.ancestorOid = :aOid ");
        if (filter.getMaxDepth() != null) {
            if (filter.getMaxDepth() == 1) {
                sb.append("and d.depth = :maxDepth ");
            } else {
                sb.append("and d.depth <=:maxDepth and d.depth>:minDepth ");
            }
        }

        if (countingObjects) {
            sb.append("group by o.oid");
        } else {
            sb.append("group by o.fullObject, o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount, o.name.orig order by o.name.orig asc");
        }

        Query query = session.createQuery(sb.toString());
        query.setString("aOid", filter.getOrgRef().getOid());
        if (filter.getMaxDepth() != null) {
            if (filter.getMaxDepth() != 1) {
                int minDepth = filter.getMinDepth() == null ? 1 : filter.getMinDepth();
                query.setInteger("minDepth", minDepth);
            }
            query.setInteger("maxDepth", filter.getMaxDepth());
        }
        if (!countingObjects) {
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        }

//        Query query = session.createQuery(
//                "select o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount from "
//                        + ClassMapper.getHQLType(UserType.class) + " as o left join o.descendants as d "
//                        + "where d.ancestorOid=:aOid and d.depth <=:maxDepth and d.depth>:minDepth "
//                        + "group by o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount, o.name.orig "
//                        + "order by o.name.orig asc");





//        SELECT o.fullobject,
//                o.stringscount,
//                o.longscount,
//                o.datescount,
//                o.referencescount,
//                o.polyscount,
//                o.name_orig
//        FROM   m_object o INNER JOIN m_org_closure c ON o.oid = c.descendant_oid
//        WHERE  ( c.ancestor_oid = '' AND c.depthvalue <=1 AND c.depthvalue >0)
//        GROUP  BY o.name_orig, o.fullobject,o.stringscount, o.longscount,
//                o.datescount,
//                o.referencescount,
//                o.polyscount ORDER  BY o.name_orig ASC
//        Query query = null;
//
//        session.createQuery(
//                "select o.fullobject,o.stringscount,o.longscount,o.datescount,o.referencescount,o.polyscount from "
//        + ClassMapper.getHQLType(type) + " as o left join o.descendants as d "
//        + "where d.ancestorOid=:aOid and d.depth <=:maxDepth and d.depth>:midDepth "
//        + "group by o.fullobject,o.stringscount,o.longscount,o.datescount,o.referencescount,o.polyscount "
//        + "order by o.name.orig asc");

//        SELECT o.fullobject,
//                o.stringscount,
//                o.longscount,
//                o.datescount,
//                o.referencescount,
//                o.polyscount,
//                o.name_orig
//        FROM   m_object o INNER JOIN m_org_closure c ON o.oid = c.descendant_oid
//        WHERE  ( c.ancestor_oid = '' AND c.depthvalue <=1 AND c.depthvalue >0)
//        GROUP  BY o.name_orig, o.fullobject,o.stringscount, o.longscount,
//                o.datescount,
//                o.referencescount,
//                o.polyscount ORDER  BY o.name_orig ASC

        return new RQueryImpl(query);
    }
}
