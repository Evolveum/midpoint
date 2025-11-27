/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.assertj.core.api.ThrowingConsumer;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = "classpath:ctx-model-test-main.xml")
public class CorrelationDefinitionUtilTest extends AbstractEmptyInternalModelTest {

    @Test
    void targetNullButSourceNotNull_mergeDefinitions_resultShouldBeEqualToSource() throws SchemaException,
            ConfigurationException {
        final CorrelationDefinitionType source = createCorrelationDefinition("source", "item");
        final CorrelationDefinitionType result = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(null, source);
        assertEquals(result, source);
    }

    @Test
    void sourceNullButTargetNotNull_mergeDefinitions_resultShouldBeEqualToTarget() throws SchemaException,
            ConfigurationException {
        final CorrelationDefinitionType target = createCorrelationDefinition("target", "item");
        final CorrelationDefinitionType result = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(target, null);
        assertEquals(result, target);
    }

    @Test
    void targetHasNoThresholdsAndSourceHasThresholds_mergeDefinitions_resultShouldHaveThresholdsFromSource()
            throws SchemaException, ConfigurationException {
        final CorrelationDefinitionType target = new CorrelationDefinitionType();
        final CorrelationDefinitionType source = new CorrelationDefinitionType()
                .thresholds(new CorrelationConfidenceThresholdsDefinitionType().definite(0.9));

        final CorrelationDefinitionType result = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(target, source);

        assertNotNull(result);
        assertNotNull(result.getThresholds());
        assertEquals(result.getThresholds().getDefinite(), 0.9);
    }

    @Test
    void bothDefinitionsHaveThresholds_mergeDefinitions_resultShouldHaveThresholdFromTarget() throws SchemaException,
            ConfigurationException {
        final CorrelationDefinitionType target = new CorrelationDefinitionType()
                .thresholds(new CorrelationConfidenceThresholdsDefinitionType().definite(0.8));
        final CorrelationDefinitionType source = new CorrelationDefinitionType()
                .thresholds(new CorrelationConfidenceThresholdsDefinitionType().definite(0.9));

        final CorrelationDefinitionType result = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(target, source);

        assertNotNull(result);
        assertNotNull(result.getThresholds());
        assertEquals(result.getThresholds().getDefinite(), 0.8);
    }

    @Test
    void bothDefinitionsArePresent_mergeDefinitions_targetDefinitionInstanceShouldNotBeModifiedDirectly()
            throws SchemaException, ConfigurationException {
        final CorrelationDefinitionType target = createCorrelationDefinition("target", "item");
        final CorrelationDefinitionType source = createCorrelationDefinition("source", "item");

        final CorrelationDefinitionType result = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(target, source);

        // Original target should not be modified
        assertEquals(target.getCorrelators().getItems().size(), 1);
        assertEquals(target.getCorrelators().getItems().get(0).getName(), "target");

        // Merged result should have both
        assertEquals(result.getCorrelators().getItems().size(), 2);
    }

    @Test
    void targetAndSourceContainDifferentCorrelators_mergeDefinitions_resultShouldContainBothCorrelators()
            throws SchemaException, ConfigurationException {
        final CorrelationDefinitionType targetDefinition = createCorrelationDefinition("first", "firstItem");
        final CorrelationDefinitionType sourceDefinition = createCorrelationDefinition("second", "secondItem");

        final CorrelationDefinitionType correlationDefinition = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(
                targetDefinition, sourceDefinition);

        assertNotNull(correlationDefinition);
        final List<ItemsSubCorrelatorType> subItems = correlationDefinition.getCorrelators().getItems();
        assertThat(subItems).hasSize(2)
                .extracting(ItemsSubCorrelatorType::getName)
                .containsExactlyInAnyOrder("first", "second");

        assertThat(subItems).filteredOn(item -> "first".equals(item.getName()))
                .singleElement()
                .satisfies(assertSingleItem("firstItem"));

        assertThat(subItems)
                .filteredOn(item -> "second".equals(item.getName()))
                .singleElement()
                .satisfies(assertSingleItem("secondItem"));
    }

    @Test
    void targetAndSourceHasSameCorrelatorButDifferentItems_mergeDefinitions_resultShouldContainCorrelatorWithBothItems()
            throws SchemaException, ConfigurationException {
        final CorrelationDefinitionType targetDefinition = createCorrelationDefinition("correlator", "firstItem");
        final CorrelationDefinitionType sourceDefinition = createCorrelationDefinition("correlator", "secondItem");

        final CorrelationDefinitionType correlationDefinition = CorrelatorsDefinitionUtil.mergeCorrelationDefinitions(
                targetDefinition, sourceDefinition);

        assertNotNull(correlationDefinition);
        final List<ItemsSubCorrelatorType> subItems = correlationDefinition.getCorrelators().getItems();
        assertThat(subItems).singleElement()
                .satisfies(item -> assertThat(item.getName()).isEqualTo("correlator"))
                .extracting(ItemsCorrelatorType::getItem).asInstanceOf(list(CorrelationItemType.class))
                .hasSize(2)
                .extracting(CorrelationItemType::getName)
                .contains("firstItem", "secondItem");
    }

    private static @NotNull ThrowingConsumer<ItemsSubCorrelatorType> assertSingleItem(String itemName) {
        return item -> {
            final List<CorrelationItemType> secondItems = item.getItem();
            assertThat(secondItems).singleElement()
                    .extracting(CorrelationItemType::getName)
                    .isEqualTo(itemName);
        };
    }

    private static CorrelationDefinitionType createCorrelationDefinition(String correlatorName, String itemName) {
        final CompositeCorrelatorType correlators = new CompositeCorrelatorType()
                .items(new ItemsSubCorrelatorType()
                        .name(correlatorName)
                        .item(new CorrelationItemType().name(itemName)));
        return new CorrelationDefinitionType()
                .correlators(correlators);
    }

}