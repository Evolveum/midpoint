/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class ProjectionContextAsserter<RA> extends ElementContextAsserter<ModelProjectionContext,ShadowType,RA> {


    public ProjectionContextAsserter(ModelProjectionContext projectionContext) {
        super(projectionContext);
    }

    public ProjectionContextAsserter(ModelProjectionContext projectionContext, String detail) {
        super(projectionContext, detail);
    }

    public ProjectionContextAsserter(ModelProjectionContext projectionContext, RA returnAsserter, String detail) {
        super(projectionContext, returnAsserter, detail);
    }

    public ModelProjectionContext getProjectionContext() {
        return getElementContext();
    }

    @Override
    public ShadowAsserter<ProjectionContextAsserter<RA>> objectOld() {
        ShadowAsserter<ProjectionContextAsserter<RA>> shadowAsserter = new ShadowAsserter<ProjectionContextAsserter<RA>>(
                getProjectionContext().getObjectOld(), this, "object old in "+desc());
        copySetupTo(shadowAsserter);
        return shadowAsserter;
    }

    @Override
    public ShadowAsserter<ProjectionContextAsserter<RA>> objectCurrent() {
        ShadowAsserter<ProjectionContextAsserter<RA>> shadowAsserter = new ShadowAsserter<ProjectionContextAsserter<RA>>(
                getProjectionContext().getObjectCurrent(), this, "object current in "+desc());
        copySetupTo(shadowAsserter);
        return shadowAsserter;
    }

    @Override
    public ShadowAsserter<ProjectionContextAsserter<RA>> objectNew() {
        ShadowAsserter<ProjectionContextAsserter<RA>> shadowAsserter = new ShadowAsserter<ProjectionContextAsserter<RA>>(
                getProjectionContext().getObjectNew(), this, "object new in "+desc());
        copySetupTo(shadowAsserter);
        return shadowAsserter;
    }

    @Override
    public ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>> primaryDelta() {
        return (ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>>) super.primaryDelta();
    }

    @Override
    public ProjectionContextAsserter<RA> assertNoPrimaryDelta() {
        super.assertNoPrimaryDelta();
        return this;
    }

    @Override
    public ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>> secondaryDelta() {
        return (ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>>) super.secondaryDelta();
    }

    @Override
    public ProjectionContextAsserter<RA> assertNoSecondaryDelta() {
        super.assertNoSecondaryDelta();
        return this;
    }

    @Override
    protected String desc() {
        // TODO: better desc
        return descWithDetails(getProjectionContext());
    }

    @Override
    public ProjectionContextAsserter<RA> display() {
        display(desc());
        return this;
    }

    @Override
    public ProjectionContextAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, getProjectionContext());
        return this;
    }
}
