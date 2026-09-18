/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPartsCombiner;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPartsWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public final class VisualizationBasedDeltaFormatter implements DeltaFormatter {

    private static final Trace LOGGER = TraceManager.getTrace(VisualizationBasedDeltaFormatter.class);

    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final PropertiesFormatter<VisualizationItem> additionalIdentificationFormatter;
    private final PropertiesFormatter<VisualizationDeltaItem> containerPropertiesModificationFormatter;
    private final IndentationGenerator indentationGenerator;
    private final LocalizationService localizationService;

    public VisualizationBasedDeltaFormatter(PropertiesFormatter<VisualizationItem> propertiesFormatter,
            PropertiesFormatter<VisualizationItem> additionalIdentificationFormatter,
            PropertiesFormatter<VisualizationDeltaItem> containerPropertiesModificationFormatter,
            IndentationGenerator indentationGenerator,
            LocalizationService localizationService) {
        this.propertiesFormatter = propertiesFormatter;
        this.additionalIdentificationFormatter = additionalIdentificationFormatter;
        this.containerPropertiesModificationFormatter = containerPropertiesModificationFormatter;
        this.indentationGenerator = indentationGenerator;
        this.localizationService = localizationService;
    }

    @Override
    public String formatVisualization(Visualization visualization) {
        return formatVisualization(visualization, new FormattingContext(this.localizationService.getDefaultLocale()));
    }

    @Override
    public String formatVisualization(Visualization visualization, FormattingContext context) {
        LOGGER.trace("Starting to format visualization {}", getObjectName(visualization.getName(), context));
        var formatingResult = formatContainer(visualization, 0, context);
        LOGGER.trace("Visualization formating ends up with a result: {}", formatingResult);
        return formatingResult;
    }

    private String formatContainer(Visualization visualization, int nestingLevel, FormattingContext context) {
        final int nextNestingLevel = nestingLevel + 1;
        final StringBuilder formattedContainer = new StringBuilder(this.indentationGenerator.indentation(nestingLevel))
                .append(createHeading(visualization, context));
        final String formattedProperties = formatProperties(visualization, nextNestingLevel, context);
        if (!formattedProperties.isEmpty()) {
                formattedContainer.append(":\n")
                        .append(formattedProperties);
        } else if (!visualization.getPartialVisualizations().isEmpty()) {
            formattedContainer.append(":");
        }

        for (final Visualization partialVisualization : visualization.getPartialVisualizations()) {
            formattedContainer.append("\n").append(formatContainer(partialVisualization, nextNestingLevel, context));
        }
        return formattedContainer.toString();
    }

    private String formatProperties(Visualization visualization, int nestingLevel, FormattingContext context) {

        return switch (visualization.getChangeType()) {
            case ADD, DELETE -> {
                final List<VisualizationItem> properties = new ArrayList<>(visualization.getItems());
                yield this.propertiesFormatter.formatProperties(properties, nestingLevel, context);
            }
            case MODIFY -> {
                final List<VisualizationItem> items = new ArrayList<>();
                final List<VisualizationDeltaItem> deltaItems = new ArrayList<>();
                for (final VisualizationItem item : visualization.getItems()) {
                    // FIXME This is a workaround to handle additional identification properties. Simply speaking, we
                    //  can not currently rely on the isDescriptive method in the item, because there is a bug
                    //  MID-10620. This workaround does not cover all cases of "additional identification" properties.
                    if (item instanceof VisualizationDeltaItem deltaItem) {
                        deltaItems.add(deltaItem);
                    } else {
                        items.add(item);
                    }
                }
                // Items, which in the "MODIFY" case are not "delta" items, are most likely additional identification
                // (akka descriptive) properties.
                final String additionalIdentification = this.additionalIdentificationFormatter.formatProperties(items,
                        nestingLevel, context);
                final String containerProperties = this.containerPropertiesModificationFormatter.formatProperties(
                        deltaItems, nestingLevel, context);
                yield concatenateNonEmptyStrings("\n", additionalIdentification, containerProperties);
            }
        };
    }

    private String createHeading(Visualization visualization, FormattingContext formattingContext) {

        final var customizableOverview = visualization.getName().getCustomizableOverview();

        if (customizableOverview != null) {
            final var customizableHeading = customizableOverview.wrap(localizationPartsWrapper())
                            .combineParts(LocalizationPartsCombiner.joiningWithSpaceIfNotEmpty());

            final var englishContext =
                    new FormattingContext(Locale.ENGLISH);

            final String englishHeading =
                    customizableHeading.translate(
                            this.localizationService,
                            englishContext.locale());

            /*
             * Simple ADD/DELETE headings equivalent to the generic formatter
             * heading are safe to translate. More complex visualizer messages
             * are kept in English to avoid partially localized sentences.
             */
            if ((visualization.getChangeType() == ChangeType.ADD
                    || visualization.getChangeType() == ChangeType.DELETE)
                    && englishHeading.equals(
                    createActionHeading(visualization, englishContext))) {

                return customizableHeading.translate(
                        this.localizationService,
                        formattingContext.locale());
            }

            return englishHeading;
        }

        return switch (visualization.getChangeType()) {
            case ADD, DELETE -> createActionHeading(visualization, formattingContext);
            case MODIFY -> createModificationHeading(visualization, formattingContext);
        };
    }

    private LocalizationPartsWrapper<String, LocalizationCustomizationContext, String>
    localizationPartsWrapper() {

        return LocalizationPartsWrapper.from(
                (String object, LocalizationCustomizationContext context) -> object,
                (String objectName, LocalizationCustomizationContext context)
                        -> "\"" + objectName + "\"",
                (String action, LocalizationCustomizationContext context) -> action,
                (String additionalInfo, LocalizationCustomizationContext context)
                        -> additionalInfo,
                (String helpingWords) -> helpingWords);
    }

    private String createModificationHeading(Visualization visualization, FormattingContext context) {
        final String objectName = encloseIfNotEmpty(getObjectName(visualization.getName(), context), "\"", "\"");
        final String objectType = getObjectType(visualization, context);
        final String defaultMessage = concatenateNonEmptyStrings(" ", objectType, objectName, "was modified");

        return this.localizationService.translate(
                "VisualizationBasedDeltaFormatter.objectWasModified",
                new Object[] { objectType, objectName }, context.locale(),
                defaultMessage).trim();
    }

    private String createActionHeading(Visualization visualization, FormattingContext context) {
        final String changeLocalizationKey = enumLocalizationKey(visualization.getChangeType());
        final String changeType = this.localizationService.translate(changeLocalizationKey, new Object[0], context.locale());
        final String objectName = encloseIfNotEmpty(getObjectName(visualization.getName(), context), "\"", "\"");
        final String objectType = getObjectType(visualization, context);

        return concatenateNonEmptyStrings(" ", changeType, objectType, objectName);
    }

    private String getObjectName(Name objectName, FormattingContext context) {
        if (objectName.getDisplayName() == null) {
            return this.localizationService.translate(objectName.getSimpleName(), context.locale());
        }

        final String displayName = emptyIfNull(this.localizationService.translate(objectName.getDisplayName(),
                context.locale()));
        if (objectName.getSimpleName() == null) {
            return displayName;
        }

        final String simpleName = emptyIfNull(this.localizationService.translate(objectName.getSimpleName(),
                context.locale()));
        if (displayName.equalsIgnoreCase(simpleName)) {
            return displayName;
        }
        return displayName + encloseIfNotEmpty(simpleName, " (", ")");
    }

    private String getObjectType(Visualization visualization, FormattingContext context) {
        if (visualization.getOwner() != null) {
            // This means visualization is not top level, thus the change is on container. For this scenario I am not
            // sure how to retrieve translated object type right now.
            return "";
        }

        final PrismContainerDefinition<?> definition = visualization.getSourceDefinition();
        if (definition == null) {
            throw new IllegalStateException(
                    "Definition of focal object is not present. Unable to properly format object type.");
        }
        final String typeKey = SchemaConstants.OBJECT_TYPE_KEY_PREFIX + definition.getTypeName().getLocalPart();
        return emptyIfNull(this.localizationService.translate(typeKey, new Object[0], context.locale()));
    }

    private static String enumLocalizationKey(Enum<?> enumValue) {
        return enumValue == null ? "" : enumValue.getClass().getSimpleName() + "." + enumValue.name();
    }

    private static String concatenateNonEmptyStrings(String joiner, String... values) {
        if (values.length == 2) {
            if (!values[0].isEmpty() && !values[1].isEmpty()) {
                return values[0] + joiner + values[1];
            } else if (values[0].isEmpty())  {
                return values[1];
            } else {
                return values[0];
            }
        }
        return Stream.of(values)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining(joiner));
    }

    private static String encloseIfNotEmpty(String value, String prefix, String suffix) {
        if (value.isEmpty()) {
            return value;
        }
        return prefix + value + suffix;
    }

}
