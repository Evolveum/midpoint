package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class ResourceObjectsLocalBeans {

    @Autowired public ResourceObjectConverter resourceObjectConverter;
    @Autowired public FakeIdentifierGenerator fakeIdentifierGenerator;

}
