/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class TestSecurityUtils {

    private List<AuthenticationSequenceType> getSequences() {
        List<AuthenticationSequenceType> sequences = new ArrayList();
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.setName("sequence1");
        sequence.getNodeGroup().add(getReference("1"));
        sequences.add(sequence);
        sequence = new AuthenticationSequenceType();
        sequence.setName("sequence2");
        sequence.getNodeGroup().add(getReference("1"));
        sequence.getNodeGroup().add(getReference("2"));
        sequences.add(sequence);
        sequence = new AuthenticationSequenceType();
        sequence.setName("sequence3");
        sequences.add(sequence);
        return sequences;
    }

    private ObjectReferenceType getReference(String oid) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(oid);
        return ref;
    }

    @Test
    public void TestGetSequenceByNodeGroup() {
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        authenticationPolicy.getSequence().addAll(getSequences());
        List<ObjectReferenceType> nodeGroups = new ArrayList();
        nodeGroups.add(getReference("2"));
        List<AuthenticationSequenceType> sequences = SecurityUtils.getSequencesForNodeGroups(nodeGroups, authenticationPolicy);
        assertEquals("Wrong sequences", 2, sequences.size());
    }

    @Test
    public void TestGetSequenceByNodeGroups() {
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        authenticationPolicy.getSequence().addAll(getSequences());
        List<ObjectReferenceType> nodeGroups = new ArrayList();
        nodeGroups.add(getReference("1"));
        nodeGroups.add(getReference("2"));
        List<AuthenticationSequenceType> sequences = SecurityUtils.getSequencesForNodeGroups(nodeGroups, authenticationPolicy);
        assertEquals("Wrong sequences", 3, sequences.size());
    }

    @Test
    public void TestGetSequenceByEmptyNodeGroups() {
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        authenticationPolicy.getSequence().addAll(getSequences());
        List<AuthenticationSequenceType> sequences = SecurityUtils.getSequencesForNodeGroups(new ArrayList(), authenticationPolicy);
        assertEquals("Wrong sequences", 1, sequences.size());
    }

}
