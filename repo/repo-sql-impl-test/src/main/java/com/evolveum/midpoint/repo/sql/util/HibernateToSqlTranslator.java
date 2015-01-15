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
                        Collections.EMPTY_MAP, factory, null
                );
        translator.compile(Collections.EMPTY_MAP, false);
        return translator.getSQLString();
    }
}
