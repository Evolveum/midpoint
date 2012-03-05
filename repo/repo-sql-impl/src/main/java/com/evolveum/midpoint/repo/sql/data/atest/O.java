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

package com.evolveum.midpoint.repo.sql.data.atest;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class O {

    private String oid;

    @Id
    @GeneratedValue(generator = "OidGenerator")
    @GenericGenerator(name = "OidGenerator", strategy = "com.evolveum.midpoint.repo.sql.OidGenerator")
    @Column(unique = true, nullable = false, updatable = false, length = 36)
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @Transient
    public abstract Collection<IdentifiableContainer> getContainers(QName name);
}
