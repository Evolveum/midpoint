/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.repo.spring;

import com.evolveum.midpoint.model.SimpleDomainObject;
import com.evolveum.midpoint.repo.PagingRepositoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;

/**
 * TODO
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public interface GenericDao {

    List<SimpleDomainObject> findAll();

    List<SimpleDomainObject> findAllOfType(final String dtype);

    List<SimpleDomainObject> findRangeUsers(final String dtype, final PagingRepositoryType paging);

    SimpleDomainObject findById(UUID id);

    SimpleDomainObject flush(SimpleDomainObject entity);

    SimpleDomainObject merge(SimpleDomainObject entity);

    void persist(SimpleDomainObject entity);

    void refresh(SimpleDomainObject entity);

    void remove(SimpleDomainObject entity);

    //void remove(String oid);

    Integer removeAll();

    public Object execute(final String query);
}
