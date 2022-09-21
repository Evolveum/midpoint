/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.repo.api.RepositoryService;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Radovan Semancik
 *
 */
public abstract class AbstractAsserter<RA> {

    private String details;
    private RA returnAsserter;
    private PrismContext prismContext;
    private SimpleObjectResolver objectResolver;
    private RepositoryService repositoryService;
    private Protector protector;
    private Clock clock;

    public AbstractAsserter() {
        this(null);
    }

    public AbstractAsserter(String details) {
        this.details = details;
    }

    public AbstractAsserter(RA returnAsserter, String details) {
        this.returnAsserter = returnAsserter;
        this.details = details;
    }

    protected PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    protected SimpleObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public void setObjectResolver(SimpleObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public AbstractAsserter<RA> withObjectResolver(SimpleObjectResolver objectResolver) {
        setObjectResolver(objectResolver);
        return this;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    protected Protector getProtector() {
        return protector;
    }

    public void setProtector(Protector protector) {
        this.protector = protector;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    protected String getDetails() {
        return details;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    protected String descWithDetails(Object o) {
        if (o == null) {
            if (details == null) {
                return "null";
            } else {
                return "null("+details+")";
            }
        }
        if (details == null) {
            return o.toString();
        } else {
            return o.toString()+" ("+details+")";
        }
    }

    public RA end() {
        return returnAsserter;
    }

    protected <O extends ObjectType> PrismObject<O> resolveObject(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException {
        if (objectResolver == null) {
            throw new IllegalStateException("Cannot resolve object "+type.getSimpleName()+" "+oid+" because there is no resolver");
        }
        OperationResult result = new OperationResult("AbstractAsserter.resolveObject");
        return objectResolver.getObject(type, oid, null, result);
    }

    abstract protected String desc();

    protected <T> void copySetupTo(AbstractAsserter<T> other) {
        other.setPrismContext(this.getPrismContext());
        other.setObjectResolver(this.getObjectResolver());
        other.setRepositoryService(this.getRepositoryService());
        other.setProtector(this.getProtector());
        other.setClock(this.getClock());
    }

    protected void assertMinMax(String message, int expectedMin, int expectedMax, int value) {
        if (value < expectedMin || value > expectedMax) {
            fail(message + ": expected " + expectedMin + "-" + expectedMax + ", real value is " + value);
        }
    }
}
