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

import com.evolveum.midpoint.repo.sql.data.common.RCObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.type.RCCreateApproverRef;
import com.evolveum.midpoint.repo.sql.data.common.type.RCModifyApproverRef;
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
    CREATE_APPROVER(RCCreateApproverRef.class, RCCreateApproverRef.DISCRIMINATOR),
    /**
     * this constant also have to be changed in
     * {@link com.evolveum.midpoint.repo.sql.data.common.type.RModifyApproverRef}
     */
    MODIFY_APPROVER(RCModifyApproverRef.class, RCModifyApproverRef.DISCRIMINATOR);

    private String discriminator;
    private Class<? extends RCObjectReference> clazz;

    private RCReferenceOwner(Class<? extends RCObjectReference> clazz, String discriminator) {
        this.discriminator = discriminator;
        this.clazz = clazz;
    }

    /**
     * This is used for {@link org.hibernate.SessionFactory} fix in
     * {@link com.evolveum.midpoint.repo.sql.util.RUtil#fixCompositeIDHandling(org.hibernate.SessionFactory)}
     *
     * @return class based on reference type
     */
    public Class<? extends RCObjectReference> getClazz() {
        return clazz;
    }

    public String getDiscriminator() {
        return discriminator;
    }

    public static RCObjectReference createObjectReference(RCReferenceOwner owner) {
        Validate.notNull(owner, "Reference owner must not be null.");

        switch (owner) {
            case CREATE_APPROVER:
                return new RCCreateApproverRef();
            case MODIFY_APPROVER:
                return new RCModifyApproverRef();
            default:
                throw new IllegalArgumentException("This is unknown reference owner: " + owner);
        }
    }
}
