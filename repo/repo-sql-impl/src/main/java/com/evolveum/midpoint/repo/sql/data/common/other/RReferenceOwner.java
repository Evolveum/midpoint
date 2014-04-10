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

import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.type.*;
import org.apache.commons.lang.Validate;

/**
 * This is just helper enumeration for different types of reference entities
 * used in many relationships.
 *
 * @author lazyman
 */
public enum RReferenceOwner {

    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef}
     */
    OBJECT_PARENT_ORG(RParentOrgRef.class, RParentOrgRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RLinkRef}
     */
    USER_ACCOUNT(RLinkRef.class, RLinkRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RResourceApproverRef}
     */
    RESOURCE_BUSINESS_CONFIGURATON_APPROVER(RResourceApproverRef.class, RResourceApproverRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RRoleApproverRef}
     */
    ROLE_APPROVER(RRoleApproverRef.class, RRoleApproverRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RCreateApproverRef}
     */
    CREATE_APPROVER(RCreateApproverRef.class, RCreateApproverRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RModifyApproverRef}
     */
    MODIFY_APPROVER(RModifyApproverRef.class, RModifyApproverRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RIncludeRef}
     */
    INCLUDE(RIncludeRef.class, RIncludeRef.DISCRIMINATOR);

    private String discriminator;
    private Class<? extends RObjectReference> clazz;

    private RReferenceOwner(Class<? extends RObjectReference> clazz, String discriminator) {
        this.discriminator = discriminator;
        this.clazz = clazz;
    }

    /**
     * This is used for {@link org.hibernate.SessionFactory} fix in
     * {@link com.evolveum.midpoint.repo.sql.util.RUtil#fixCompositeIDHandling(org.hibernate.SessionFactory)}
     *
     * @return class based on reference type
     */
    public Class<? extends RObjectReference> getClazz() {
        return clazz;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public static RObjectReference createObjectReference(RReferenceOwner owner) {
        Validate.notNull(owner, "Reference owner must not be null.");

        switch (owner) {
            case OBJECT_PARENT_ORG:
                return new RParentOrgRef();
            case ROLE_APPROVER:
                return new RRoleApproverRef();
            case USER_ACCOUNT:
                return new RLinkRef();
            case RESOURCE_BUSINESS_CONFIGURATON_APPROVER:
                return new RResourceApproverRef();
            case CREATE_APPROVER:
                return new RCreateApproverRef();
            case MODIFY_APPROVER:
                return new RModifyApproverRef();
            case INCLUDE:
                return new RIncludeRef();
            default:
                throw new IllegalArgumentException("This is unknown reference owner: " + owner);
        }
    }
}
