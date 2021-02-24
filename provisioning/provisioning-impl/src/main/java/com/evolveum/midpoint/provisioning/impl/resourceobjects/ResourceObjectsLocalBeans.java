/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ResourceObjectsLocalBeans {

    @Autowired public ResourceObjectConverter resourceObjectConverter;
    @Autowired public FakeIdentifierGenerator fakeIdentifierGenerator;

}
