/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.factory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author lazyman
 */
public class MetadataFactory {

    public static MetadataType toJAXB(Metadata repo, PrismContext context) {
        if (isNull(repo)) {
            return null;
        }

        MetadataType jaxb = new MetadataType();

        jaxb.setCreateChannel(repo.getCreateChannel());
        jaxb.setCreateTimestamp(repo.getCreateTimestamp());
        jaxb.setModifyChannel(repo.getModifyChannel());
        jaxb.setModifyTimestamp(repo.getModifyTimestamp());

        if (repo.getCreatorRef() != null) {
            jaxb.setCreatorRef(repo.getCreatorRef().toJAXB(context));
        }
        if (repo.getModifierRef() != null) {
            jaxb.setModifierRef(repo.getModifierRef().toJAXB(context));
        }

        if (repo instanceof RObject) {
            List refs = RUtil.toObjectReferenceTypeList(repo.getCreateApproverRef());
            if (!refs.isEmpty()) {
                jaxb.getCreateApproverRef().addAll(refs);
            }
            refs = RUtil.toObjectReferenceTypeList(repo.getModifyApproverRef());
            if (!refs.isEmpty()) {
                jaxb.getModifyApproverRef().addAll(refs);
            }
        }

        return jaxb;
    }

    private static boolean isNull(Metadata repo) {
        return StringUtils.isNotEmpty(repo.getCreateChannel())
                && repo.getCreateTimestamp() == null
                && (repo.getCreateApproverRef() == null || repo.getCreateApproverRef().isEmpty())
                && repo.getCreatorRef() == null
                && StringUtils.isNotEmpty(repo.getModifyChannel())
                && repo.getModifyTimestamp() == null
                && (repo.getModifyApproverRef() == null || repo.getModifyApproverRef().isEmpty())
                && repo.getModifierRef() == null;
    }

    public static void fromJaxb(
            MetadataType jaxb, Metadata repo, RelationRegistry relationRegistry)
            throws DtoTranslationException {
        if (jaxb == null) {
            repo.setCreateChannel(null);
            repo.setCreateTimestamp(null);

            repo.setModifyChannel(null);
            repo.setModifyTimestamp(null);

            repo.setCreatorRef(null);
            repo.setModifierRef(null);

            repo.getCreateApproverRef().clear();
            repo.getModifyApproverRef().clear();

            return;
        }

        repo.setCreateChannel(jaxb.getCreateChannel());
        repo.setCreateTimestamp(jaxb.getCreateTimestamp());
        repo.setModifyChannel(jaxb.getModifyChannel());
        repo.setModifyTimestamp(jaxb.getModifyTimestamp());

        repo.setCreatorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getCreatorRef(), relationRegistry));
        repo.setModifierRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getModifierRef(), relationRegistry));

        if (repo instanceof RObject) {
            repo.getCreateApproverRef().clear();
            repo.getCreateApproverRef().addAll(
                    RUtil.toRObjectReferenceSet(jaxb.getCreateApproverRef(),
                            (RObject) repo, RReferenceType.CREATE_APPROVER, relationRegistry));
            repo.getModifyApproverRef().clear();
            repo.getModifyApproverRef().addAll(
                    RUtil.toRObjectReferenceSet(jaxb.getModifyApproverRef(),
                            (RObject) repo, RReferenceType.MODIFY_APPROVER, relationRegistry));
        } else {
            repo.getCreateApproverRef().clear();
            repo.getCreateApproverRef().addAll(
                    safeListReferenceToSet(
                            jaxb.getCreateApproverRef(), (RAssignment) repo,
                            RCReferenceType.CREATE_APPROVER, relationRegistry));
            repo.getModifyApproverRef().clear();
            repo.getModifyApproverRef().addAll(
                    safeListReferenceToSet(
                            jaxb.getModifyApproverRef(), (RAssignment) repo,
                            RCReferenceType.MODIFY_APPROVER, relationRegistry));
        }
    }

    public static boolean equals(Metadata m1, Metadata m2) {
        if (m1 == m2) { return true; }

        if (m1.getCreateApproverRef() != null ? !m1.getCreateApproverRef().equals(m2.getCreateApproverRef()) : m2.getCreateApproverRef() != null) {
            return false;
        }
        if (m1.getCreateChannel() != null ? !m1.getCreateChannel().equals(m2.getCreateChannel()) : m2.getCreateChannel() != null) {
            return false;
        }
        if (m1.getCreateTimestamp() != null ? !m1.getCreateTimestamp().equals(m2.getCreateTimestamp()) : m2.getCreateTimestamp() != null) {
            return false;
        }
        if (m1.getCreatorRef() != null ? !m1.getCreatorRef().equals(m2.getCreatorRef()) : m2.getCreatorRef() != null) {
            return false;
        }
        if (m1.getModifierRef() != null ? !m1.getModifierRef().equals(m2.getModifierRef()) : m2.getModifierRef() != null) {
            return false;
        }
        if (m1.getModifyApproverRef() != null ? !m1.getModifyApproverRef().equals(m2.getModifyApproverRef()) : m2.getModifyApproverRef() != null) {
            return false;
        }
        if (m1.getModifyChannel() != null ? !m1.getModifyChannel().equals(m2.getModifyChannel()) : m2.getModifyChannel() != null) {
            return false;
        }
        if (m1.getModifyTimestamp() != null ? !m1.getModifyTimestamp().equals(m2.getModifyTimestamp()) : m2.getModifyTimestamp() != null) {
            return false;
        }

        return true;
    }

    public static Set<RAssignmentReference> safeListReferenceToSet(List<ObjectReferenceType> list,
            RAssignment owner, RCReferenceType refOwner, RelationRegistry relationRegistry) {
        Set<RAssignmentReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RAssignmentReference rRef = jaxbRefToRepo(ref, owner, refOwner, relationRegistry);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RAssignmentReference jaxbRefToRepo(ObjectReferenceType reference,
            RAssignment owner, RCReferenceType refOwner, RelationRegistry relationRegistry) {
        if (reference == null) {
            return null;
        }
        Objects.requireNonNull(owner, "Owner of reference must not be null.");
        Objects.requireNonNull(refOwner, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RAssignmentReference repoRef = new RAssignmentReference();
        repoRef.setReferenceType(refOwner);
        repoRef.setOwner(owner);
        RAssignmentReference.fromJaxb(reference, repoRef, relationRegistry);

        return repoRef;
    }
}
