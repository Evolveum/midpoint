/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ObjectReferenceAsserter<O extends ObjectType,R> extends AbstractAsserter<R> {

    final private PrismReferenceValue refVal;
    private PrismObject<? extends O> resolvedTarget = null;
    final private Class<O> defaultTargetTypeClass;

    public ObjectReferenceAsserter(PrismReferenceValue refVal, Class<O> defaultTargetTypeClass) {
        super();
        this.refVal = refVal;
        this.defaultTargetTypeClass = defaultTargetTypeClass;
    }

    public ObjectReferenceAsserter(PrismReferenceValue refVal, Class<O> defaultTargetTypeClass, String detail) {
        super(detail);
        this.refVal = refVal;
        this.defaultTargetTypeClass = defaultTargetTypeClass;
    }

    public ObjectReferenceAsserter(PrismReferenceValue refVal, Class<O> defaultTargetTypeClass, PrismObject<? extends O> resolvedTarget, R returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.refVal = refVal;
        this.defaultTargetTypeClass = defaultTargetTypeClass;
        this.resolvedTarget = resolvedTarget;
    }

    protected PrismReferenceValue getRefVal() {
        return refVal;
    }

    public String getOid() {
        return refVal.getOid();
    }

    public ObjectReferenceAsserter<O,R> assertOid() {
        assertNotNull("No OID in "+desc(), refVal.getOid());
        return this;
    }

    public ObjectReferenceAsserter<O,R> assertOid(String expected) {
        assertEquals("Wrong OID in "+desc(), expected, refVal.getOid());
        return this;
    }

    public ObjectReferenceAsserter<O,R> assertOidDifferentThan(String expected) {
        assertFalse("Wrong OID in "+desc(), expected.equals(refVal.getOid()));
        return this;
    }

    public PrismObjectAsserter<O,ObjectReferenceAsserter<O,R>> object() {
        return new PrismObjectAsserter<>((PrismObject<O>)refVal.getObject(), this, "object in "+desc());
    }

    protected PrismObject<O> getResolvedTarget() throws ObjectNotFoundException, SchemaException {
        if (resolvedTarget == null) {
            resolvedTarget = resolveTargetObject();
        }
        return (PrismObject<O>) resolvedTarget;
    }

    public PrismObjectAsserter<O,? extends ObjectReferenceAsserter<O,R>> target() throws ObjectNotFoundException, SchemaException {
        return new PrismObjectAsserter<>(getResolvedTarget(), this, "object resolved from "+desc());
    }

    public PrismObjectAsserter<O,? extends ObjectReferenceAsserter<O,R>> resolveTarget() throws ObjectNotFoundException, SchemaException {
        PrismObject<O> object = resolveTargetObject();
        return new PrismObjectAsserter<>(object, this, "object resolved from "+desc());
    }

    protected PrismObject<O> resolveTargetObject() throws ObjectNotFoundException, SchemaException {
        return resolveObject(getObjectTypeClass(), refVal.getOid());
    }

    private Class<O> getObjectTypeClass() {
        QName targetType = refVal.getTargetType();
        if (targetType == null) {
            return defaultTargetTypeClass;
        }
        return (Class<O>) ObjectTypes.getObjectTypeFromTypeQName(targetType).getClassDefinition();
    }

    protected String desc() {
        return descWithDetails(refVal);
    }

    public ObjectReferenceAsserter<O,R> display() {
        display(desc());
        return this;
    }

    public ObjectReferenceAsserter<O,R> display(String message) {
        IntegrationTestTools.display(message, refVal);
        return this;
    }
}
