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

package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.type.RACreateApproverRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RAModifyApproverRef;
import org.apache.commons.lang.Validate;

/**
 * This is just helper enumeration for different types of reference entities
 * used in many relationships.
 *
 * @author lazyman
 */
public enum RCReferenceOwner {

    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RCreateApproverRef}
     */
    CREATE_APPROVER(RACreateApproverRef.class, RACreateApproverRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RModifyApproverRef}
     */
    MODIFY_APPROVER(RAModifyApproverRef.class, RAModifyApproverRef.DISCRIMINATOR);

    private String discriminator;
    private Class<? extends RAssignmentReference> clazz;

    private RCReferenceOwner(Class<? extends RAssignmentReference> clazz, String discriminator) {
        this.discriminator = discriminator;
        this.clazz = clazz;
    }

    /**
     * This is used for {@link org.hibernate.SessionFactory} fix in
     * {@link com.evolveum.midpoint.repo.sql.util.RUtil#fixCompositeIDHandling(org.hibernate.SessionFactory)}
     *
     * @return class based on reference type
     */
    public Class<? extends RAssignmentReference> getClazz() {
        return clazz;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public static RAssignmentReference createObjectReference(RCReferenceOwner owner) {
        Validate.notNull(owner, "Reference owner must not be null.");

        switch (owner) {
            case CREATE_APPROVER:
                return new RACreateApproverRef();
            case MODIFY_APPROVER:
                return new RAModifyApproverRef();
            default:
                throw new IllegalArgumentException("This is unknown reference owner: " + owner);
        }
    }
}
