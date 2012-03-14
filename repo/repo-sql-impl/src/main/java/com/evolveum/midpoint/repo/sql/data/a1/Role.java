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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
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
@Table(name = "role")
@ForeignKey(name = "fk_role")
public class Role extends RObjectType {

    private Set<RAssignment> assignments;

    @OneToMany(mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public static void copyToJAXB(Role repo, RoleType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObjectType.copyToJAXB(repo, jaxb, prismContext);

//        if (repo.getAssignment() == null) {
//            return;
//        }
//
//        for (RAssignmentType rAssignment : repo.getAssignment()) {
//            jaxb.getAssignment().add(rAssignment.toJAXB(prismContext));
//        }
    }

    public static void copyFromJAXB(RoleType jaxb, Role repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObjectType.copyFromJAXB(jaxb, repo, prismContext);

//        if (!jaxb.getAssignment().isEmpty()) {
//            repo.setAssignment(new ArrayList<RAssignmentType>());
//        }
//
//        for (AssignmentType assignment : jaxb.getAssignment()) {
//            RAssignmentType rAssignment = new RAssignmentType();
//            RAssignmentType.copyFromJAXB(assignment, rAssignment, prismContext);
//
//            repo.getAssignment().add(rAssignment);
//        }
    }

    @Override
    public RoleType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        RoleType object = new RoleType();
        Role.copyToJAXB(this, object, prismContext);
        RUtil.revive(object.asPrismObject(), RoleType.class, prismContext);
        return object;
    }
}
