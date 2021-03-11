/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class LinksAsserter<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> extends AbstractAsserter<FA> {

    private final FA focusAsserter;
    private List<PrismReferenceValue> links;

    public LinksAsserter(FA focusAsserter) {
        super();
        this.focusAsserter = focusAsserter;
    }

    public LinksAsserter(FA focusAsserter, String details) {
        super(details);
        this.focusAsserter = focusAsserter;
    }

    public static <F extends FocusType> LinksAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
        return new LinksAsserter<>(FocusAsserter.forFocus(focus));
    }

    PrismObject<ShadowType> getLinkTarget(String oid) throws ObjectNotFoundException, SchemaException {
        return focusAsserter.getCachedObject(ShadowType.class, oid);
    }

    List<PrismReferenceValue> getAllLinks() {
        if (links == null) {
            PrismReference linkRef = getFocus().findReference(FocusType.F_LINK_REF);
            if (linkRef == null) {
                links = new ArrayList<>();
            } else {
                links = linkRef.getValues();
            }
        }
        return links;
    }

    private List<PrismReferenceValue> getLiveLinks() {
        RelationRegistry relationRegistry = SchemaService.get().relationRegistry();
        return getAllLinks().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .collect(Collectors.toList());
    }

    private List<PrismReferenceValue> getDeadLinks() {
        RelationRegistry relationRegistry = SchemaService.get().relationRegistry();
        return getAllLinks().stream()
                .filter(ref -> !relationRegistry.isMember(ref.getRelation()))
                .collect(Collectors.toList());
    }

    public LinksAsserter<F, FA, RA> assertLiveLinks(int expected) {
        assertEquals("Wrong number of live links in " + desc(), expected, getLiveLinks().size());
        return this;
    }

    public LinksAsserter<F, FA, RA> assertDeadLinks(int expected) {
        assertEquals("Wrong number of related links in " + desc(), expected, getDeadLinks().size());
        return this;
    }

    public LinksAsserter<F, FA, RA> assertLinks(int live, int dead) {
        assertLiveLinks(live);
        assertDeadLinks(dead);
        return this;
    }

    public LinksAsserter<F, FA, RA> assertLinks(int expected) {
        assertEquals("Wrong number of links in " + desc(), expected, getAllLinks().size());
        return this;
    }

    public LinksAsserter<F, FA, RA> assertNone() {
        assertLiveLinks(0);
        return this;
    }

    ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> forLink(PrismReferenceValue refVal, PrismObject<ShadowType> shadow) {
        ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> asserter = new ShadowReferenceAsserter<>(refVal, shadow, this, "link in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> singleAny() {
        assertLinks(1);
        return forLink(getAllLinks().get(0), null);
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> singleLive() {
        assertLiveLinks(1);
        return forLink(getLiveLinks().get(0), null);
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> singleDead() {
        assertDeadLinks(1);
        return forLink(getDeadLinks().get(0), null);
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> link(String oid) {
        for (PrismReferenceValue link : getAllLinks()) {
            if (oid.equals(link.getOid())) {
                return forLink(link, null);
            }
        }
        fail("No link with OID "+oid+" in "+desc());
        return null; // not reached
    }

    PrismObject<F> getFocus() {
        return focusAsserter.getObject();
    }

    @Override
    public FA end() {
        return focusAsserter;
    }

    @Override
    protected String desc() {
        return descWithDetails("links of "+getFocus());
    }

    public LinkFinder<F,FA,RA> by() {
        return new LinkFinder<>(this);
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> projectionOnResource(String resourceOid) throws ObjectNotFoundException, SchemaException {
        return by()
            .resourceOid(resourceOid)
            .find();
    }

    public ShadowReferenceAsserter<LinksAsserter<F, FA, RA>> deadShadow(String resourceOid) throws ObjectNotFoundException, SchemaException {
        return by()
            .dead(true)
            .find();
    }

    public List<String> getOids() {
        List<String> oids = new ArrayList<>();
        for (PrismReferenceValue link: getAllLinks()) {
            oids.add(link.getOid());
        }
        return oids;
    }

}
