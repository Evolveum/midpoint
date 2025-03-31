package com.evolveum.midpoint.notifications.impl.formatters;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPartsCombiner;
import com.evolveum.midpoint.model.api.visualizer.localization.LocalizationPartsWrapper;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

@Component
public class VisualizationBasedDeltaFormatter implements IDeltaFormatter {
    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final PropertiesFormatter<VisualizationItem> additionalIdentificationFormatter;
    private final PropertiesFormatter<VisualizationDeltaItem> containerPropertiesModificationFormatter;
    private final IndentationGenerator indentationGenerator;
    private final LocalizationService localizationService;
    private final Locale defaultLocale;

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
        this.defaultLocale = Locale.getDefault();
    }

    @Autowired
    // FIXME This is temporary constructor used during development. Remove it before production.
    public VisualizationBasedDeltaFormatter(Visualizer visualizer, LocalizationService localizationService) {
        this.localizationService = localizationService;
        this.indentationGenerator = new IndentationGenerator("|", "\t");
        this.defaultLocale = Locale.getDefault();
        final PropertyFormatter propertyFormatter = new PropertyFormatter(this.localizationService, " ", "\n");
        this.propertiesFormatter = new PlainTextPropertiesFormatter(this.indentationGenerator, propertyFormatter);
        this.additionalIdentificationFormatter = new AdditionalIdentificationFormatter(this.propertiesFormatter,
                this.indentationGenerator);
        this.containerPropertiesModificationFormatter = new ContainerPropertiesModificationFormatter(
                this.propertiesFormatter, this.indentationGenerator,
                new ModifiedPropertiesFormatter(propertyFormatter, this.indentationGenerator));
    }

    @Override
    public String formatVisualization(Visualization visualization) {
        return formatContainer(visualization, 0);
    }

    private String formatContainer(Visualization visualization, int nestingLevel) {
        final int nextNestingLevel = nestingLevel + 1;
        final StringBuilder formattedContainer = new StringBuilder(this.indentationGenerator.indentation(nestingLevel))
                .append(createHeading(visualization));
        final String formattedProperties = formatProperties(visualization, nextNestingLevel);
        if (!formattedProperties.isEmpty()) {
                formattedContainer.append(":\n")
                        .append(formattedProperties);
        } else if (!visualization.getPartialVisualizations().isEmpty()) {
            formattedContainer.append(":");
        }

        for (final Visualization partialVisualization : visualization.getPartialVisualizations()) {
            formattedContainer.append("\n").append(formatContainer(partialVisualization, nextNestingLevel));
        }
        return formattedContainer.toString();
    }

    private String formatProperties(Visualization visualization, int nestingLevel) {

        return switch (visualization.getChangeType()) {
            case ADD, DELETE -> {
                final List<VisualizationItem> properties = new ArrayList<>(visualization.getItems());
                yield this.propertiesFormatter.formatProperties(properties, nestingLevel);
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
                        nestingLevel);
                final String containerProperties = this.containerPropertiesModificationFormatter.formatProperties(
                        deltaItems, nestingLevel);
                yield concatenateNonEmptyStrings("\n", additionalIdentification, containerProperties);
            }
        };
    }

    private String createHeading(Visualization visualization) {
        final var customizableOverview = visualization.getName().getCustomizableOverview();
        if (customizableOverview != null) {
            return customizableOverview.wrap(
                    LocalizationPartsWrapper.from(
                            (object, context) -> object,
                            (objectName, context) -> "\"" + objectName + "\"",
                            (action, context) -> action,
                            (additionalInfo, context) -> additionalInfo,
                            helpingWords -> helpingWords))
                    .combineParts(LocalizationPartsCombiner.joiningWithSpaceIfNotEmpty())
                    .translate(this.localizationService, this.defaultLocale);
        }

        return switch (visualization.getChangeType()) {
            case ADD, DELETE -> createAddOrDeleteHeading(visualization);
            case MODIFY -> createModificationHeading(visualization);
        };
    }

    private String createModificationHeading(Visualization visualization) {
        final String objectName = encloseIfNotEmpty(getObjectName(visualization.getName()), "\"", "\"");
        final String objectType = getObjectType(visualization);

        // TODO translate the "has been modified" suffix.
        return concatenateNonEmptyStrings(" ", objectType, objectName, "has been modified");
    }

    private String createAddOrDeleteHeading(Visualization visualization) {
        final String changeLocalizationKey = enumLocalizationKey(visualization.getChangeType());
        final String changeType = this.localizationService.translate(changeLocalizationKey, new Object[0],
                this.defaultLocale);
        final String objectName = encloseIfNotEmpty(getObjectName(visualization.getName()), "\"", "\"");
        final String objectType = getObjectType(visualization);

        return concatenateNonEmptyStrings(" ", changeType, objectType, objectName);
    }

    private String getObjectName(Name objectName) {
        if (objectName.getDisplayName() == null) {
            return this.localizationService.translate(objectName.getSimpleName(), this.defaultLocale);
        }

        final String displayName = emptyIfNull(this.localizationService.translate(objectName.getDisplayName(),
                this.defaultLocale));
        if (objectName.getSimpleName() == null) {
            return displayName;
        }

        final String simpleName = emptyIfNull(this.localizationService.translate(objectName.getSimpleName(),
                this.defaultLocale));
        if (displayName.equalsIgnoreCase(simpleName)) {
            return displayName;
        }
        return displayName + encloseIfNotEmpty(simpleName, " (", ")");
    }

    private String getObjectType(Visualization visualization) {
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
        return emptyIfNull(this.localizationService.translate(typeKey, new Object[0], this.defaultLocale));
    }

    private static String enumLocalizationKey(Enum<?> enumValue) {
        return enumValue == null ? "" : enumValue.getClass().getSimpleName() + "." + enumValue.name();
    }

    private static String concatenateNonEmptyStrings(String joiner, String... values) {
        if (values.length == 2) {
            return values[0].isEmpty() ? values[1] : values[0] + joiner + values[1];
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
