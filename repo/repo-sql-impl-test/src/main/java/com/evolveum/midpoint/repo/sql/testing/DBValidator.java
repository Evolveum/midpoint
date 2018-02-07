/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Arrays;
import java.util.List;

/**
 * This class provides simple validation for data stored in database. It does various selects and asserts data quality.
 * Validation occurs only when H2 is used and dropIfExists=true, only during tests.
 *
 * @author lazyman
 */
public class DBValidator {

    public static <T extends ObjectType> void validateOwners(Class<T> type, SqlRepositoryConfiguration config,
                                                             SessionFactory factory) {

        if (!config.isUsingH2() || !config.isDropIfExists()) {
            return;
        }

        Session session = factory.openSession();
        try {
            session.beginTransaction();

            validate("select e.owner_oid, e.eName from m_object_ext_string e where e.ownerType is null",
                    "extension string are null", session);
            validate("select e.owner_oid, e.eName from m_object_ext_poly e where e.ownerType is null",
                    "extension poly are null", session);
            validate("select e.owner_oid, e.eName from m_object_ext_long e where e.ownerType is null",
                    "extension long are null", session);
            validate("select e.owner_oid, e.eName from m_object_ext_date e where e.ownerType is null",
                    "extension date are null", session);
            validate("select e.owner_oid, e.eName from m_object_ext_reference e where e.ownerType is null",
                    "extension reference are null", session);

            if (FocusType.class.isAssignableFrom(type)) {
                validate("select a.owner_oid, a.id from m_assignment a where a.assignmentOwner is null",
                        "assignment owners are null", session);

                validate("select e.anyContainer_owner_owner_oid, e.anyContainer_owner_id, e.eName from m_assignment_ext_string e where e.extensionType is null",
                        "assignment extension string are null", session);
                validate("select e.anyContainer_owner_owner_oid, e.anyContainer_owner_id, e.eName from m_assignment_ext_poly e where e.extensionType is null",
                        "assignment extension poly are null", session);
                validate("select e.anyContainer_owner_owner_oid, e.anyContainer_owner_id, e.eName from m_assignment_ext_long e where e.extensionType is null",
                        "assignment extension long are null", session);
                validate("select e.anyContainer_owner_owner_oid, e.anyContainer_owner_id, e.eName from m_assignment_ext_date e where e.extensionType is null",
                        "assignment extension date are null", session);
                validate("select e.anyContainer_owner_owner_oid, e.anyContainer_owner_id, e.eName from m_assignment_ext_reference e where e.extensionType is null",
                        "assignment extension reference are null", session);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private static final void validate(String sql, String message, Session session) {
        Query query = session.createNativeQuery(sql);
        List nullAssignments = query.list();
        if (!nullAssignments.isEmpty()) {
            throw new SystemException(message + Arrays.deepToString(nullAssignments.toArray()));
        }
    }
}
