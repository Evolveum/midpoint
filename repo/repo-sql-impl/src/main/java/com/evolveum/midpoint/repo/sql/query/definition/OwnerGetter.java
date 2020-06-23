/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes method that is used to access the container parent. E.g. AccessCertificationCase -> AccessCertificationCampaign.
 *
 * @author lazyman
 * @author mederly
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OwnerGetter {

    // hard to generify, can be <? extends RObject> but also a Container<? extends RObject>
    Class ownerClass();
}
