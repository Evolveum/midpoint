/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 *
 * Note: considered to align this with ParentOrgRefFinder into some kind of common superclass.
 * But the resulting structure of generics is just too insane. It is lesser evil to have copy&pasted code.
 *
 * @author semancik
 */
public class ProjectionContextFinder<F extends ObjectType, MA extends ModelContextAsserter<F, RA>,RA> {

    private final ProjectionContextsAsserter<F,MA,RA> projectionsAsserter;
    private String shadowOid;
    private Boolean dead;

    public ProjectionContextFinder(ProjectionContextsAsserter<F,MA,RA> projAsserter) {
        this.projectionsAsserter = projAsserter;
    }

    public ProjectionContextFinder<F,MA,RA> shadowOid(String shadowOid) {
        this.shadowOid = shadowOid;
        return this;
    }

    public ProjectionContextFinder<F,MA,RA> dead(boolean dead) {
        this.dead = dead;
        return this;
    }

    public ProjectionContextAsserter<ProjectionContextsAsserter<F, MA, RA>> find() throws ObjectNotFoundException, SchemaException {
        ModelProjectionContext found = null;
        PrismObject<ShadowType> foundTarget = null;
        for (ModelProjectionContext projCtx: projectionsAsserter.getProjectionContexts()) {
            if (matches(projCtx)) {
                if (found == null) {
                    found = projCtx;
                } else {
                    fail("Found more than one link that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no projection context that matches search criteria");
        }
        return projectionsAsserter.forProjectionContext(found);
    }

    public ProjectionContextsAsserter<F,MA,RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
        int foundCount = 0;
        for (ModelProjectionContext projCtx: projectionsAsserter.getProjectionContexts()) {
            if (matches(projCtx)) {
                foundCount++;
            }
        }
        assertEquals("Wrong number of projection contexts for specified criteria in "+projectionsAsserter.desc(), expectedCount, foundCount);
        return projectionsAsserter;
    }

    private boolean matches(ModelProjectionContext projCtx) throws ObjectNotFoundException, SchemaException {

        if (shadowOid != null) {
            if (!shadowOid.equals(projCtx.getOid())) {
                return false;
            }
        }

        if (dead != null) {
            if (dead && !projCtx.getResourceShadowDiscriminator().isTombstone()) {
                return false;
            } else if (!dead && projCtx.getResourceShadowDiscriminator().isTombstone()) {
                return false;
            }
        }

        // TODO: more criteria
        return true;
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

}
