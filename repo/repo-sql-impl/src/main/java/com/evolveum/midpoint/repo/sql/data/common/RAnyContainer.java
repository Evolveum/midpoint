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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowAttributesType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@IdClass(RAnyContainerId.class)
@Table(name = "m_any")
public class RAnyContainer implements Serializable {

    private RContainer owner;
    private String ownerOid;
    private Long ownerId;
    private RContainerType ownerType;

    private Set<RStringValue> strings;
    private Set<RLongValue> longs;
    private Set<RDateValue> dates;
    private Set<RClobValue> clobs;

    @ForeignKey(name = "none")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "owner_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public RContainer getOwner() {
        return owner;
    }

    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerTypeGenerator")
    @GenericGenerator(name = "ContainerTypeGenerator", strategy = "com.evolveum.midpoint.repo.sql.ContainerTypeGenerator")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAnyContainer that = (RAnyContainer) o;

        if (clobs != null ? !clobs.equals(that.clobs) : that.clobs != null) return false;
        if (dates != null ? !dates.equals(that.dates) : that.dates != null) return false;
        if (longs != null ? !longs.equals(that.longs) : that.longs != null) return false;
        if (strings != null ? !strings.equals(that.strings) : that.strings != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerType != null ? ownerType.hashCode() : 0;

        return result;
    }

    public static void copyToJAXB(RAnyContainer repo, ResourceObjectShadowAttributesType jaxb,
            PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyToJAXB(repo, jaxb.asPrismContainerValue(), prismContext);
    }

    public static void copyToJAXB(RAnyContainer repo, ExtensionType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyToJAXB(repo, jaxb.asPrismContainerValue(), prismContext);
    }

    private static void copyToJAXB(RAnyContainer repo, PrismContainerValue containerValue,
            PrismContext prismContext) throws
            DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(prismContext);
        if (repo.getClobs() != null) {
            for (RClobValue value : repo.getClobs()) {
                converter.convertFromRValue(value, containerValue);
            }
        }
        if (repo.getDates() != null) {
            for (RDateValue value : repo.getDates()) {
                converter.convertFromRValue(value, containerValue);
            }
        }
        if (repo.getLongs() != null) {
            for (RLongValue value : repo.getLongs()) {
                converter.convertFromRValue(value, containerValue);
            }
        }
        if (repo.getStrings() != null) {
            for (RStringValue value : repo.getStrings()) {
                converter.convertFromRValue(value, containerValue);
            }
        }
    }

    public static void copyFromJAXB(ResourceObjectShadowAttributesType jaxb, RAnyContainer repo,
            PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyFromJAXB(jaxb.asPrismContainerValue(), repo, prismContext);
    }

    public static void copyFromJAXB(ExtensionType jaxb, RAnyContainer repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        copyFromJAXB(jaxb.asPrismContainerValue(), repo, prismContext);
    }

    private static void copyFromJAXB(PrismContainerValue containerValue, RAnyContainer repo,
            PrismContext prismContext) throws
            DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(prismContext);

        Set<RValue> values = new HashSet<RValue>();
        try {
            List<Item> items = containerValue.getItems();
            for (Item item : items) {
                values.addAll(converter.convertToRValue(item));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        for (RValue value : values) {
            if (value instanceof RClobValue) {
                if (repo.getClobs() == null) {
                    repo.setClobs(new HashSet<RClobValue>());
                }
                repo.getClobs().add((RClobValue) value);
            } else if (value instanceof RDateValue) {
                if (repo.getDates() == null) {
                    repo.setDates(new HashSet<RDateValue>());
                }
                repo.getDates().add((RDateValue) value);
            } else if (value instanceof RLongValue) {
                if (repo.getLongs() == null) {
                    repo.setLongs(new HashSet<RLongValue>());
                }
                repo.getLongs().add((RLongValue) value);
            } else if (value instanceof RStringValue) {
                if (repo.getStrings() == null) {
                    repo.setStrings(new HashSet<RStringValue>());
                }
                repo.getStrings().add((RStringValue) value);
            }
        }
    }
}
