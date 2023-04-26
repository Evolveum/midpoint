/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;


import org.hibernate.transform.ResultTransformer;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Specifies columns to return for AssignmentType searches and how to extract/process them.
 */
public final class GetAssignmentResult implements Serializable {

    public static final ResultStyle RESULT_STYLE = new ResultStyle() {
        @Override
        public ResultTransformer getResultTransformer() {
            return new ResultTransformer() {
                @Override
                public Object transformTuple(Object[] tuple, String[] aliases) {
                    return new GetAssignmentResult(tuple);
                }
            };
        }

        @Override
        public List<String> getIdentifiers(String rootAlias) {
            return Stream.of("ownerOid", "id", "order", "lifecycleState", "activation",
                    "targetRef", "tenantRef", "orgRef", "resourceRef",
                    "createTimestamp", "creatorRef", "createChannel",
                    "modifyTimestamp", "modifierRef", "modifyChannel")
                    .map(col -> rootAlias + '.' + col)
                    .collect(Collectors.toList());
        }

        @Override
        public String getCountString(String basePath) {
            return "*";
        }

        @Override
        public List<String> getContentAttributes(String rootAlias) {
            return Collections.emptyList();
        }
    };

    private final Object[] tuple;

    public GetAssignmentResult(Object[] tuple) {
        this.tuple = tuple;
    }

    public AssignmentType createAssignmentType(PrismContext prismContext)
            throws DtoTranslationException {
        // String ownerOid = (String) tuple[0]; unused for now

        int i = 1;
        // We could populate AssignmentType directly for most part, but we need some implementation
        // of Metadata anyway so we start with RAssignment after all.
        RAssignment row = new RAssignment();
        row.setId((Integer) tuple[i++]);
        row.setOrder((Integer) tuple[i++]);
        row.setLifecycleState((String) tuple[i++]);
        // skipping policySituation and extension to avoid to-many fetch
        row.setActivation((RActivation) tuple[i++]);
        row.setTargetRef((REmbeddedReference) tuple[i++]);
        row.setTenantRef((REmbeddedReference) tuple[i++]);
        row.setOrgRef((REmbeddedReference) tuple[i++]);
        row.setResourceRef((REmbeddedReference) tuple[i++]);
        // metadata
        row.setCreateTimestamp((XMLGregorianCalendar) tuple[i++]);
        row.setCreatorRef((REmbeddedReference) tuple[i++]);
        row.setCreateChannel((String) tuple[i++]);
        row.setModifyTimestamp((XMLGregorianCalendar) tuple[i++]);
        row.setModifierRef((REmbeddedReference) tuple[i++]);
        row.setModifyChannel((String) tuple[i]); // no ++ here, careful if adding more lines

        return new AssignmentType(prismContext)
                .id(row.getId() != null ? row.getId().longValue() : null)
                .order(row.getOrder())
                .lifecycleState(row.getLifecycleState())
                // .policySituation() to-many fetch, let's avoid it for now
                // .extension() to-many fetch, skipping now
                .activation(toActivation(row.getActivation(), prismContext))
                .targetRef(toObjectRef(row.getTargetRef(), prismContext))
                .tenantRef(toObjectRef(row.getTenantRef(), prismContext))
                .orgRef(toObjectRef(row.getOrgRef(), prismContext))
                .construction(toConstruction(row.getResourceRef(), prismContext))
                .metadata(MetadataFactory.toJAXB(row, prismContext));
    }

    private ActivationType toActivation(RActivation repoActivation, PrismContext prismContext)
            throws DtoTranslationException {
        if (repoActivation == null) {
            return null;
        }

        ActivationType activation = new ActivationType(prismContext);
        RActivation.fromJaxb(activation, repoActivation);
        return activation;
    }

    private ObjectReferenceType toObjectRef(REmbeddedReference repoRef, PrismContext prismContext) {
        if (repoRef == null) {
            return null;
        }

        ObjectReferenceType ref = new ObjectReferenceType();
        REmbeddedReference.copyToJAXB(repoRef, ref, prismContext);
        return ref;
    }

    private ConstructionType toConstruction(
            REmbeddedReference resourceRef, PrismContext prismContext) {
        if (resourceRef == null) {
            return null;
        }

        ConstructionType construction = new ConstructionType(prismContext);
        ObjectReferenceType ref = new ObjectReferenceType();
        REmbeddedReference.copyToJAXB(resourceRef, ref, prismContext);
        construction.setResourceRef(ref);
        return construction;
    }
}
