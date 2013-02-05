/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

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
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RAccountRef}
     */
    USER_ACCOUNT(RAccountRef.class, RAccountRef.DISCRIMINATOR),
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
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.ROrgRootRef}
     */
    SYSTEM_CONFIGURATION_ORG_ROOT(ROrgRootRef.class, ROrgRootRef.DISCRIMINATOR);

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
            case SYSTEM_CONFIGURATION_ORG_ROOT:
                return new ROrgRootRef();
            case USER_ACCOUNT:
                return new RAccountRef();
            case RESOURCE_BUSINESS_CONFIGURATON_APPROVER:
                return new RResourceApproverRef();
            default:
                throw new IllegalArgumentException("This is unknown reference owner: " + owner);
        }
    }
}
