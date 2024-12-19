/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import static jakarta.persistence.CascadeType.ALL;

import java.io.Serializable;
import java.util.Objects;
import java.util.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.ROperationExecution;
import com.evolveum.midpoint.repo.sql.data.common.container.RTrigger;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@NamedQueries({
        @NamedQuery(name = "get.focusPhoto", query = "select p.photo from RFocusPhoto p where p.ownerOid = :oid"),
        @NamedQuery(name = "get.taskResult", query = "select t.fullResult from RTask t where t.oid = :oid"),
        @NamedQuery(name = "get.taskStatus", query = "select t.status from RTask t where t.oid = :oid"),
        @NamedQuery(name = "get.object", query = "select o.oid, o.fullObject, 0, 0, 0, 0, 0, 0 from RObject as o where o.oid=:oid"),
        @NamedQuery(name = "searchShadowOwner.getShadow", query = "select s.oid from RShadow as s where s.oid = :oid"),
        @NamedQuery(name = "searchShadowOwner.getOwner", query = "select o.oid, o.fullObject, 0, 0, 0, 0, 0, 0 from RFocus as o left join o.linkRef as ref where ref.targetOid = :oid"),
        @NamedQuery(name = "listAccountShadowOwner.getUser", query = "select u.oid, u.fullObject, 0, 0, 0, 0, 0, 0 from RUser as u left join u.linkRef as ref where ref.targetOid = :oid"),
        @NamedQuery(name = "getVersion", query = "select o.version from RObject as o where o.oid = :oid"),
        @NamedQuery(name = "existOrgClosure", query = "select count(*) from ROrgClosure as o where o.ancestorOid = :ancestorOid and o.descendantOid = :descendantOid"),
        @NamedQuery(name = "sqlDeleteOrgClosure", query = "delete from ROrgClosure as o where o.descendantOid = :oid or o.ancestorOid = :oid"),
        @NamedQuery(name = "listResourceObjectShadows", query = "select s.oid, s.fullObject, 0, 0, 0, 0, 0, 0 from RShadow as s left join s.resourceRef as ref where ref.targetOid = :oid"),
        @NamedQuery(name = "getDefinition.ROExtDate", query = "select c.itemId from ROExtDate as c where c.owner.oid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtString", query = "select c.itemId from ROExtString as c where c.owner.oid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtPolyString", query = "select c.itemId from ROExtPolyString as c where c.owner.oid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtLong", query = "select c.itemId from ROExtLong as c where c.owner.oid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtReference", query = "select c.itemId from ROExtReference as c where c.owner.oid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "getDefinition.ROExtBoolean", query = "select c.itemId from ROExtBoolean as c where c.owner.oid = :oid and c.ownerType = :ownerType"),
        @NamedQuery(name = "isAnySubordinateAttempt.oneLowerOid", query = "select count(*) from ROrgClosure o where o.ancestorOid=:aOid and o.descendantOid=:dOid"),
        @NamedQuery(name = "isAnySubordinateAttempt.moreLowerOids", query = "select count(*) from ROrgClosure o where o.ancestorOid=:aOid and o.descendantOid in (:dOids)"),
        @NamedQuery(name = "get.lookupTableLastId", query = "select max(r.id) from RLookupTableRow r where r.ownerOid = :oid"),
        @NamedQuery(name = "delete.lookupTableData", query = "delete RLookupTableRow r where r.ownerOid = :oid"),
        @NamedQuery(name = "delete.lookupTableDataRow", query = "delete RLookupTableRow r where r.ownerOid = :oid and r.id = :id"),
        @NamedQuery(name = "delete.lookupTableDataRowByKey", query = "delete RLookupTableRow r where r.ownerOid = :oid and r.key = :key"),
        @NamedQuery(name = "get.campaignCaseLastId", query = "select max(c.id) from RAccessCertificationCase c where c.ownerOid = :oid"),
        @NamedQuery(name = "delete.campaignCases", query = "delete RAccessCertificationCase c where c.ownerOid = :oid"),
        @NamedQuery(name = "delete.campaignCasesWorkItems", query = "delete RAccessCertificationWorkItem r where r.ownerOwnerOid = :oid"),
        @NamedQuery(name = "delete.campaignCasesWorkItemReferences", query = "delete RCertWorkItemReference r where r.ownerOwnerOwnerOid = :oid"),
        @NamedQuery(name = "delete.campaignCase", query = "delete RAccessCertificationCase c where c.ownerOid = :oid and c.id = :id"),
        @NamedQuery(name = "resolveReferences", query = "select o.oid, o.name from RObject as o where o.oid in (:oid)"),
        @NamedQuery(name = "get.campaignCase", query = "select c.fullObject from RAccessCertificationCase c where c.ownerOid=:ownerOid and c.id=:id"),
        @NamedQuery(name = "get.campaignCases", query = "select c.fullObject from RAccessCertificationCase c where c.ownerOid=:ownerOid")
})
@QueryEntity(
        anyElements = {
                @VirtualAny(jaxbNameLocalPart = "extension", ownerType = RObjectExtensionType.EXTENSION)
        },
        collections = {
                @VirtualCollection(jaxbName = @JaxbName(localPart = "assignment"), jaxbType = Set.class,
                        jpaName = "assignments", jpaType = Set.class, additionalParams = {
                        @VirtualQueryParam(name = "assignmentOwner", type = RAssignmentOwner.class,
                                value = "FOCUS") }, collectionType = RAssignment.class) })
@Entity
@Table(name = "m_object", indexes = {
        @Index(name = "iObjectNameOrig", columnList = "name_orig"),
        @Index(name = "iObjectNameNorm", columnList = "name_norm"),
        @Index(name = "iObjectTypeClass", columnList = "objectTypeClass"),
        @Index(name = "iObjectCreateTimestamp", columnList = "createTimestamp"),
        @Index(name = "iObjectLifecycleState", columnList = "lifecycleState") })
@Inheritance(strategy = InheritanceType.JOINED)
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public abstract class RObject implements Metadata<RObjectReference<RFocus>>, EntityState, Serializable {

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
    private Set<String> subtype;
    private Set<RObjectReference<ROrg>> parentOrgRef;
    private Set<RTrigger> trigger;
    private RSimpleEmbeddedReference tenantRef;
    private String lifecycleState;
    //Metadata
    private XMLGregorianCalendar createTimestamp;
    private RSimpleEmbeddedReference creatorRef;
    private Set<RObjectReference<RFocus>> createApproverRef;
    private String createChannel;
    private XMLGregorianCalendar modifyTimestamp;
    private RSimpleEmbeddedReference modifierRef;
    private Set<RObjectReference<RFocus>> modifyApproverRef;
    private String modifyChannel;
    //extension, and other "any" like shadow/attributes
    private Collection<ROExtString> strings = new ArrayList<>();
    private Collection<ROExtLong> longs = new ArrayList<>();
    private Collection<ROExtDate> dates = new ArrayList<>();
    private Collection<ROExtReference> references = new ArrayList<>();
    private Collection<ROExtPolyString> polys = new ArrayList<>();
    private Collection<ROExtBoolean> booleans = new ArrayList<>();

    private Set<RObjectTextInfo> textInfoItems;

    private Set<ROperationExecution> operationExecutions;

    // AssignmentHolderType information
    private Set<RObjectReference<RAbstractRole>> roleMembershipRef; // AssignmentHolderType
    private Set<RObjectReference<RFocus>> delegatedRef; // AssignmentHolderType
    private Set<RObjectReference<RArchetype>> archetypeRef; // AssignmentHolderType
    private Set<RAssignment> assignments; // AssignmentHolderType

    private Set<String> policySituation;

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

    @OneToMany(mappedBy = RTrigger.F_OWNER, orphanRemoval = true, cascade = ALL)
    public Set<RTrigger> getTrigger() {
        if (trigger == null) {
            trigger = new HashSet<>();
        }
        return trigger;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 0")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true, cascade = ALL)
    public Set<RObjectReference<ROrg>> getParentOrgRef() {
        if (parentOrgRef == null) {
            parentOrgRef = new HashSet<>();
        }
        return parentOrgRef;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class,
            mappedBy = "descendant", cascade = CascadeType.REMOVE)
    public Set<ROrgClosure> getDescendants() {
        return descendants;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, targetEntity = ROrgClosure.class,
            mappedBy = "ancestor", cascade = CascadeType.REMOVE)
    public Set<ROrgClosure> getAncestors() {
        return ancestors;
    }

    @NotQueryable
    public int getVersion() {
        return version;
    }

    @Embedded
    public RSimpleEmbeddedReference getTenantRef() {
        return tenantRef;
    }

    @Lob
    @NotQueryable
    public byte[] getFullObject() {
        return fullObject;
    }

    @Override
    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 5")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true, cascade = ALL)
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createApproverRef") })
    public Set<RObjectReference<RFocus>> getCreateApproverRef() {
        if (createApproverRef == null) {
            createApproverRef = new HashSet<>();
        }
        return createApproverRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 8")
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Set<RObjectReference<RAbstractRole>> getRoleMembershipRef() {
        if (roleMembershipRef == null) {
            roleMembershipRef = new HashSet<>();
        }
        return roleMembershipRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 9")
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Set<RObjectReference<RFocus>> getDelegatedRef() {
        if (delegatedRef == null) {
            delegatedRef = new HashSet<>();
        }
        return delegatedRef;
    }

    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 11")
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Set<RObjectReference<RArchetype>> getArchetypeRef() {
        if (archetypeRef == null) {
            archetypeRef = new HashSet<>();
        }
        return archetypeRef;
    }

    @Transient
    protected Set<RAssignment> getAssignments(RAssignmentOwner owner) {
        Set<RAssignment> assignments = getAssignments();
        Set<RAssignment> wanted = new HashSet<>();
        if (assignments == null) {
            return wanted;
        }

        for (RAssignment ass : assignments) {
            if (owner.equals(ass.getAssignmentOwner())) {
                wanted.add(ass);
            }
        }

        return wanted;
    }

    @Transient
    public Set<RAssignment> getAssignment() {
        return getAssignments(RAssignmentOwner.FOCUS);
    }

    @JaxbPath(itemPath = @JaxbName(localPart = "assignment"))
    @JaxbPath(itemPath = @JaxbName(localPart = "inducement"))
    @OneToMany(mappedBy = RAssignment.F_OWNER, orphanRemoval = true, cascade = ALL)
    @NotQueryable // virtual definition is used instead
    public Set<RAssignment> getAssignments() {
        if (assignments == null) {
            assignments = new HashSet<>();
        }
        return assignments;
    }

    @Override
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createChannel") })
    public String getCreateChannel() {
        return createChannel;
    }

    @Override
    @Type(XMLGregorianCalendarType.class)
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "createTimestamp") })
    public XMLGregorianCalendar getCreateTimestamp() {
        return createTimestamp;
    }

    @Override
    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "creatorRef") })
    public RSimpleEmbeddedReference getCreatorRef() {
        return creatorRef;
    }

    @Override
    @Embedded
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifierRef") })
    public RSimpleEmbeddedReference getModifierRef() {
        return modifierRef;
    }

    @Override
    @Where(clause = RObjectReference.REFERENCE_TYPE + "= 6")
    @OneToMany(mappedBy = RObjectReference.F_OWNER, orphanRemoval = true, cascade = ALL)
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyApproverRef") })
    public Set<RObjectReference<RFocus>> getModifyApproverRef() {
        if (modifyApproverRef == null) {
            modifyApproverRef = new HashSet<>();
        }
        return modifyApproverRef;
    }

    @Override
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyChannel") })
    public String getModifyChannel() {
        return modifyChannel;
    }

    @Override
    @Type(XMLGregorianCalendarType.class)
    @JaxbPath(itemPath = { @JaxbName(localPart = "metadata"), @JaxbName(localPart = "modifyTimestamp") })
    public XMLGregorianCalendar getModifyTimestamp() {
        return modifyTimestamp;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Collection<ROExtLong> getLongs() {
        return longs;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Collection<ROExtBoolean> getBooleans() {
        return booleans;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Collection<ROExtString> getStrings() {
        return strings;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Collection<ROExtDate> getDates() {
        return dates;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Collection<ROExtReference> getReferences() {
        return references;
    }

    @NotQueryable
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Collection<ROExtPolyString> getPolys() {
        return polys;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated
    @NotQueryable
    public RObjectType getObjectTypeClass() {
        return objectTypeClass;
    }

    @ElementCollection
    @CollectionTable(name = "m_object_subtype", joinColumns = {
            @JoinColumn(name = "object_oid", referencedColumnName = "oid",
                    foreignKey = @jakarta.persistence.ForeignKey(name = "fk_object_subtype")) })
    public Set<String> getSubtype() {
        return subtype;
    }

    public void setSubtype(Set<String> subtype) {
        this.subtype = subtype;
    }

    @Override
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

    @Override
    public void setCreateApproverRef(Set<RObjectReference<RFocus>> createApproverRef) {
        this.createApproverRef = createApproverRef;
    }

    @Override
    public void setCreateChannel(String createChannel) {
        this.createChannel = createChannel;
    }

    @Override
    public void setCreateTimestamp(XMLGregorianCalendar createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    @Override
    public void setCreatorRef(RSimpleEmbeddedReference creatorRef) {
        this.creatorRef = creatorRef;
    }

    @Override
    public void setModifierRef(RSimpleEmbeddedReference modifierRef) {
        this.modifierRef = modifierRef;
    }

    @Override
    public void setModifyApproverRef(Set<RObjectReference<RFocus>> modifyApproverRef) {
        this.modifyApproverRef = modifyApproverRef;
    }

    @Override
    public void setModifyChannel(String modifyChannel) {
        this.modifyChannel = modifyChannel;
    }

    @Override
    public void setModifyTimestamp(XMLGregorianCalendar modifyTimestamp) {
        this.modifyTimestamp = modifyTimestamp;
    }

    public void setFullObject(byte[] fullObject) {
        this.fullObject = fullObject;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setTenantRef(RSimpleEmbeddedReference tenantRef) {
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

    public void setPolys(Collection<ROExtPolyString> polys) {
        this.polys = polys;
    }

    public void setReferences(Collection<ROExtReference> references) {
        this.references = references;
    }

    public void setDates(Collection<ROExtDate> dates) {
        this.dates = dates;
    }

    public void setLongs(Collection<ROExtLong> longs) {
        this.longs = longs;
    }

    public void setStrings(Collection<ROExtString> strings) {
        this.strings = strings;
    }

    public void setBooleans(Collection<ROExtBoolean> booleans) {
        this.booleans = booleans;
    }

    @NotQueryable
    @OneToMany(mappedBy = "owner", orphanRemoval = true, cascade = ALL)
    public Set<RObjectTextInfo> getTextInfoItems() {
        if (textInfoItems == null) {
            textInfoItems = new HashSet<>();
        }
        return textInfoItems;
    }

    public void setTextInfoItems(Set<RObjectTextInfo> textInfoItems) {
        this.textInfoItems = textInfoItems;
    }

    @OneToMany(mappedBy = RAssignment.F_OWNER, orphanRemoval = true, cascade = ALL)
    @JaxbName(localPart = "operationExecution")
    public Set<ROperationExecution> getOperationExecutions() {
        if (operationExecutions == null) {
            operationExecutions = new HashSet<>();
        }
        return operationExecutions;
    }

    public void setOperationExecutions(
            Set<ROperationExecution> operationExecutions) {
        this.operationExecutions = operationExecutions;
    }

    public void setAssignments(Set<RAssignment> assignments) {
        this.assignments = assignments;
    }

    public void setRoleMembershipRef(Set<RObjectReference<RAbstractRole>> roleMembershipRef) {
        this.roleMembershipRef = roleMembershipRef;
    }

    public void setDelegatedRef(Set<RObjectReference<RFocus>> delegatedRef) {
        this.delegatedRef = delegatedRef;
    }

    public void setArchetypeRef(Set<RObjectReference<RArchetype>> archetypeRef) {
        this.archetypeRef = archetypeRef;
    }

    @ElementCollection
    @ForeignKey(name = "fk_object_policy_situation")
    @CollectionTable(name = "m_object_policy_situation", joinColumns = {
            @JoinColumn(name = "object_oid", referencedColumnName = "oid")
    })
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<String> getPolicySituation() {
        return policySituation;
    }

    public void setPolicySituation(Set<String> policySituation) {
        this.policySituation = policySituation;
    }

    static void copyAssignmentHolderInformationFromJAXB(AssignmentHolderType jaxb, RObject repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) throws DtoTranslationException {

        copyObjectInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.getRoleMembershipRef().addAll(
                RUtil.toRObjectReferenceSet(jaxb.getRoleMembershipRef(), repo, RReferenceType.ROLE_MEMBER, repositoryContext.relationRegistry));

        repo.getDelegatedRef().addAll(
                RUtil.toRObjectReferenceSet(jaxb.getDelegatedRef(), repo, RReferenceType.DELEGATED, repositoryContext.relationRegistry));

        repo.getArchetypeRef().addAll(
                RUtil.toRObjectReferenceSet(jaxb.getArchetypeRef(), repo, RReferenceType.ARCHETYPE, repositoryContext.relationRegistry));

        for (AssignmentType assignment : jaxb.getAssignment()) {
            RAssignment rAssignment = new RAssignment(repo, RAssignmentOwner.FOCUS);
            RAssignment.fromJaxb(assignment, rAssignment, jaxb, repositoryContext, generatorResult);

            repo.getAssignments().add(rAssignment);
        }
    }

    static void copyObjectInformationFromJAXB(ObjectType jaxb, RObject repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult)
            throws DtoTranslationException {
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        Objects.requireNonNull(repo, "Repo object must not be null.");

        repo.setTransient(generatorResult.isGeneratedOid());
        repo.setOid(jaxb.getOid());

        repo.setObjectTypeClass(RObjectType.getType(ClassMapper.getHQLTypeClass(jaxb.getClass())));
        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setLifecycleState(jaxb.getLifecycleState());

        repo.setSubtype(RUtil.listToSet(jaxb.getSubtype()));

        String strVersion = jaxb.getVersion();
        int version = StringUtils.isNotEmpty(strVersion) && strVersion.matches("[0-9]*")
                ? Integer.parseInt(jaxb.getVersion()) : 0;
        repo.setVersion(version);

        repo.getParentOrgRef().addAll(RUtil.toRObjectReferenceSet(jaxb.getParentOrgRef(),
                repo, RReferenceType.OBJECT_PARENT_ORG, repositoryContext.relationRegistry));

        for (TriggerType trigger : jaxb.getTrigger()) {
            RTrigger rTrigger = new RTrigger(null);
            RTrigger.fromJaxb(trigger, rTrigger, jaxb, repositoryContext, generatorResult);

            repo.getTrigger().add(rTrigger);
        }

        MetadataFactory.fromJaxb(jaxb.getMetadata(), repo, repositoryContext.relationRegistry);
        repo.setTenantRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTenantRef(), repositoryContext.relationRegistry));

        repo.setPolicySituation(RUtil.listToSet(jaxb.getPolicySituation()));

        if (jaxb.getExtension() != null) {
            copyExtensionOrAttributesFromJAXB(jaxb.getExtension().asPrismContainerValue(), repo, repositoryContext, RObjectExtensionType.EXTENSION, generatorResult);
        }

        repo.getTextInfoItems().addAll(RObjectTextInfo.createItemsSet(jaxb, repo, repositoryContext));
        for (OperationExecutionType opExec : jaxb.getOperationExecution()) {
            ROperationExecution rOpExec = new ROperationExecution(repo);
            ROperationExecution.fromJaxb(opExec, rOpExec, jaxb, repositoryContext, generatorResult);
            repo.getOperationExecutions().add(rOpExec);
        }
    }

    static void copyExtensionOrAttributesFromJAXB(PrismContainerValue<?> containerValue, RObject repo,
            RepositoryContext repositoryContext, RObjectExtensionType ownerType, IdGeneratorResult generatorResult) throws DtoTranslationException {
        RAnyConverter converter = new RAnyConverter(repositoryContext.prismContext, repositoryContext.extItemDictionary);

        Set<RAnyValue<?>> values = new HashSet<>();
        try {
            //TODO: is this enough? should we try items without definitions?
            for (Item<?, ?> item : containerValue.getItems()) {
                Set<RAnyValue<?>> converted = converter.convertToRValue(item, false, ownerType);
                if (generatorResult.isGeneratedOid()) {
                    converted.forEach(v -> v.setTransient(true));
                }
                values.addAll(converted);
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        for (RAnyValue<?> value : values) {
            ROExtValue<?> ex = (ROExtValue<?>) value;
            ex.setOwner(repo);
            ex.setOwnerType(ownerType);

            if (value instanceof ROExtDate) {
                repo.getDates().add((ROExtDate) value);
            } else if (value instanceof ROExtLong) {
                repo.getLongs().add((ROExtLong) value);
            } else if (value instanceof ROExtReference) {
                repo.getReferences().add((ROExtReference) value);
            } else if (value instanceof ROExtString) {
                repo.getStrings().add((ROExtString) value);
            } else if (value instanceof ROExtPolyString) {
                repo.getPolys().add((ROExtPolyString) value);
            } else if (value instanceof ROExtBoolean) {
                repo.getBooleans().add((ROExtBoolean) value);
            }
        }
    }

    // DO NOT override. All subclasses are "entities", not "values" and OID-based equals is good.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RObject rObject = (RObject) o;
        return Objects.equals(oid, rObject.oid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid);
    }

    @Override
    public String toString() {
        return RUtil.getDebugString(this);
    }
}
