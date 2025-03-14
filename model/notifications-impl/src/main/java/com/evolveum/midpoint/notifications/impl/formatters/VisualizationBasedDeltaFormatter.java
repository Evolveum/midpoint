package com.evolveum.midpoint.notifications.impl.formatters;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    public String formatObjectModificationDelta(@NotNull ObjectDelta<? extends Objectable> objectDelta,
            Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes, PrismObject<?> objectOld,
            PrismObject<?> objectNew, Task task, OperationResult result) {
        // FIXME Solve somehow the differences between API signatures of hiddenPaths parameter (Collection vs List)
        return formatObjectModificationDelta(objectDelta, new ArrayList<>(hiddenPaths), showOperationalAttributes,
                task, result);
    }

    @Override
    public String formatObjectAdd(PrismObject<? extends ObjectType> object, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes,
            Task task, OperationResult result) {
        final Visualization visualization;
        try {
            visualization = this.visualizer.visualize(object, task, result);
        } catch (SchemaException | ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }

        return formatContainer(visualization, 0);
    }

    @Override
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta,
            List<ItemPath> hiddenPaths, boolean showOperationalAttributes, Task task, OperationResult result) {
        // FIXME Change signature in visualizer to take deltas of Objectable instaed of deltas of ObjectType
        var delta = (ObjectDelta<ObjectType>) objectDelta;

        final Visualization visualization;
        try {
            visualization = this.visualizer.visualizeDelta(delta, task, result);
        } catch (SchemaException | ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }

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
                // FIXME This is workaround for the fact, that `getItems` returns
                //  `List<? extends VisualizationItem>`
                final ArrayList<VisualizationItem> items = new ArrayList<>(visualization.getItems());
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

        final PrismContainerDefinition<?> definition = visualization.getSourceDefinition();
        if (definition == null) {
            throw new IllegalStateException(
                    "Definition of added focal object is not present. Unable to properly format object addition.");
        }
        final String typeKey = SchemaConstants.OBJECT_TYPE_KEY_PREFIX + definition.getTypeName().getLocalPart();
        final String objectType = emptyIfNull(this.localizationService.translate(typeKey, new Object[0],
                this.defaultLocale));

        return switch (visualization.getChangeType()) {
            case ADD, DELETE -> createAddOrDeleteHeading(visualization, objectType);
            case MODIFY -> createModificationHeading(visualization, objectType);
        };
    }

    private String createModificationHeading(Visualization visualization, String objectType) {
        // TODO translate the "has been modified" suffix.
        String objectName = getObjectName(visualization.getName());
        if (!objectName.isEmpty()) {
            objectName = " \"" + objectName + "\"";
        }
        return objectType + objectName + " has been modified";
    }

    private String createAddOrDeleteHeading(Visualization visualization, String objectType) {

        final String changeLocalizationKey = enumLocalizationKey(visualization.getChangeType());
        final String changeType = this.localizationService.translate(changeLocalizationKey, new Object[0],
                this.defaultLocale);
        String objectName = getObjectName(visualization.getName());
        if (objectName.isEmpty()) {
            objectName = "";
        } else {
            objectName = "\"" + objectName + "\"";
        }
        return Stream.of(changeType, objectType, objectName)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining(" "));
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
        return displayName + " (" + simpleName + ")";
    }

    private static String enumLocalizationKey(Enum<?> enumValue) {
        return enumValue == null ? "" : enumValue.getClass().getSimpleName() + "." + enumValue.name();
    }

}
