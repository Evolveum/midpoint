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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;

import java.lang.reflect.Field;
import java.util.Collections;

/**
 * @author lazyman
 */
public class HibernateToSqlTranslator {

    /**
     * Do not use in production code! Only for testing purposes only. Used for example during query engine upgrade.
     * Method provides translation from hibernate {@link Criteria} to plain SQL string query.
     *
     * @param criteria
     * @return SQL string, null if criteria parameter was null.
     */
    public static String toSql(Criteria criteria) {
        if (criteria == null) {
            return null;
        }

        try {
            CriteriaImpl c;
            if (criteria instanceof CriteriaImpl) {
                c = (CriteriaImpl) criteria;
            } else {
                CriteriaImpl.Subcriteria subcriteria = (CriteriaImpl.Subcriteria) criteria;
                c = (CriteriaImpl) subcriteria.getParent();
            }
            SessionImpl s = (SessionImpl) c.getSession();
            SessionFactoryImplementor factory = s.getSessionFactory();
            String[] implementors = factory.getImplementors(c.getEntityOrClassName());
            CriteriaLoader loader = new CriteriaLoader((OuterJoinLoadable) factory.getEntityPersister(implementors[0]),
                    factory, c, implementors[0], s.getLoadQueryInfluencers());
            Field f = OuterJoinLoader.class.getDeclaredField("sql");
            f.setAccessible(true);
            return (String) f.get(loader);
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    /**
     * Do not use in production code! Only for testing purposes only. Used for example during query engine upgrade.
     * Method provides translation from hibernate HQL query to plain SQL string query.
     *
     * @param sessionFactory
     * @param hqlQueryText
     * @return SQL string, null if hqlQueryText parameter is empty.
     */
    public static String toSql(SessionFactory sessionFactory, String hqlQueryText) {
        Validate.notNull(sessionFactory, "Session factory must not be null.");

        if (StringUtils.isEmpty(hqlQueryText)) {
            return null;
        }

        final QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();
        final SessionFactoryImplementor factory =
                (SessionFactoryImplementor) sessionFactory;
        final QueryTranslator translator = translatorFactory.
                createQueryTranslator(
                        hqlQueryText,
                        hqlQueryText,
                        Collections.EMPTY_MAP, factory
                );
        translator.compile(Collections.EMPTY_MAP, false);
        return translator.getSQLString();
    }
}
