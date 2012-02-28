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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
@Entity
@Table(name = "role")
public class RRoleType extends RExtensibleObjectType {

    private List<RAssignmentType> assignment;

    @OneToMany
    @JoinTable(name = "role_assignment", joinColumns = @JoinColumn(name = "roleOid"),
            inverseJoinColumns = @JoinColumn(name = "assignmentId"))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public List<RAssignmentType> getAssignment() {
        return assignment;
    }

    public void setAssignment(List<RAssignmentType> assignment) {
        this.assignment = assignment;
    }

    public static void copyToJAXB(RRoleType repo, RoleType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb, prismContext);

        if (repo.getAssignment() == null) {
            return;
        }

        for (RAssignmentType rAssignment : repo.getAssignment()) {
            jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
        }
    }

    public static void copyFromJAXB(RoleType jaxb, RRoleType repo, PrismContext prismContext) throws
            DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo, prismContext);

        if (!jaxb.getAssignment().isEmpty()) {
            repo.setAssignment(new ArrayList<RAssignmentType>());
        }

        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignmentType rAssignment = new RAssignmentType();
            RAssignmentType.copyFromJAXB(assignment, rAssignment, prismContext);

            repo.getAssignment().add(rAssignment);
        }
    }

    @Override
    public RoleType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        RoleType object = new RoleType();
        RRoleType.copyToJAXB(this, object, prismContext);
        RUtil.revive(object.asPrismObject(), RoleType.class, prismContext);
        return object;
    }
}
