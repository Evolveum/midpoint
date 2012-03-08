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

package com.evolveum.midpoint.repo.sql.data.a;

import org.hibernate.annotations.Cascade;

import javax.persistence.Entity;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/8/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class R extends O {

    private Set<A> aset;

    @OneToMany(mappedBy = "r")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<A> getAset() {
        return aset;
    }

    public void setAset(Set<A> aset) {
        this.aset = aset;
    }
}
