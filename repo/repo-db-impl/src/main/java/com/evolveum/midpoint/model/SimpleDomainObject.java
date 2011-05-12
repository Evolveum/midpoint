/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;

/**
 * Base entity is an abstract class that contains a {@link #modificationDate} field.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@Entity
@Table(catalog = SimpleDomainObject.DDL_CATALOG, name = SimpleDomainObject.DDL_TABLE_DOMAINOBJECT)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", length = 45)
public abstract class SimpleDomainObject extends IdentifiableBase {

    public static final String code_id = "$Id$";
    public static final String DDL_CATALOG = "midPoint";
    public static final String DDL_TABLE_DOMAINOBJECT = "Objects";
    /**
     *
     */
    private static final long serialVersionUID = 3407043276308745632L;
    protected String name;
    private Integer version;
    private Boolean unsaved = Boolean.TRUE;
    private List<Property> properties = new ArrayList<Property>(0);
    /**
     * The creation date of this entity.
     */
    private Date modificationDate = new Date();
    /**
     * The domain.
     */
    private Domain domain;

    @Transient
    public Boolean isTransient() {
        return unsaved;
    }

    void setTransient(Boolean unsaved) {
        this.unsaved = unsaved;
    }

    //public abstract String getName();
    public void setName(String name) {
        this.name = name;
    }

    @Version
    @Column(name = "version")
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * @return the modificationDate
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "repomod", nullable = false, length = 19)
    public Date getModificationDate() {
        return cloneDate(modificationDate);
    }

    /**
     * @param modificationDate
     *            the modificationDate to set
     */
    public void setModificationDate(final Date modificationDate) {
        this.modificationDate = cloneDate(modificationDate);
    }

    /**
     * Get the domain.
     *
     * @return the domain
    //     */
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "domain_uuid", nullable = true)
    @Transient
    public Domain getDomain() {
        return domain;
    }

    /**
     * Set the domain.
     *
     * @param domain
     *            the domain to set
     */
    public void setDomain(final Domain domain) {
        this.domain = domain;
    }

    @ManyToAny(fetch = FetchType.EAGER, metaDef = "Property", metaColumn = @Column(name = "property_type"))
    @AnyMetaDef(name = "Property", metaType = "string", idType = "com.evolveum.midpoint.hibernate.usertype.UUIDType",
    metaValues = {
        @MetaValue(value = "B", targetEntity = BooleanProperty.class),
        @MetaValue(value = "I", targetEntity = IntegerProperty.class),
        @MetaValue(value = "S", targetEntity = StringProperty.class),
        @MetaValue(value = "D", targetEntity = DateProperty.class)
    })
    @Cascade(value={org.hibernate.annotations.CascadeType.ALL})
    @JoinTable(name = "Objects_Properties",
    joinColumns = @JoinColumn(name = "object_uuid"),
    inverseJoinColumns = @JoinColumn(name = "property_uuid"))
    @IndexColumn(name = "property_index")
    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    /**
     * Clone date null safely.
     *
     * @param date
     *            the date to clone
     * @return null if the <code>date</code> is null, otherwise the cloned date.
     */
    protected static final Date cloneDate(final Date date) {
        if (date == null) {
            return null;
        } else {
            return (Date) date.clone();
        }
    }

    @PrePersist
    //@PreUpdate
    public void initialise() {
        setModificationDate(new Date());
    }

    
}
