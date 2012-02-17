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
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "extension")
public class RExtension implements Identifiable {

    private long id;
    private Set<RValue> objects;

    @Id
    @GeneratedValue
    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @OneToMany
    @JoinTable(name = "extension_object", joinColumns = @JoinColumn(name = "extensionId"),
            inverseJoinColumns = @JoinColumn(name = "rvalueId"))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RValue> getObjects() {
        return objects;
    }

    public void setObjects(Set<RValue> objects) {
        this.objects = objects;
    }

    public static void copyToJAXB(RExtension repo, Extension jaxb) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

//        jaxb.setId(repo.getId());

        if (repo.getObjects() == null) {
            return;
        }

        try {
            for (RValue value : repo.getObjects()) {
                jaxb.getAny().add(value.toObject());
            }
        } catch (DtoTranslationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(Extension jaxb, RExtension repo) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

//        repo.setId(jaxb.getId());

        try {
            if (!jaxb.getAny().isEmpty()) {
                repo.setObjects(new HashSet<RValue>());

                for (Object object : jaxb.getAny()) {
                    repo.getObjects().add(RUtil.createRValue(object));
                }
            }
        } catch (DtoTranslationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public Extension toJAXB() throws DtoTranslationException {
        Extension extension = new Extension();
        RExtension.copyToJAXB(this, extension);

        return extension;
    }
}
