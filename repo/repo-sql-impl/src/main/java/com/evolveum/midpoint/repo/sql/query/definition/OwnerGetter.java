/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OwnerGetter {

    // hard to generify, can be <? extends RObject> but also a Container<? extends RObject>
    Class ownerClass();
}
