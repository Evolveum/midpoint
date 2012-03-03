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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.hibernate.annotations.Cascade;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Role extends O {

    private List<Assignment> assignment;
    private String description;

    public String getDescription() {
        return description;
    }

    @OneToMany(mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public List<Assignment> getAssignment() {
        return assignment;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAssignment(List<Assignment> assignment) {
        this.assignment = assignment;
    }

    @Transient
    @Override
    public Collection<IdentifiableContainer> getContainers(QName name) {
        Collection<IdentifiableContainer> containers = new ArrayList<IdentifiableContainer>();
        if (getAssignment() != null) {
            containers.addAll(getAssignment());
        }

        return containers;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
