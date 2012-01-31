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
import com.evolveum.midpoint.repo.sql.jaxb.XExtension;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.type.TextType;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "extension")
public class RExtension implements Identifiable {

    private long id;
    private Set<String> objects;   //todo mapping

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

    @ElementCollection(targetClass = TextType.class)
    @CollectionTable(name = "extension_object", joinColumns =
            {@JoinColumn(name = "extensionId")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<String> getObjects() {
        return objects;
    }

    public void setObjects(Set<String> objects) {
        this.objects = objects;
    }

    public static void copyToJAXB(RExtension repo, Extension jaxb) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (jaxb instanceof XExtension) {
            XExtension ext = (XExtension) jaxb;
            ext.setId(repo.getId());
        }

        try {

        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(Extension jaxb, RExtension repo) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (jaxb instanceof XExtension) {
            XExtension ext = (XExtension) jaxb;
            repo.setId(ext.getId());
        }

        try {

        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public Extension toJAXB() throws DtoTranslationException {
        XExtension extension = new XExtension();
        RExtension.copyToJAXB(this, extension);

        return extension;
    }
}
