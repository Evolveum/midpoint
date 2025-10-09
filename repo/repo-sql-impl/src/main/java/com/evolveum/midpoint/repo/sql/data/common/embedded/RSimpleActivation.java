/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

import jakarta.persistence.Embeddable;

@JaxbType(type = ActivationType.class)
@Embeddable
public class RSimpleActivation extends RActivation {
}
