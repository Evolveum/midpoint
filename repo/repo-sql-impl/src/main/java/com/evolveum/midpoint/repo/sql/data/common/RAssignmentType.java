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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.Identifiable;
import com.evolveum.midpoint.repo.sql.jaxb.XAssignmentType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import org.apache.commons.lang.Validate;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@Table(name = "assignment")
public class RAssignmentType implements Identifiable {

    private static final Trace LOGGER = TraceManager.getTrace(RAssignmentType.class);
    private long id;
    private RExtension extension;
    private RObjectReferenceType targetRef;
    private String accountConstruction;
    private RActivationType activation;

    @Id
    @GeneratedValue
    @Override
    public long getId() {
        return id;
    }

    public String getAccountConstruction() {
        return accountConstruction;
    }

    @Embedded
    public RActivationType getActivation() {
        return activation;
    }

    @ManyToOne
    public RExtension getExtension() {
        return extension;
    }

    @ManyToOne
    @JoinTable(name = "assignment_target_ref", joinColumns = @JoinColumn(name = "assignment"),
            inverseJoinColumns = @JoinColumn(name = "objectRef"))
    public RObjectReferenceType getTargetRef() {
        return targetRef;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public void setAccountConstruction(String accountConstruction) {
        this.accountConstruction = accountConstruction;
    }

    public void setActivation(RActivationType activation) {
        this.activation = activation;
    }

    public void setExtension(RExtension extension) {
        this.extension = extension;
    }

    public void setTargetRef(RObjectReferenceType targetRef) {
        this.targetRef = targetRef;
    }

    public static void copyToJAXB(RAssignmentType repo, AssignmentType jaxb) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

//        if (jaxb instanceof XAssignmentType) {
//            XAssignmentType xAssignment = (XAssignmentType) jaxb;
//            xAssignment.setId(repo.getId());
//        }

        try {
            jaxb.setAccountConstruction(RUtil.toJAXB(repo.getAccountConstruction(), AccountConstructionType.class));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        RActivationType activation = repo.getActivation();
        if (activation != null) {
            jaxb.setActivation(activation.toJAXB());
        }

        RExtension extension = repo.getExtension();
        if (extension != null) {
            jaxb.setExtension(extension.toJAXB());
        }

        if (repo.getTargetRef() != null) {
            jaxb.setTargetRef(repo.getTargetRef().toJAXB());
        }
    }

    public static void copyFromJAXB(AssignmentType jaxb, RAssignmentType repo) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        try {
            repo.setAccountConstruction(RUtil.toRepo(jaxb.getAccountConstruction()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        if (jaxb.getActivation() != null) {
            RActivationType activation = new RActivationType();
            RActivationType.copyFromJAXB(jaxb.getActivation(), activation);
            repo.setActivation(activation);
        }

        if (jaxb.getExtension() != null) {
            RExtension extension = new RExtension();
            RExtension.copyFromJAXB(jaxb.getExtension(), extension);
            repo.setExtension(extension);
        }

        if (jaxb.getTarget() != null) {
            LOGGER.warn("Target from assignment type won't be saved. It should be translated to target reference.");
        }

        repo.setTargetRef(RUtil.jaxbRefToRepo(jaxb.getTargetRef()));
    }

    public AssignmentType toJAXB() throws DtoTranslationException {
        AssignmentType object = new AssignmentType();
        RAssignmentType.copyToJAXB(this, object);
        return object;
    }
}
