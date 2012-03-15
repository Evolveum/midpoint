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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "any")
public class RAnyContainer implements Serializable {

    private RContainer owner;
    private String ownerOid;
    private Long ownerId;
    private RContainerType ownerType;

    private Set<RStringValue> strings;
    private Set<RLongValue> longs;
    private Set<RDateValue> dates;
    private Set<RClobValue> clobs;

    @Transient
    public RContainer getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "ownerType")
    public RContainerType getOwnerType() {
        return ownerType;
    }

    @ElementCollection
    @ForeignKey(name = "fk_any_long")
    @CollectionTable(name = "any_long", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id"), @JoinColumn(name = "ownerType")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RLongValue> getLongs() {
        return longs;
    }

    @ElementCollection
    @ForeignKey(name = "fk_any_string")
    @CollectionTable(name = "any_string", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id"), @JoinColumn(name = "ownerType")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RStringValue> getStrings() {
        return strings;
    }

    @ElementCollection
    @ForeignKey(name = "fk_any_clob")
    @CollectionTable(name = "any_clob", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id"), @JoinColumn(name = "ownerType")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RClobValue> getClobs() {
        return clobs;
    }

    @ElementCollection
    @ForeignKey(name = "fk_any_date")
    @CollectionTable(name = "any_date", joinColumns =
            {@JoinColumn(name = "owner_oid"), @JoinColumn(name = "owner_id"), @JoinColumn(name = "ownerType")})
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RDateValue> getDates() {
        return dates;
    }

    public void setOwnerType(RContainerType ownerType) {
        this.ownerType = ownerType;
    }

    public void setClobs(Set<RClobValue> clobs) {
        this.clobs = clobs;
    }

    public void setDates(Set<RDateValue> dates) {
        this.dates = dates;
    }

    public void setLongs(Set<RLongValue> longs) {
        this.longs = longs;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setStrings(Set<RStringValue> strings) {
        this.strings = strings;
    }

    public void setOwner(RContainer owner) {
        this.owner = owner;
    }

    public static void copyToJAXB(RAnyContainer repo, ExtensionType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        if (repo.getClobs() != null) {
            for (RClobValue value : repo.getClobs()) {
                //todo
            }
        }
        if (repo.getDates() != null) {
            for (RDateValue value : repo.getDates()) {
                //todo
            }
        }
        if (repo.getLongs() != null) {
            for (RLongValue value : repo.getLongs()) {
                //todo
            }
        }
        if (repo.getStrings() != null) {
            for (RStringValue value : repo.getStrings()) {
                //todo
            }
        }
    }

    public static void copyFromJAXB(ExtensionType jaxb, RAnyContainer repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        List<Object> anyList = jaxb.getAny();
        Set<RValue> values = new HashSet<RValue>(); 
        for (Object any : anyList) {
            //todo add any to values
        }
        
        for (RValue value : values) {

        }
    }

    public ExtensionType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ExtensionType extension = new ExtensionType();
        RAnyContainer.copyToJAXB(this, extension, prismContext);

        return extension;
    }
}
