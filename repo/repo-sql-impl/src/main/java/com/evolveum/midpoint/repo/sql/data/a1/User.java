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

package com.evolveum.midpoint.repo.sql.data.a1;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "user")
@ForeignKey(name = "fk_user")
public class User extends O {

    private String fullName;
    private Set<Reference> accountRefs;
//    private Set<Assignment> assignments;

    @OneToMany(mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<Reference> getAccountRefs() {
        return accountRefs;
    }

//    @OneToMany(mappedBy = "owner")
//    @ForeignKey(name = "none")
//    @Cascade({org.hibernate.annotations.CascadeType.ALL})
//    public Set<Assignment> getAssignments() {
//        return assignments;
//    }

    public String getFullName() {
        return fullName;
    }

//    public void setAssignments(Set<Assignment> assignments) {
//        this.assignments = assignments;
//    }

    public void setAccountRefs(Set<Reference> accountRefs) {
        this.accountRefs = accountRefs;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
