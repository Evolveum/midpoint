/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.id.RAnyContainerId;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
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

    public static final String OWNER_TYPE = "owner_type";

    private RObject owner;
    private String ownerOid;
    private RObjectType ownerType;

    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short clobsCount;
    private Short polysCount;

    private Set<RAnyString> strings;
    private Set<RAnyLong> longs;
    private Set<RAnyDate> dates;
    private Set<RAnyReference> references;
    private Set<RAnyClob> clobs;
    private Set<RAnyPolyString> polys;

    @ForeignKey(name = "none")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    public RObject getOwner() {
        return owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerTypeGenerator")
    @GenericGenerator(name = "ContainerTypeGenerator",
            strategy = "com.evolveum.midpoint.repo.sql.util.ContainerTypeGenerator")
    @Enumerated(EnumType.ORDINAL)
    @Column(name = OWNER_TYPE)
    public RObjectType getOwnerType() {
        return ownerType;
    }

    @OneToMany(mappedBy = RAnyClob.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAnyClob> getClobs() {
        if (clobs == null) {
            clobs = new HashSet<>();
        }
        return clobs;
    }

    @OneToMany(mappedBy = RAnyLong.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAnyLong> getLongs() {
        if (longs == null) {
            longs = new HashSet<>();
        }
        return longs;
    }

    @OneToMany(mappedBy = RAnyString.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAnyString> getStrings() {
        if (strings == null) {
            strings = new HashSet<>();
        }
        return strings;
    }

    @OneToMany(mappedBy = RAnyDate.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAnyDate> getDates() {
        if (dates == null) {
            dates = new HashSet<>();
        }
        return dates;
    }

    @OneToMany(mappedBy = RAnyReference.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAnyReference> getReferences() {
        if (references == null) {
            references = new HashSet<>();
        }
        return references;
    }

    @OneToMany(mappedBy = RAnyPolyString.ANY_CONTAINER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAnyPolyString> getPolys() {
        if (polys == null) {
            polys = new HashSet<>();
        }
        return polys;
    }

    public Short getStringsCount() {
        return stringsCount;
    }

    public Short getLongsCount() {
        return longsCount;
    }

    public Short getDatesCount() {
        return datesCount;
    }

    public Short getReferencesCount() {
        return referencesCount;
    }

    public Short getClobsCount() {
        return clobsCount;
    }

    public Short getPolysCount() {
        return polysCount;
    }

    public void setStringsCount(Short stringsCount) {
        this.stringsCount = stringsCount;
    }

    public void setLongsCount(Short longsCount) {
        this.longsCount = longsCount;
    }

    public void setDatesCount(Short datesCount) {
        this.datesCount = datesCount;
    }

    public void setReferencesCount(Short referencesCount) {
        this.referencesCount = referencesCount;
    }

    public void setClobsCount(Short clobsCount) {
        this.clobsCount = clobsCount;
    }

    public void setPolysCount(Short polysCount) {
        this.polysCount = polysCount;
    }

    public void setPolys(Set<RAnyPolyString> polys) {
        this.polys = polys;
    }

    public void setClobs(Set<RAnyClob> clobs) {
        this.clobs = clobs;
    }

    public void setReferences(Set<RAnyReference> references) {
        this.references = references;
    }

    public void setOwnerType(RObjectType ownerType) {
        this.ownerType = ownerType;
    }

    public void setDates(Set<RAnyDate> dates) {
        this.dates = dates;
    }

    public void setLongs(Set<RAnyLong> longs) {
        this.longs = longs;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setStrings(Set<RAnyString> strings) {
        this.strings = strings;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAnyContainer that = (RAnyContainer) o;

        if (clobs != null ? !clobs.equals(that.clobs) : that.clobs != null) return false;
        if (clobsCount != null ? !clobsCount.equals(that.clobsCount) : that.clobsCount != null) return false;
        if (dates != null ? !dates.equals(that.dates) : that.dates != null) return false;
        if (datesCount != null ? !datesCount.equals(that.datesCount) : that.datesCount != null) return false;
        if (longs != null ? !longs.equals(that.longs) : that.longs != null) return false;
        if (longsCount != null ? !longsCount.equals(that.longsCount) : that.longsCount != null) return false;
        if (polys != null ? !polys.equals(that.polys) : that.polys != null) return false;
        if (polysCount != null ? !polysCount.equals(that.polysCount) : that.polysCount != null) return false;
        if (references != null ? !references.equals(that.references) : that.references != null) return false;
        if (referencesCount != null ? !referencesCount.equals(that.referencesCount) : that.referencesCount != null)
            return false;
        if (strings != null ? !strings.equals(that.strings) : that.strings != null) return false;
        if (stringsCount != null ? !stringsCount.equals(that.stringsCount) : that.stringsCount != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerType != null ? ownerType.hashCode() : 0;

        return result;
    }

    public static void copyToJAXB(RAnyContainer repo, ShadowAttributesType jaxb,
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

        if (repo.getClobsCount() > 0) convertValues(converter, containerValue, repo.getClobs());
        if (repo.getDatesCount() > 0) convertValues(converter, containerValue, repo.getDates());
        if (repo.getLongsCount() > 0) convertValues(converter, containerValue, repo.getLongs());
        if (repo.getStringsCount() > 0) convertValues(converter, containerValue, repo.getStrings());
        if (repo.getReferencesCount() > 0) convertValues(converter, containerValue, repo.getReferences());
        if (repo.getPolysCount() > 0) convertValues(converter, containerValue, repo.getPolys());
    }

    private static <T extends RAnyValue> void convertValues(RAnyConverter converter, PrismContainerValue containerValue,
                                                                  Set<T> values) throws DtoTranslationException {
        if (values == null) {
            return;
        }

        for (RAnyValue value : values) {
            converter.convertFromRValue(value, containerValue);
        }
    }

    public static void copyFromJAXB(ShadowAttributesType jaxb, RAnyContainer repo,
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

        Set<RAnyValue> values = new HashSet<RAnyValue>();
        try {
            List<Item<?>> items = containerValue.getItems();
            for (Item item : items) {
                values.addAll(converter.convertToRValue(item, false));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        for (RAnyValue value : values) {
            ((RExtensionValue) value).setAnyContainer(repo);

            if (value instanceof RAnyClob) {
                repo.getClobs().add((RAnyClob) value);
            } else if (value instanceof RAnyDate) {
                repo.getDates().add((RAnyDate) value);
            } else if (value instanceof RAnyLong) {
                repo.getLongs().add((RAnyLong) value);
            } else if (value instanceof RAnyReference) {
                repo.getReferences().add((RAnyReference) value);
            } else if (value instanceof RAnyString) {
                repo.getStrings().add((RAnyString) value);
            } else if (value instanceof RAnyPolyString) {
                repo.getPolys().add((RAnyPolyString) value);
            }
        }

        repo.setClobsCount((short) repo.getClobs().size());
        repo.setStringsCount((short) repo.getStrings().size());
        repo.setDatesCount((short) repo.getDates().size());
        repo.setPolysCount((short) repo.getPolys().size());
        repo.setReferencesCount((short) repo.getReferences().size());
        repo.setLongsCount((short) repo.getLongs().size());
    }
}
