package com.evolveum.midpoint.repo.sql.data.factory;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            List refs = RUtil.safeSetReferencesToList(repo.getCreateApproverRef(), context);
            if (!refs.isEmpty()) {
                jaxb.getCreateApproverRef().addAll(refs);
            }
            refs = RUtil.safeSetReferencesToList(repo.getModifyApproverRef(), context);
            if (!refs.isEmpty()) {
                jaxb.getModifyApproverRef().addAll(refs);
            }
        } else {

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

    public static void fromJAXB(MetadataType jaxb, Metadata repo, PrismContext prismContext)
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

        repo.setCreatorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getCreatorRef(), prismContext));
        repo.setModifierRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getModifierRef(), prismContext));

        if (repo instanceof RObject) {
            repo.getCreateApproverRef().addAll(RUtil.safeListReferenceToSet(jaxb.getCreateApproverRef(), prismContext,
                    (RObject) repo, RReferenceOwner.CREATE_APPROVER));
            repo.getModifyApproverRef().addAll(RUtil.safeListReferenceToSet(jaxb.getModifyApproverRef(), prismContext,
                    (RObject) repo, RReferenceOwner.MODIFY_APPROVER));
        } else {
            repo.getCreateApproverRef().addAll(safeListReferenceToSet(jaxb.getCreateApproverRef(), prismContext,
                    (RAssignment) repo, RCReferenceOwner.CREATE_APPROVER));
            repo.getModifyApproverRef().addAll(safeListReferenceToSet(jaxb.getModifyApproverRef(), prismContext,
                    (RAssignment) repo, RCReferenceOwner.MODIFY_APPROVER));
        }
    }

    public static boolean equals(Metadata m1, Metadata m2) {
        if (m1 == m2) return true;

        if (m1.getCreateApproverRef() != null ? !m1.getCreateApproverRef().equals(m2.getCreateApproverRef()) : m2.getCreateApproverRef() != null)
            return false;
        if (m1.getCreateChannel() != null ? !m1.getCreateChannel().equals(m2.getCreateChannel()) : m2.getCreateChannel() != null)
            return false;
        if (m1.getCreateTimestamp() != null ? !m1.getCreateTimestamp().equals(m2.getCreateTimestamp()) : m2.getCreateTimestamp() != null)
            return false;
        if (m1.getCreatorRef() != null ? !m1.getCreatorRef().equals(m2.getCreatorRef()) : m2.getCreatorRef() != null)
            return false;
        if (m1.getModifierRef() != null ? !m1.getModifierRef().equals(m2.getModifierRef()) : m2.getModifierRef() != null)
            return false;
        if (m1.getModifyApproverRef() != null ? !m1.getModifyApproverRef().equals(m2.getModifyApproverRef()) : m2.getModifyApproverRef() != null)
            return false;
        if (m1.getModifyChannel() != null ? !m1.getModifyChannel().equals(m2.getModifyChannel()) : m2.getModifyChannel() != null)
            return false;
        if (m1.getModifyTimestamp() != null ? !m1.getModifyTimestamp().equals(m2.getModifyTimestamp()) : m2.getModifyTimestamp() != null)
            return false;

        return true;
    }

    public static Set<RAssignmentReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
                                                                   RAssignment owner, RCReferenceOwner refOwner) {
        Set<RAssignmentReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RAssignmentReference rRef = jaxbRefToRepo(ref, prismContext, owner, refOwner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RAssignmentReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
                                                     RAssignment owner, RCReferenceOwner refOwner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notNull(refOwner, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RAssignmentReference repoRef = new RAssignmentReference();
        repoRef.setReferenceType(refOwner);
        repoRef.setOwner(owner);
        RAssignmentReference.copyFromJAXB(reference, repoRef);

        return repoRef;
    }
}
