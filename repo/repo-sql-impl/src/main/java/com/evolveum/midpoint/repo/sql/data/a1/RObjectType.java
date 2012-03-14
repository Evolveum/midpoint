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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "object")
@ForeignKey(name = "fk_container")
public abstract class RObjectType extends Container {

    private String name;
    private String description;
    private RAnyContainer extension;

    @ManyToOne(optional = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JoinColumns({
            @JoinColumn(name = "extOid", referencedColumnName = "owner_oid"),
            @JoinColumn(name = "extId", referencedColumnName = "owner_id"),
            @JoinColumn(name = "extType", referencedColumnName = "ownerType")
    })
    public RAnyContainer getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExtension(RAnyContainer extension) {
        this.extension = extension;
        if (extension != null) {
            extension.setOwnerType(RContainerType.OBJECT);
        }
    }

    public static void copyToJAXB(RObjectType repo, ObjectType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setName(repo.getName());
        jaxb.setOid(repo.getOid());
        jaxb.setVersion(Long.toString(repo.getVersion()));

        if (repo.getExtension() != null) {
            ExtensionType extension = new ExtensionType();
            jaxb.setExtension(extension);

            RAnyContainer.copyToJAXB(repo.getExtension(), extension, prismContext);
        }
    }

    public static void copyFromJAXB(ObjectType jaxb, RObjectType repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setName(jaxb.getName());
        repo.setOid(jaxb.getOid());
        repo.setId(0L); //objects types have default id

        String strVersion = jaxb.getVersion();
        long version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*")
                ? Long.parseLong(jaxb.getVersion()) : 0;
        repo.setVersion(version);

        if (jaxb.getExtension() != null) {
            RAnyContainer extension = new RAnyContainer();
            repo.setExtension(extension);

            RAnyContainer.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
        }
    }

    public abstract <T extends ObjectType> T toJAXB(PrismContext prismContext) throws DtoTranslationException;
}
