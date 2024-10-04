/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Radovan Semancik
 *
 */
public abstract class AbstractAsserter<RA> {

    private final String details;
    private RA returnAsserter;
    private PrismContext prismContext;
    private SimpleObjectResolver objectResolver;
    private RepositoryService repositoryService;
    private Protector protector;
    private Clock clock;
    protected ObjectType expectedActor;

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

    public ObjectType getExpectedActor() {
        return expectedActor;
    }

    public void setExpectedActor(ObjectType expectedActor) {
        this.expectedActor = expectedActor;
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
            return o+" ("+details+")";
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

    public RA getReturnAsserter() {
        return returnAsserter;
    }

    protected void assertEventMarks(TestObject<MarkType>[] expectedTags, Collection<String> realTags) {
        if (!getRepositoryService().supportsMarks()) {
            return;
        }
        Set<String> expectedTagsOids = Arrays.stream(expectedTags)
                .map(r -> r.oid)
                .collect(Collectors.toSet());
        assertThat(realTags)
                .as("event marks")
                .containsExactlyInAnyOrderElementsOf(expectedTagsOids);
    }

    protected PrismContainer<ValueMetadataType> getValueMetadata(
            PrismContainerValue<?> parent, ItemPath path, ValueSelector<? extends PrismValue> valueSelector) {
        Object o = parent.find(path);
        if (o instanceof PrismValue prismValue) {
            return prismValue.getValueMetadataAsContainer();
        } else if (o instanceof Item<?, ?> item) {
            if (valueSelector == null) {
                if (item.size() == 1) {
                    return item.getValue().getValueMetadataAsContainer();
                } else {
                    throw new AssertionError(
                            "Item '%s' has not a single value in %s: %d values: %s".formatted(
                                    path, parent, item.size(), item));
                }
            } else {
                //noinspection unchecked,rawtypes
                PrismValue anyValue = item.getAnyValue((ValueSelector) valueSelector);
                if (anyValue != null) {
                    return anyValue.getValueMetadataAsContainer();
                } else {
                    throw new AssertionError(
                            "Item '%s' has no value matching given selector in %s: %d values: %s".formatted(
                                    path, parent, item.size(), item));
                }
            }
        } else if (o != null) {
            throw new AssertionError(
                    "Object '%s' has no unexpected value matching given selector in %s: %s".formatted(path, parent, o));
        } else {
            throw new AssertionError("Item '%s' not found in %s".formatted(path, parent));
        }
    }
}
