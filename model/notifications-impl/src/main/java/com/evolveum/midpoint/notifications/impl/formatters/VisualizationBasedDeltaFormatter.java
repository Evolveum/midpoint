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
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.LocalizableMessage;

@Component
public class VisualizationBasedDeltaFormatter implements IDeltaFormatter {
    private final Visualizer visualizer;
    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final PropertiesFormatter<VisualizationDeltaItem> containerPropertiesModificationFormatter;
    private final IndentationGenerator indentationGenerator;
    private final LocalizationService localizationService;
    private final Locale defaultLocale;

    public VisualizationBasedDeltaFormatter(Visualizer visualizer,
            PropertiesFormatter<VisualizationItem> propertiesFormatter,
            PropertiesFormatter<VisualizationDeltaItem> containerPropertiesModificationFormatter,
            IndentationGenerator indentationGenerator,
            LocalizationService localizationService) {
        this.visualizer = visualizer;
        this.propertiesFormatter = propertiesFormatter;
        this.containerPropertiesModificationFormatter = containerPropertiesModificationFormatter;
        this.indentationGenerator = indentationGenerator;
        this.localizationService = localizationService;
        this.defaultLocale = Locale.getDefault();
    }

    @Autowired
    // FIXME This is temporary constructor used during development. Remove it before production.
    public VisualizationBasedDeltaFormatter(Visualizer visualizer, LocalizationService localizationService) {
        this.visualizer = visualizer;
        this.localizationService = localizationService;
        this.indentationGenerator = new IndentationGenerator("|", "\t");
        this.defaultLocale = Locale.getDefault();
        final PropertyFormatter propertyFormatter = new PropertyFormatter(this.localizationService, " ", "\n");
        this.propertiesFormatter = new PlainTextPropertiesFormatter(this.indentationGenerator, propertyFormatter);
        this.containerPropertiesModificationFormatter =
                new ContainerPropertiesModificationFormatter(this.localizationService, this.propertiesFormatter,
                        this.indentationGenerator, new ModifiedPropertiesFormatter(propertyFormatter,
                        this.indentationGenerator));
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
                // FIXME `formatProperties` takes in this case `Collection<VisualizationItem>` as parameter, but
                //  `getItems` returns `List<? extends VisualizationItem>`
                final List<VisualizationItem> items = new ArrayList<>(visualization.getItems());
                yield this.propertiesFormatter.formatProperties(items, nestingLevel);
            }
            case MODIFY -> {
                // FIXME This is workaround for the fact, that `getItems` returns
                //  `List<? extends VisualizationItem>`
                final List<VisualizationDeltaItem> items = (List<VisualizationDeltaItem>) visualization.getItems();
                yield this.containerPropertiesModificationFormatter.formatProperties(items, nestingLevel);
            }
        };
    }

    private String createHeading(Visualization visualization) {
        final LocalizableMessage overview = visualization.getName().getOverview();
        if (overview != null) {
            return this.localizationService.translate(overview, this.defaultLocale);
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
        return concatenateNonEmptyStrings(objectType, objectName, "has been modified");
    }

    private String createAddOrDeleteHeading(Visualization visualization) {
        final String changeLocalizationKey = enumLocalizationKey(visualization.getChangeType());
        final String changeType = this.localizationService.translate(changeLocalizationKey, new Object[0],
                this.defaultLocale);
        final String objectName = encloseIfNotEmpty(getObjectName(visualization.getName()), "\"", "\"");
        final String objectType = getObjectType(visualization);

        return concatenateNonEmptyStrings(changeType, objectType, objectName);
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

    private static String concatenateNonEmptyStrings(String... values) {
        return Stream.of(values)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining(" "));
    }

    private static String encloseIfNotEmpty(String value, String prefix, String suffix) {
        if (value.isEmpty()) {
            return value;
        }
        return prefix + value + suffix;
    }

}
