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

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class GeneralHelper {

    public int findLastIdInRepo(Session session, String tableOid, String queryName) {
        Query query = session.getNamedQuery(queryName);
        query.setParameter("oid", tableOid);
        Integer lastId = (Integer) query.uniqueResult();
        if (lastId == null) {
            lastId = 0;
        }
        return lastId;
    }

    public <C extends Containerable> void validateContainerable(C value, Class<C> type)
            throws SchemaException {
        if (value == null) {
            throw new SchemaException("Null object as a result of repository get operation for " + type);
        }
        Class<? extends Containerable> realType = value.getClass();
        if (!type.isAssignableFrom(realType)) {
            throw new SchemaException("Expected to find '" + type.getSimpleName() + "' but found '" + realType.getSimpleName());
        }
        // TODO call check consistence if possible
    }


}
