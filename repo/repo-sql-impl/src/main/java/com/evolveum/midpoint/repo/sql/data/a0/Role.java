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

package com.evolveum.midpoint.repo.sql.data.a0;

import com.evolveum.midpoint.repo.sql.data.atest.IdentifiableContainer;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Role extends O {

    private Set<Assignment> assignments;
    private String description;

    public String getDescription() {
        return description;
    }

    @OneToMany(mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<Assignment> getAssignments() {
        return assignments;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAssignments(Set<Assignment> assignments) {
        this.assignments = assignments;
    }

    @Transient
    @Override
    public Collection<Assignment> getContainers(QName name) {
        Collection<Assignment> containers = new ArrayList<Assignment>();
        if (getAssignments() != null) {
            containers.addAll(getAssignments());
        }

        return containers;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
