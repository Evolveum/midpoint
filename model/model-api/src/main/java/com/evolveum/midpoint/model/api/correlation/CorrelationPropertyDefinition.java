package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Contains information about a correlation property that is to be (e.g.) displayed in the correlation case view.
 */
public class CorrelationPropertyDefinition implements Serializable, DebugDumpable {

    /** The "technical" name. */
    @NotNull private final String name;

    /** Path within the focus object. */
    @NotNull private final ItemPath itemPath;

    /** Definition in the focus object. */
    @Nullable private final ItemDefinition<?> definition;

    private CorrelationPropertyDefinition(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @Nullable ItemDefinition<?> definition) {
        this.name = name;
        this.itemPath = itemPath;
        this.definition = definition;
    }

    public static @NotNull CorrelationPropertyDefinition fromConfiguration(
            @NotNull CorrelationItemType itemBean, @Nullable ComplexTypeDefinition complexTypeDefinition)
            throws ConfigurationException {
        String name = getName(itemBean);
        ItemPath rawPath = getPath(itemBean);
        if (complexTypeDefinition != null) {
            var resolvedPath = ResolvedItemPath.create(complexTypeDefinition, rawPath);
            if (resolvedPath.isComplete()) {
                return new CorrelationPropertyDefinition(name, resolvedPath.getResolvedPath(), resolvedPath.getLastDefinition());
            }
        }
        return new CorrelationPropertyDefinition(name, rawPath, null); // fallback
    }

    public static @NotNull CorrelationPropertyDefinition fromData(
            @NotNull ItemPath path, @NotNull PrismProperty<?> property) {
        ItemName lastName =
                MiscUtil.requireNonNull(path.lastName(), () -> new IllegalArgumentException("Path has no last name: " + path));
        return new CorrelationPropertyDefinition(lastName.getLocalPart(), path, property.getDefinition());
    }

    private static @NotNull String getName(CorrelationItemType itemBean) throws ConfigurationException {
        String explicitName = itemBean.getName();
        if (explicitName != null) {
            return explicitName;
        }
        ItemPathType pathBean = itemBean.getRef();
        if (pathBean != null) {
            ItemName lastName = pathBean.getItemPath().lastName();
            if (lastName != null) {
                return lastName.getLocalPart();
            }
        }
        throw new ConfigurationException(
                "Couldn't determine name for correlation item: no name nor path in " + itemBean);
    }

    private static @NotNull ItemPath getPath(@NotNull CorrelationItemType itemBean) throws ConfigurationException {
        ItemPathType specifiedPath = itemBean.getRef();
        if (specifiedPath != null) {
            return specifiedPath.getItemPath();
        } else {
            throw new ConfigurationException("No path for " + itemBean);
        }
    }

    public @NotNull ItemPath getItemPath() {
        return itemPath;
    }

    public @NotNull ItemPath getSecondaryPath() {
        return SchemaConstants.PATH_FOCUS_IDENTITY.append(FocusIdentityType.F_DATA, itemPath);
    }

    public @Nullable ItemDefinition<?> getDefinition() {
        return definition;
    }

    public @NotNull String getDisplayName() {
        if (definition != null) {
            if (definition.getDisplayName() != null) {
                return definition.getDisplayName();
            } else {
                return definition.getItemName().getLocalPart();
            }
        } else {
            return name;
        }
    }

    public Integer getDisplayOrder() {
        return definition != null ? definition.getDisplayOrder() : null;
    }

    public @NotNull String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "itemPath=" + itemPath +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "itemPath", String.valueOf(itemPath), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "definition", String.valueOf(definition), indent + 1);
        return sb.toString();
    }
}
