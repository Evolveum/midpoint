/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */

public class TestAuthSequenceUtil extends AbstractHigherUnitTest {

    private List<AuthenticationSequenceType> getSequences() {
        List<AuthenticationSequenceType> sequences = new ArrayList<>();
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.setName("sequence1");
        sequence.getNodeGroup().add(getReference("1"));
        sequence.setChannel(getChannel("gui1"));
        sequences.add(sequence);
        sequence = new AuthenticationSequenceType();
        sequence.setName("sequence2");
        sequence.getNodeGroup().add(getReference("1"));
        sequence.getNodeGroup().add(getReference("2"));
        sequence.setChannel(getChannel("gui2"));
        sequences.add(sequence);
        sequence = new AuthenticationSequenceType();
        sequence.setName("sequence3");
        sequence.setChannel(getChannel("gui3"));
        sequences.add(sequence);
        return sequences;
    }

    private AuthenticationSequenceChannelType getChannel(String key) {
        AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
        channel.setUrlSuffix(key);
        channel.setChannelId("channel#" + key);
        return channel;
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
        List<ObjectReferenceType> nodeGroups = new ArrayList<>();
        nodeGroups.add(getReference("2"));
        List<AuthenticationSequenceType> sequences = AuthSequenceUtil.getSequencesForNodeGroups(nodeGroups, authenticationPolicy);
        assertEquals("Wrong sequences", 2, sequences.size());
    }

    @Test
    public void TestGetSequenceByNodeGroups() {
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        authenticationPolicy.getSequence().addAll(getSequences());
        List<ObjectReferenceType> nodeGroups = new ArrayList<>();
        nodeGroups.add(getReference("1"));
        nodeGroups.add(getReference("2"));
        List<AuthenticationSequenceType> sequences = AuthSequenceUtil.getSequencesForNodeGroups(nodeGroups, authenticationPolicy);
        assertEquals("Wrong sequences", 3, sequences.size());
    }

    @Test
    public void TestGetSequenceByEmptyNodeGroups() {
        AuthenticationsPolicyType authenticationPolicy = new AuthenticationsPolicyType();
        authenticationPolicy.getSequence().addAll(getSequences());
        List<AuthenticationSequenceType> sequences = AuthSequenceUtil.getSequencesForNodeGroups(new ArrayList<>(), authenticationPolicy);
        assertEquals("Wrong sequences", 1, sequences.size());
    }

    /**
     * Sequence of a plain security policy:
     *
     * - modules are listed in the order in which they are executed
     * - the first listed module is kept
     */
    @Test
    public void testFirstExecutedModuleOfSequenceInExecutionOrder() {
        AuthenticationSequenceType sequence = getSequenceWithModules(
                getModule("userName", 10),
                getModule("ldapAuth", 30));

        AuthenticationSequenceType trimmed = AuthSequenceUtil.sequenceWithFirstExecutedModulesOnly(sequence, 1);

        assertEquals("Wrong modules", List.of("userName"), getModuleIdentifiers(trimmed));
    }

    /**
     * Sequence of a merged security policy:
     *
     * - archetype policy defines the LDAP module, the identification module is inherited and appended
     * - the appended module has a lower order, so it is executed first
     * - the executed module is kept, not the one listed first
     */
    @Test
    public void testFirstExecutedModuleOfMergedSequence() {
        AuthenticationSequenceType sequence = getSequenceWithModules(
                getModule("ldapAuth", 30),
                getModule("userName", 10));

        AuthenticationSequenceType trimmed = AuthSequenceUtil.sequenceWithFirstExecutedModulesOnly(sequence, 1);

        assertEquals("Wrong modules", List.of("userName"), getModuleIdentifiers(trimmed));
    }

    /**
     * Prefix of a merged security policy sequence:
     *
     * - the first two executed modules are kept, e.g. for a reset to the second module
     * - they are listed in the execution order, not in the document order
     */
    @Test
    public void testFirstTwoExecutedModulesOfMergedSequence() {
        AuthenticationSequenceType sequence = getSequenceWithModules(
                getModule("ldapAuth", 30),
                getModule("userName", 10));

        AuthenticationSequenceType trimmed = AuthSequenceUtil.sequenceWithFirstExecutedModulesOnly(sequence, 2);

        assertEquals("Wrong modules", List.of("userName", "ldapAuth"), getModuleIdentifiers(trimmed));
    }

    private AuthenticationSequenceType getSequenceWithModules(AuthenticationSequenceModuleType... modules) {
        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.setIdentifier("gui-default");
        for (AuthenticationSequenceModuleType module : modules) {
            sequence.getModule().add(module);
        }
        return sequence;
    }

    private AuthenticationSequenceModuleType getModule(String identifier, int order) {
        AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
        module.setIdentifier(identifier);
        module.setOrder(order);
        return module;
    }

    private List<String> getModuleIdentifiers(AuthenticationSequenceType sequence) {
        return sequence.getModule().stream()
                .map(AuthenticationSequenceModuleType::getIdentifier)
                .toList();
    }
}
