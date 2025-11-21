/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.smart.api.SynchronizationConfigurationScenario;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-smart-integration-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPredefinedSynchronizationReactions extends AbstractSmartIntegrationTest {

    @Test
    public void test500PredefinedReactionsForSource() {
        var reactions = smartIntegrationService.getPredefinedSynchronizationReactions(
                SynchronizationConfigurationScenario.SOURCE, true); // flag irrelevant for SOURCE

        // Expect exactly UNMATCHED, DELETED, UNLINKED, LINKED
        assertThat(reactions.getReaction()).hasSize(4);

        var unmatched = findReaction(reactions, SynchronizationSituationType.UNMATCHED);
        assertThat(unmatched.getActions().getAddFocus()).hasSize(1);
        assertThat(unmatched.getActions().getAddFocus().get(0).getOrder()).isEqualTo(1);
        assertNoOtherActions(unmatched.getActions(), Set.of("addFocus"));

        var deleted = findReaction(reactions, SynchronizationSituationType.DELETED);
        assertThat(deleted.getActions().getDeleteFocus()).hasSize(1);
        assertThat(deleted.getActions().getDeleteFocus().get(0).getOrder()).isEqualTo(1);
        assertThat(deleted.getActions().getInactivateFocus()).hasSize(1);
        assertThat(deleted.getActions().getInactivateFocus().get(0).getOrder()).isEqualTo(2);
        assertThat(deleted.getActions().getSynchronize()).hasSize(1);
        assertThat(deleted.getActions().getSynchronize().get(0).getOrder()).isEqualTo(3);

        var unlinked = findReaction(reactions, SynchronizationSituationType.UNLINKED);
        assertThat(unlinked.getActions().getLink()).hasSize(1);
        assertThat(unlinked.getActions().getLink().get(0).getOrder()).isEqualTo(1);
        assertNoOtherActions(unlinked.getActions(), Set.of("link"));

        var linked = findReaction(reactions, SynchronizationSituationType.LINKED);
        assertThat(linked.getActions().getSynchronize()).hasSize(1);
        assertThat(linked.getActions().getSynchronize().get(0).getOrder()).isEqualTo(1);
        assertNoOtherActions(linked.getActions(), Set.of("synchronize"));

        // DISPUTED is not present for SOURCE
        assertThat(reactions.getReaction().stream()
                .noneMatch(r -> r.getSituation().contains(SynchronizationSituationType.DISPUTED))).isTrue();
    }

    @Test
    public void test510PredefinedReactionsForTargetWithCorrelationCase() {
        var reactions = smartIntegrationService.getPredefinedSynchronizationReactions(
                SynchronizationConfigurationScenario.TARGET, true);

        // Expect UNMATCHED, DELETED, UNLINKED, LINKED, DISPUTED
        assertThat(reactions.getReaction()).hasSize(5);

        var unmatched = findReaction(reactions, SynchronizationSituationType.UNMATCHED);
        assertThat(unmatched.getActions().getInactivateResourceObject()).hasSize(1);
        assertThat(unmatched.getActions().getInactivateResourceObject().get(0).getOrder()).isEqualTo(1);
        assertThat(unmatched.getActions().getDeleteResourceObject()).hasSize(1);
        assertThat(unmatched.getActions().getDeleteResourceObject().get(0).getOrder()).isEqualTo(2);

        var deleted = findReaction(reactions, SynchronizationSituationType.DELETED);
        assertThat(deleted.getActions().getSynchronize()).hasSize(1);
        assertThat(deleted.getActions().getSynchronize().get(0).getOrder()).isEqualTo(1);

        var unlinked = findReaction(reactions, SynchronizationSituationType.UNLINKED);
        assertThat(unlinked.getActions().getLink()).hasSize(1);
        assertThat(unlinked.getActions().getLink().get(0).getOrder()).isEqualTo(1);

        var linked = findReaction(reactions, SynchronizationSituationType.LINKED);
        assertThat(linked.getActions().getSynchronize()).hasSize(1);
        assertThat(linked.getActions().getSynchronize().get(0).getOrder()).isEqualTo(1);

        var disputed = findReaction(reactions, SynchronizationSituationType.DISPUTED);
        assertThat(disputed.getActions().getCreateCorrelationCase()).hasSize(1);
        assertThat(disputed.getActions().getCreateCorrelationCase().get(0).getOrder()).isEqualTo(1);
        assertNoOtherActions(disputed.getActions(), Set.of("createCorrelationCase"));
    }

    @Test
    public void test520PredefinedReactionsForTargetWithoutCorrelationCase() {
        var reactions = smartIntegrationService.getPredefinedSynchronizationReactions(
                SynchronizationConfigurationScenario.TARGET_WITH_FEEDBACK, false);

        // Expect UNMATCHED, DELETED, UNLINKED, LINKED (no DISPUTED)
        assertThat(reactions.getReaction()).hasSize(4);
        assertThat(reactions.getReaction().stream()
                .noneMatch(r -> r.getSituation() != null && r.getSituation().contains(SynchronizationSituationType.DISPUTED))).isTrue();
    }

    private SynchronizationReactionType findReaction(
            SynchronizationReactionsType reactions, SynchronizationSituationType situation) {
        return reactions.getReaction().stream()
                .filter(r -> r.getSituation() != null && r.getSituation().contains(situation))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No reaction for situation " + situation));
    }

    private void assertNoOtherActions(SynchronizationActionsType actions, Set<String> allowed) {
        // Verify that only expected action groups are populated
        if (!allowed.contains("addFocus")) assertThat(actions.getAddFocus()).isEmpty();
        if (!allowed.contains("deleteFocus")) assertThat(actions.getDeleteFocus()).isEmpty();
        if (!allowed.contains("inactivateFocus")) assertThat(actions.getInactivateFocus()).isEmpty();
        if (!allowed.contains("synchronize")) assertThat(actions.getSynchronize()).isEmpty();
        if (!allowed.contains("link")) assertThat(actions.getLink()).isEmpty();
        if (!allowed.contains("deleteResourceObject")) assertThat(actions.getDeleteResourceObject()).isEmpty();
        if (!allowed.contains("inactivateResourceObject")) assertThat(actions.getInactivateResourceObject()).isEmpty();
        if (!allowed.contains("createCorrelationCase")) assertThat(actions.getCreateCorrelationCase()).isEmpty();
    }
}
