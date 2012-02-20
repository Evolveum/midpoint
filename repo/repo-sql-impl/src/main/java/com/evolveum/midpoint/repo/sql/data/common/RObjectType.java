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
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryContainer;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@QueryContainer(namespace = SchemaConstants.NS_COMMON, name = "object")
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class RObjectType {

    @QueryAttribute(name = "name")
    private String name;
    @QueryAttribute(name = "description")
    private String description;
    @QueryAttribute(name = "oid")
    private String oid;
    @QueryAttribute(name = "version")
    private long version;

    @Type(type = "org.hibernate.type.TextType")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Index(name = "iName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(generator = "OidGenerator")
    @GenericGenerator(name = "OidGenerator", strategy = "com.evolveum.midpoint.repo.sql.OidGenerator")
    @Column(unique = true, nullable = false, updatable = false, length = 36)
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @Version()
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public static void copyToJAXB(RObjectType repo, ObjectType jaxb) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setDescription(repo.getDescription());
        jaxb.setName(repo.getName());
        jaxb.setOid(repo.getOid());
        jaxb.setVersion(repo.getVersion());
    }

    public static void copyFromJAXB(ObjectType jaxb, RObjectType repo) throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setName(jaxb.getName());
        repo.setOid(jaxb.getOid());

        repo.setVersion(jaxb.getVersion());
    }

    public abstract <T extends ObjectType> T toJAXB() throws DtoTranslationException;
}
