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
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryContainer;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
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
    private RExtension extension;

    @ManyToOne(optional = true)
//    @JoinTable(name = "extensible_object_extension", joinColumns = @JoinColumn(name = "extensibleOid"),
//            inverseJoinColumns = @JoinColumn(name = "extensionId"))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RExtension getExtension() {
        return extension;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getDescription() {
        return description;
    }

    @Version()
    public long getVersion() {
        return version;
    }

    @Id
    @GeneratedValue(generator = "OidGenerator")
    @GenericGenerator(name = "OidGenerator", strategy = "com.evolveum.midpoint.repo.sql.OidGenerator")
    @Column(unique = true, nullable = false, updatable = false, length = 36)
    public String getOid() {
        return oid;
    }

    @Index(name = "iName")
    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public void setExtension(RExtension extension) {
        this.extension = extension;
    }

    public void setVersion(long version) {
        this.version = version;
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

            RExtension.copyToJAXB(repo.getExtension(), extension, prismContext);
        }
    }

    public static void copyFromJAXB(ObjectType jaxb, RObjectType repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDescription(jaxb.getDescription());
        repo.setName(jaxb.getName());
        repo.setOid(jaxb.getOid());

        String strVersion = jaxb.getVersion();
        long version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*")
                ? Long.parseLong(jaxb.getVersion()) : 0;
        repo.setVersion(version);

        if (jaxb.getExtension() != null) {
            RExtension extension = new RExtension();
            repo.setExtension(extension);

            RExtension.copyFromJAXB(jaxb.getExtension(), extension, prismContext);
        }
    }

    public abstract <T extends ObjectType> T toJAXB(PrismContext prismContext) throws DtoTranslationException;
}
