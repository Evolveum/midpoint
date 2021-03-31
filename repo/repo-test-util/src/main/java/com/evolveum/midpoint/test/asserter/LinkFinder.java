/*
 * Copyright (c) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.QNameUtil;

import org.apache.commons.lang3.ArrayUtils;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 *
 * Note: considered to align this with ParentOrgRefFinder into some kind of common superclass.
 * But the resulting structure of generics is just too insane. It is lesser evil to have copy&pasted code.
 *
 * @author semancik
 */
public class LinkFinder<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> {

    private final LinksAsserter<F,FA,RA> linksAsserter;
    private String resourceOid;
    private Boolean dead;
    private QName relation;
    private String intent;
    private String tag;
    private String[] notTags;

    public LinkFinder(LinksAsserter<F,FA,RA> linksAsserter) {
        this.linksAsserter = linksAsserter;
    }

    public LinkFinder<F,FA,RA> resourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
        return this;
    }

    public LinkFinder<F,FA,RA> dead(boolean dead) {
        this.dead = dead;
        return this;
    }

    public LinkFinder<F,FA,RA> relation(QName relation) {
        this.relation = relation;
        return this;
    }

    public LinkFinder<F,FA,RA> intent(String intent) {
        this.intent = intent;
        return this;
    }

    public LinkFinder<F,FA,RA> tag(String tag) {
        this.tag = tag;
        return this;
    }

    public LinkFinder<F,FA,RA> notTags(String... tags) {
        this.notTags = tags;
        return this;
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> find() throws ObjectNotFoundException, SchemaException {
        PrismReferenceValue found = null;
        PrismObject<ShadowType> foundTarget = null;
        for (PrismReferenceValue link: linksAsserter.getAllLinks()) {
            PrismObject<ShadowType> linkTarget = linksAsserter.getLinkTarget(link.getOid());
            if (matches(link, linkTarget)) {
                if (found == null) {
                    found = link;
                    foundTarget = linkTarget;
                } else {
                    fail("Found more than one link that matches search criteria");
                }
            }
        }
        if (found == null) {
            fail("Found no link that matches search criteria");
        }
        return linksAsserter.forLink(found, foundTarget);
    }

    public LinksAsserter<F,FA,RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
        int foundCount = 0;
        for (PrismReferenceValue link: linksAsserter.getAllLinks()) {
            PrismObject<ShadowType> linkTarget = linksAsserter.getLinkTarget(link.getOid());
            if (matches(link, linkTarget)) {
                foundCount++;
            }
        }
        assertEquals("Wrong number of links for specified criteria in "+linksAsserter.desc(), expectedCount, foundCount);
        return linksAsserter;
    }

    private boolean matches(PrismReferenceValue refVal, PrismObject<ShadowType> linkTarget) throws ObjectNotFoundException, SchemaException {
        ShadowType linkTargetType = linkTarget.asObjectable();

        if (resourceOid != null) {
            if (!resourceOid.equals(linkTargetType.getResourceRef().getOid())) {
                return false;
            }
        }

        if (dead != null) {
            if (dead && !ShadowUtil.isDead(linkTargetType)) {
                return false;
            } else if (!dead && ShadowUtil.isDead(linkTargetType)) {
                return false;
            }
        }

        if (relation != null) {
            if (!SchemaService.get().prismContext().relationMatches(relation, refVal.getRelation())) {
                return false;
            }
        }

        if (intent != null) {
            if (!intent.equals(linkTargetType.getIntent())) {
                return false;
            }
        }

        if (tag != null) {
            if (!tag.equals(linkTargetType.getTag())) {
                return false;
            }
        }

        if (notTags != null) {
            if (ArrayUtils.contains(notTags, linkTargetType.getTag())) {
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
