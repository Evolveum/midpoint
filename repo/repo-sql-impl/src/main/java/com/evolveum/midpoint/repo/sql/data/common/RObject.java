/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtBoolean;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtDate;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtLong;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtPolyString;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtReference;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtString;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtValue;
import com.evolveum.midpoint.repo.sql.data.common.container.RTrigger;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbPath;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualAny;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.*;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
@NamedQueries({
        @NamedQuery(name = "get.focusPhoto", query = "select p.photo from RFocusPhoto p where p.ownerOid = :oid"),
        @NamedQuery(name = "get.object", query = "select o.fullObject, o.stringsCount, o.longsCount, o.datesCount, o.referencesCount, o.polysCount, o.booleansCount from RObject as o where o.oid=:oid"),
        @NamedQuery(name = "searchShadowOwner.getShadow", query = "select s.oid from RShadow as s where s.oid = :oid"),
        @NamedQuery(name = "searchShadowOwner.getOwner", query = "select o.fullObject, o.stringsCount, o.longsCount, o.datesCount, o.referencesCount, o.polysCount, o.booleansCount from RFocus as o left join o.linkRef as ref where ref.targetOid = :oid"),
        @NamedQuery(name = "listAccountShadowOwner.getUser", query = "select u.fullObject, u.stringsCount, u.longsCount, u.datesCount, u.referencesCount, u.polysCount, u.booleansCount from RUser as u left join u.linkRef as ref where ref.targetOid = :oid"),
        @NamedQuery(name = "getExtCount", query = "select stringsCount, longsCount, datesCount, referencesCount, polysCount, booleansCount from RObject where oid = :oid"),
        @NamedQuery(name = "getVersion", query = "select o.version from RObject as o where o.oid = :oid"),
        @NamedQuery(name = "existOrgClosure", query = "select count(*) from ROrgClosure as o where o.ancestorOid = :ancestorOid and o.descendantOid = :descendantOid"),
        @NamedQuery(name = "sqlDeleteOrgClosure", query = "delete from ROrgClosure as o where o.descendantOid = :oid or o.ancestorOid = :oid"),
        @NamedQuery(name = "listResourceObjectShadows", query = "select s.fullObject, s.stringsCount, s.longsCount, s.datesCount, s.referencesCount, s.polysCount, s.booleansCount from RShadow as s left join s.resourceRef as ref where ref.targetOid = :oid"),
        @NamedQuery(name = "getDefinition.ROExtDate", query = "select c.name, c.type, c.valueType from ROExtDate as c where c.ownerOid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtString", query = "select c.name, c.type, c.valueType from ROExtString as c where c.ownerOid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtPolyString", query = "select c.name, c.type, c.valueType from ROExtPolyString as c where c.ownerOid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtLong", query = "select c.name, c.type, c.valueType from ROExtLong as c where c.ownerOid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtReference", query = "select c.name, c.type, c.valueType from ROExtReference as c where c.ownerOid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtBoolean", query = "select c.name, c.type, c.valueType from ROExtBoolean as c where c.ownerOid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "isAnySubordinateAttempt.oneLowerOid", query = "select count(*) from ROrgClosure o where o.ancestorOid=:aOid and o.descendantOid=:dOid"),
        @NamedQuery(name = "isAnySubordinateAttempt.moreLowerOids", query = "select count(*) from ROrgClosure o where o.ancestorOid=:aOid and o.descendantOid in (:dOids)"),
        @NamedQuery(name = "get.lookupTableLastId", query = "select max(r.id) from RLookupTableRow r where r.ownerOid = :oid"),
        @NamedQuery(name = "delete.lookupTableData", query = "delete RLookupTableRow r where r.ownerOid = :oid"),
        @NamedQuery(name = "delete.lookupTableDataRow", query = "delete RLookupTableRow r where r.ownerOid = :oid and r.id = :id"),
        @NamedQuery(name = "delete.lookupTableDataRowByKey", query = "delete RLookupTableRow r where r.ownerOid = :oid and r.key = :key"),
        @NamedQuery(name = "get.campaignCaseLastId", query = "select max(c.id) from RAccessCertificationCase c where c.ownerOid = :oid"),
        @NamedQuery(name = "delete.campaignCases", query = "delete RAccessCertificationCase c where c.ownerOid = :oid"),
        @NamedQuery(name = "delete.campaignCasesDecisions", query = "delete RAccessCertificationDecision d where d.ownerOwnerOid = :oid"),
        @NamedQuery(name = "delete.campaignCasesReferences", query = "delete RCertCaseReference r where r.ownerOid = :oid"),
        @NamedQuery(name = "delete.campaignCase", query = "delete RAccessCertificationCase c where c.ownerOid = :oid and c.id = :id"),
        @NamedQuery(name = "delete.campaignCaseDecisions", query = "delete RAccessCertificationDecision d where d.ownerOwnerOid = :oid and d.ownerId = :id"),
        // doesn't work; generates SQL of "delete from m_acc_cert_case_reference where owner_owner_oid=? and owner_id=? and owner_owner_oid=? and reference_type=? and relation=? and targetOid=?"
        //@NamedQuery(name = "delete.campaignCaseReferences", query = "delete RCertCaseReference r where r.ownerOid = :oid and r.id = :id"),
        @NamedQuery(name = "resolveReferences", query = "select o.oid, o.name from RObject as o where o.oid in (:oid)"),
        @NamedQuery(name = "get.campaignCase", query = "select c.fullObject from RAccessCertificationCase c where c.ownerOid=:ownerOid and c.id=:id"),
        @NamedQuery(name = "get.campaignCases", query = "select c.fullObject from RAccessCertificationCase c where c.ownerOid=:ownerOid")
})
@QueryEntity(
        anyElements = {
                @VirtualAny(jaxbNameLocalPart = "extension", ownerType = RObjectExtensionType.EXTENSION)
        }
//        ,
//        entities = {
//                @VirtualEntity(
//                        jaxbName = @JaxbName(localPart = "metadata"),
//                        jaxbType = MetadataType.class,
//                        jpaName = "",
//                        jpaType = Serializable.class            // dummy value (ignored)
//                )
//        }
    )
@Entity
@Table(name = "m_object", indexes = {
        @Index(name = "iObjectNameOrig", columnList = "name_orig"),
        @Index(name = "iObjectNameNorm", columnList = "name_norm"),
        @Index(name = "iObjectTypeClass", columnList = "objectTypeClass"),
        @Index(name = "iObjectCreateTimestamp", columnList = "createTimestamp"),
        @Index(name = "iObjectLifecycleState", columnList = "lifecycleState")})
@Inheritance(strategy = InheritanceType.JOINED)
@Persister(impl = MidPointJoinedPersister.class)
public abstract class RObject<T extends ObjectType> implements Metadata<RObjectReference<RFocus>>, EntityState, Serializable {

    public static final String F_OBJECT_TYPE_CLASS = "objectTypeClass";
	public static final String F_TEXT_INFO_ITEMS = "textInfoItems";

	private Boolean trans;

    private String oid;
    private int version;
    //full XML
    private byte[] fullObject;
    //org. closure table
    private Set<ROrgClosure> descendants;
    private Set<ROrgClosure> ancestors;
    //object type
    private RObjectType objectTypeClass;
    //ObjectType searchable fields
    private RPolyString name;
    private Set<RObjectReference<ROrg>> parentOrgRef;
    private Set<RTrigger> trigger;
    private REmbeddedReference tenantRef;
    private String lifecycleState;
    //Metadata
    private XMLGregorianCalendar createTimestamp;
    private REmbeddedReference creatorRef;
    private Set<RObjectReference<RFocus>> createApproverRef;
    private String createChannel;
    private XMLGregorianCalendar modifyTimestamp;
    private REmbeddedReference modifierRef;
    private Set<RObjectReference<RFocus>> modifyApproverRef;
    private String modifyChannel;
    //extension, and other "any" like shadow/attributes
    private Short booleansCount;
    private Short stringsCount;
    private Short longsCount;
    private Short datesCount;
    private Short referencesCount;
    private Short polysCount;
    private Set<ROExtString> strings;
    private Set<ROExtLong> longs;
    private Set<ROExtDate> dates;
    private Set<ROExtReference> references;
    private Set<ROExtPolyString> polys;
    private Set<ROExtBoolean> booleans;

    private Set<RObjectTextInfo> textInfoItems;

    @Id
    @GeneratedValue(generator = "ObjectOidGenerator")
    @GenericGenerator(name = "ObjectOidGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ObjectOidGenerator")
    @Column(name = "oid", nullable = false, updatable = false, length = RUtil.COLUMN_LENGTH_OID)
    @IdQueryProperty
    public String getOid() {
        return oid;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    //    @JoinTable(foreignKey = @ForeignKey(name = "none"))
    @OneToMany(mappedBy = RTrigger.F_OWNER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RTrigger> getTrigger() {
        if (trigger == null) {
            trigger = new HashSet<>();
        }
        return trigger;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 0")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectReference<ROrg>> getParentOrgRef() {
        if (parentOrgRef == null) {
            parentOrgRef = new HashSet<>();
        }
        return parentOrgRef;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class, mappedBy = "descendant")
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    public Set<ROrgClosure> getDescendants() {
        return descendants;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class, mappedBy = "ancestor")//, orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    public Set<ROrgClosure> getAncestors() {
        return ancestors;
    }

    @NotQueryable
    public int getVersion() {
        return version;
    }

    @Embedded
    public REmbeddedReference getTenantRef() {
        return tenantRef;
    }

    @Lob
    @NotQueryable
    public byte[] getFullObject() {
        return fullObject;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 5")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
//    @JoinTable(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createApproverRef") })
    public Set<RObjectReference<RFocus>> getCreateApproverRef() {
        if (createApproverRef == null) {
            createApproverRef = new HashSet<>();
        }
        return createApproverRef;
    }

    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createChannel") })
    public String getCreateChannel() {
        return createChannel;
    }

    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createTimestamp") })
    public XMLGregorianCalendar getCreateTimestamp() {
        return createTimestamp;
    }

    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "creatorRef") })
    public REmbeddedReference getCreatorRef() {
        return creatorRef;
    }

    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifierRef") })
    public REmbeddedReference getModifierRef() {
        return modifierRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 6")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true)
//    @JoinTable(foreignKey = @ForeignKey(name = "none"))
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyApproverRef") })
    public Set<RObjectReference<RFocus>> getModifyApproverRef() {
        if (modifyApproverRef == null) {
            modifyApproverRef = new HashSet<>();
        }
        return modifyApproverRef;
    }

    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyChannel") })
    public String getModifyChannel() {
        return modifyChannel;
    }

    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyTimestamp") })
    public XMLGregorianCalendar getModifyTimestamp() {
        return modifyTimestamp;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ROExtLong> getLongs() {
        if (longs == null) {
            longs = new HashSet<>();
        }
        return longs;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ROExtBoolean> getBooleans() {
        if (booleans == null) {
            booleans = new HashSet<>();
        }
        return booleans;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ROExtString> getStrings() {
        if (strings == null) {
            strings = new HashSet<>();
        }
        return strings;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ROExtDate> getDates() {
        if (dates == null) {
            dates = new HashSet<>();
        }
        return dates;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ROExtReference> getReferences() {
        if (references == null) {
            references = new HashSet<>();
        }
        return references;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ROExtPolyString> getPolys() {
        if (polys == null) {
            polys = new HashSet<>();
        }
        return polys;
    }

    @NotQueryable
    public Short getStringsCount() {
        if (stringsCount == null) {
            stringsCount = 0;
        }
        return stringsCount;
    }

    @NotQueryable
    public Short getBooleansCount() {
        if (booleansCount == null) {
            booleansCount = 0;
        }
        return booleansCount;
    }

    @NotQueryable
    public Short getLongsCount() {
        if (longsCount == null) {
            longsCount = 0;
        }
        return longsCount;
    }

    @NotQueryable
    public Short getDatesCount() {
        if (datesCount == null) {
            datesCount = 0;
        }
        return datesCount;
    }

    @NotQueryable
    public Short getReferencesCount() {
        if (referencesCount == null) {
            referencesCount = 0;
        }
        return referencesCount;
    }

    @NotQueryable
    public Short getPolysCount() {
        if (polysCount == null) {
            polysCount = 0;
        }
        return polysCount;
    }

    @Enumerated
    @NotQueryable
    public RObjectType getObjectTypeClass() {
        return objectTypeClass;
    }

    @Transient
    public Boolean isTransient() {
        return trans;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public void setObjectTypeClass(RObjectType objectTypeClass) {
        this.objectTypeClass = objectTypeClass;
    }

    public void setCreateApproverRef(Set<RObjectReference<RFocus>> createApproverRef) {
        this.createApproverRef = createApproverRef;
    }

    public void setCreateChannel(String createChannel) {
        this.createChannel = createChannel;
    }

    public void setCreateTimestamp(XMLGregorianCalendar createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public void setCreatorRef(REmbeddedReference creatorRef) {
        this.creatorRef = creatorRef;
    }

    public void setModifierRef(REmbeddedReference modifierRef) {
        this.modifierRef = modifierRef;
    }

    public void setModifyApproverRef(Set<RObjectReference<RFocus>> modifyApproverRef) {
        this.modifyApproverRef = modifyApproverRef;
    }

    public void setModifyChannel(String modifyChannel) {
        this.modifyChannel = modifyChannel;
    }

    public void setModifyTimestamp(XMLGregorianCalendar modifyTimestamp) {
        this.modifyTimestamp = modifyTimestamp;
    }

    public void setFullObject(byte[] fullObject) {
        this.fullObject = fullObject;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setTenantRef(REmbeddedReference tenantRef) {
        this.tenantRef = tenantRef;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public void setTrigger(Set<RTrigger> trigger) {
        this.trigger = trigger;
    }

    public void setDescendants(Set<ROrgClosure> descendants) {
        this.descendants = descendants;
    }

    public void setAncestors(Set<ROrgClosure> ancestors) {
        this.ancestors = ancestors;
    }

    public void setParentOrgRef(Set<RObjectReference<ROrg>> parentOrgRef) {
        this.parentOrgRef = parentOrgRef;
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

    public void setPolysCount(Short polysCount) {
        this.polysCount = polysCount;
    }

    public void setPolys(Set<ROExtPolyString> polys) {
        this.polys = polys;
    }

    public void setReferences(Set<ROExtReference> references) {
        this.references = references;
    }

    public void setDates(Set<ROExtDate> dates) {
        this.dates = dates;
    }

    public void setLongs(Set<ROExtLong> longs) {
        this.longs = longs;
    }

    public void setStrings(Set<ROExtString> strings) {
        this.strings = strings;
    }

    public void setBooleansCount(Short booleansCount) {
        this.booleansCount = booleansCount;
    }

    public void setBooleans(Set<ROExtBoolean> booleans) {
        this.booleans = booleans;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RObjectTextInfo> getTextInfoItems() {
        if (textInfoItems == null) {
            textInfoItems = new HashSet<>();
        }
        return textInfoItems;
    }

    public void setTextInfoItems(Set<RObjectTextInfo> textInfoItems) {
        this.textInfoItems = textInfoItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RObject rObject = (RObject) o;

        if (name != null ? !name.equals(rObject.name) : rObject.name != null)
            return false;
        if (descendants != null ? !descendants.equals(rObject.descendants) : rObject.descendants != null)
            return false;
        if (ancestors != null ? !ancestors.equals(rObject.ancestors) : rObject.ancestors != null)
            return false;
        if (parentOrgRef != null ? !parentOrgRef.equals(rObject.parentOrgRef) : rObject.parentOrgRef != null)
            return false;
        if (trigger != null ? !trigger.equals(rObject.trigger) : rObject.trigger != null)
            return false;
        if (tenantRef != null ? !tenantRef.equals(rObject.tenantRef) : rObject.tenantRef != null)
            return false;
        if (lifecycleState != null ? !lifecycleState.equals(rObject.lifecycleState) : rObject.lifecycleState != null)
            return false;
        if (!MetadataFactory.equals(this, rObject)) return false;

        if (dates != null ? !dates.equals(rObject.dates) : rObject.dates != null) return false;
        if (datesCount != null ? !datesCount.equals(rObject.datesCount) : rObject.datesCount != null) return false;
        if (longs != null ? !longs.equals(rObject.longs) : rObject.longs != null) return false;
        if (longsCount != null ? !longsCount.equals(rObject.longsCount) : rObject.longsCount != null) return false;
        if (polys != null ? !polys.equals(rObject.polys) : rObject.polys != null) return false;
        if (polysCount != null ? !polysCount.equals(rObject.polysCount) : rObject.polysCount != null) return false;
        if (references != null ? !references.equals(rObject.references) : rObject.references != null) return false;
        if (referencesCount != null ? !referencesCount.equals(rObject.referencesCount) : rObject.referencesCount != null)
            return false;
        if (strings != null ? !strings.equals(rObject.strings) : rObject.strings != null) return false;
        if (stringsCount != null ? !stringsCount.equals(rObject.stringsCount) : rObject.stringsCount != null)
            return false;
        if (booleans != null ? !booleans.equals(rObject.booleans) : rObject.booleans != null) return false;
        if (booleansCount != null ? !booleansCount.equals(rObject.booleansCount) : rObject.booleansCount != null)
            return false;
        if (textInfoItems != null ? !textInfoItems.equals(rObject.textInfoItems) : rObject.textInfoItems != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);

        result = 31 * result + (createTimestamp != null ? createTimestamp.hashCode() : 0);
        result = 31 * result + (creatorRef != null ? creatorRef.hashCode() : 0);
        result = 31 * result + (createChannel != null ? createChannel.hashCode() : 0);
        result = 31 * result + (modifyTimestamp != null ? modifyTimestamp.hashCode() : 0);
        result = 31 * result + (modifierRef != null ? modifierRef.hashCode() : 0);
        result = 31 * result + (modifyChannel != null ? modifyChannel.hashCode() : 0);
        result = 31 * result + (lifecycleState != null ? lifecycleState.hashCode() : 0);

        return result;
    }

    @Deprecated
    protected static <T extends ObjectType> void copyToJAXB(RObject<T> repo, ObjectType jaxb, PrismContext prismContext,
                                                            Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setOid(repo.getOid());
        jaxb.setVersion(Integer.toString(repo.getVersion()));
        jaxb.setLifecycleState(repo.getLifecycleState());

        if (SelectorOptions.hasToLoadPath(ObjectType.F_PARENT_ORG_REF, options)) {
            List orgRefs = RUtil.safeSetReferencesToList(repo.getParentOrgRef(), prismContext);
            if (!orgRefs.isEmpty()) {
                jaxb.getParentOrgRef().addAll(orgRefs);
            }
        }
    }

    public static <T extends ObjectType> void copyFromJAXB(ObjectType jaxb, RObject<T> repo, PrismContext prismContext,
                                                           IdGeneratorResult generatorResult)
            throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setTransient(generatorResult.isGeneratedOid());
        repo.setOid(jaxb.getOid());

        repo.setObjectTypeClass(RObjectType.getType(ClassMapper.getHQLTypeClass(jaxb.getClass())));
        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setLifecycleState(jaxb.getLifecycleState());

        String strVersion = jaxb.getVersion();
        int version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*") ? Integer.parseInt(jaxb
                .getVersion()) : 0;
        repo.setVersion(version);

        repo.getParentOrgRef().addAll(RUtil.safeListReferenceToSet(jaxb.getParentOrgRef(), prismContext,
                repo, RReferenceOwner.OBJECT_PARENT_ORG));

        for (TriggerType trigger : jaxb.getTrigger()) {
            RTrigger rTrigger = new RTrigger(null);
            RTrigger.copyFromJAXB(trigger, rTrigger, jaxb, prismContext, generatorResult);

            repo.getTrigger().add(rTrigger);
        }

        MetadataFactory.fromJAXB(jaxb.getMetadata(), repo, prismContext);
        repo.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTenantRef(), prismContext));

        if (jaxb.getExtension() != null) {
            copyFromJAXB(jaxb.getExtension().asPrismContainerValue(), repo, prismContext, RObjectExtensionType.EXTENSION);
        }

        repo.getTextInfoItems().addAll(RObjectTextInfo.createItemsSet(jaxb, repo, prismContext));
    }

    @Deprecated
    public abstract T toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException;

    @Override
    public String toString() {
        return RUtil.getDebugString(this);
    }

    public static void copyFromJAXB(PrismContainerValue containerValue, RObject repo, PrismContext prismContext,
                                    RObjectExtensionType ownerType) throws DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(prismContext);

        Set<RAnyValue> values = new HashSet<RAnyValue>();
        try {
            List<Item<?,?>> items = containerValue.getItems();
            //TODO: is this ehought??should we try items without definitions??
            if (items != null) {
                for (Item item : items) {
                    values.addAll(converter.convertToRValue(item, false));
                }
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        for (RAnyValue value : values) {
            ROExtValue ex = (ROExtValue) value;
            ex.setOwner(repo);
            ex.setOwnerType(ownerType);

            if (value instanceof ROExtDate) {
                repo.getDates().add(value);
            } else if (value instanceof ROExtLong) {
                repo.getLongs().add(value);
            } else if (value instanceof ROExtReference) {
                repo.getReferences().add(value);
            } else if (value instanceof ROExtString) {
                repo.getStrings().add(value);
            } else if (value instanceof ROExtPolyString) {
                repo.getPolys().add(value);
            } else if (value instanceof ROExtBoolean) {
                repo.getBooleans().add(value);
            }
        }

        repo.setStringsCount((short) repo.getStrings().size());
        repo.setDatesCount((short) repo.getDates().size());
        repo.setPolysCount((short) repo.getPolys().size());
        repo.setReferencesCount((short) repo.getReferences().size());
        repo.setLongsCount((short) repo.getLongs().size());
        repo.setBooleansCount((short) repo.getBooleans().size());
    }
}
