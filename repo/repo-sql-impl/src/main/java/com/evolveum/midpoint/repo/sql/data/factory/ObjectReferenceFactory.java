package com.evolveum.midpoint.repo.sql.data.factory;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
public class ObjectReferenceFactory {

    public static List<ObjectReferenceType> safeSetReferencesToList(Set<ObjectReference> set, PrismContext context) {
        List<ObjectReferenceType> list = new ArrayList<ObjectReferenceType>();

        if (set == null || set.isEmpty()) {
            return list;
        }

        for (ObjectReference str : set) {
            ObjectReferenceType ort = new ObjectReferenceType();
            copyToJAXB(str, ort, context);
            list.add(ort);
        }
        return list;
    }

    public static void copyToJAXB(ObjectReference repo, ObjectReferenceType jaxb, PrismContext context) {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setType(ClassMapper.getQNameForHQLType(repo.getType()));
        jaxb.setOid(repo.getTargetOid());
        jaxb.setRelation(RUtil.stringToQName(repo.getRelation()));
    }

    public static Set<RObjectReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext context,
                                                               RObject owner, RReferenceOwner refOwner) {
        Set<RObjectReference> set = new HashSet<RObjectReference>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RObjectReference rRef = RUtil.jaxbRefToRepo(ref, context, owner, refOwner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static Set<RAssignmentReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext context,
                                                                RAssignment owner, RCReferenceOwner refOwner) {
        Set<RAssignmentReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RAssignmentReference rRef = jaxbRefToAssignmentRepo(ref, context, owner, refOwner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }


    public static RAssignmentReference jaxbRefToAssignmentRepo(ObjectReferenceType reference, PrismContext prismContext,
                                                            RAssignment owner, RCReferenceOwner refOwner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notNull(refOwner, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RAssignmentReference repoRef = RCReferenceOwner.createObjectReference(refOwner);
        repoRef.setOwner(owner);
        RAssignmentReference.copyFromJAXB(reference, repoRef, prismContext);

        return repoRef;
    }
}
