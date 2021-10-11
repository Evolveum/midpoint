/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.model.api.validator.Issue;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class DataModelUtil {

    private static final Trace LOGGER = TraceManager.getTrace(DataModelUtil.class);

    public static final String CAT_ITEM_PATH = "itemPath";
    public static final String C_DOES_NOT_START_WITH_NAME = "itemPath";
    public static final String C_NO_DEFAULT_VARIABLE = "noDefaultVariable";
    public static final String C_ILLEGAL_USE_OF_ACCOUNT_VARIABLE = "cannotUseAccountVariable";
    public static final String C_NO_OBJECT_DEFINITION = "noObjectDefinition";

    public static class PathResolutionResult {
        @NotNull private List<Issue> issues = new ArrayList<>();
        private final ItemDefinition<?> definition;

        public PathResolutionResult(ItemDefinition<?> definition) {
            this.definition = definition;
        }

        public PathResolutionResult(@NotNull Issue issue) {
            this.definition = null;
            issues.add(issue);
        }

        @NotNull
        public List<Issue> getIssues() {
            return issues;
        }

        public ItemDefinition<?> getDefinition() {
            return definition;
        }
    }

    public static class PathResolutionContext {
        @NotNull PrismContext prismContext;
        String defaultVariable;

        public PathResolutionContext(@NotNull PrismContext prismContext, String defaultVariable) {
            this.prismContext = prismContext;
            this.defaultVariable = defaultVariable;
        }
    }

    public static class ResourceResolutionContext extends PathResolutionContext {
        @NotNull ResourceType resource;
        @NotNull ShadowKindType kind;
        @NotNull String intent;

        public ResourceResolutionContext(@NotNull PrismContext prismContext, String defaultVariable, @NotNull ResourceType resource,
                @NotNull ShadowKindType kind, @NotNull String intent) {
            super(prismContext, defaultVariable);
            this.resource = resource;
            this.kind = kind;
            this.intent = intent;
        }
    }

    @Nullable        // null means not supported yet
    @SuppressWarnings("unchecked")
    public static PathResolutionResult resolvePath(@NotNull ItemPath path, @NotNull PathResolutionContext context) {
        if (!path.startsWithName() && !path.startsWithVariable()) {
            return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_DOES_NOT_START_WITH_NAME,
                            "Path does not start with a name nor variable: '" + path + "'", null, null));
        }
        String varName;
        ItemPath itemPath;
        Object first = path.first();
        if (ItemPath.isVariable(first)) {
            varName = ItemPath.toVariableName(first).getLocalPart();
            itemPath = path.rest();
        } else {
            if (context.defaultVariable == null) {
                return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_NO_DEFAULT_VARIABLE,
                                "No default variable for item path: '" + path + "'", null, null));
            }
            varName = context.defaultVariable;
            itemPath = path;
        }

        if (ExpressionConstants.VAR_PROJECTION.equals(varName) || ExpressionConstants.VAR_SHADOW.equals(varName) || ExpressionConstants.VAR_ACCOUNT.equals(varName)) {
            if (!(context instanceof ResourceResolutionContext)) {
                return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_ILLEGAL_USE_OF_ACCOUNT_VARIABLE,
                        "Illegal use of '"+varName+"' variable: '" + path + "'", null, null));
            } else {
                // TODO implement checking of $account-based paths
                return null;
            }
        } else if (ExpressionConstants.VAR_USER.equals(varName) || ExpressionConstants.VAR_FOCUS.equals(varName)) {
            Class<? extends FocusType> clazz = FocusType.class;
            if (context instanceof ResourceResolutionContext) {
                ResourceResolutionContext rctx = (ResourceResolutionContext) context;
                ObjectSynchronizationType def = ResourceTypeUtil.findObjectSynchronization(rctx.resource, rctx.kind, rctx.intent);
                if (def != null) {
                    QName focusType = def.getFocusType() != null ? def.getFocusType() : UserType.COMPLEX_TYPE;
                    PrismObjectDefinition<Objectable> objdef = rctx.prismContext.getSchemaRegistry().findObjectDefinitionByType(focusType);
                    if (objdef != null && objdef.getCompileTimeClass() != null && FocusType.class.isAssignableFrom(objdef.getCompileTimeClass())) {
                        clazz = (Class) objdef.getCompileTimeClass();
                    }
                } else {
                    clazz = UserType.class;        // the default (MID-3307)
                }
            }
            return resolvePathForType(clazz, itemPath, context);
        } else if (ExpressionConstants.VAR_ACTOR.equals(varName)) {
            return resolvePathForType(FocusType.class, itemPath, context);
        } else if (ExpressionConstants.VAR_INPUT.equals(varName)) {
            return null;
        } else {
            return null;        // TODO list all possible variables here and issue a warning if no one matches
        }
    }

    @Nullable        // null means not supported yet
    public static PathResolutionResult resolvePathForType(@NotNull Class<? extends ObjectType> clazz, @NotNull ItemPath path, @NotNull PathResolutionContext context) {
        PrismObjectDefinition<?> def = context.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        if (def == null) {
            return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_NO_OBJECT_DEFINITION,
                    "No definition for " + clazz + " in item path: '" + path + "'", null, null));
        }
        ItemDefinition<?> itemDef = def.findItemDefinition(path);
        if (itemDef != null) {
            return new PathResolutionResult(itemDef);
        } else {
            if (FocusType.class.equals(clazz) && context instanceof ResourceResolutionContext) {
                ResourceResolutionContext rctx = (ResourceResolutionContext) context;
                return new PathResolutionResult(new Issue(Issue.Severity.INFO, CAT_ITEM_PATH, C_NO_OBJECT_DEFINITION,
                        "Couldn't verify item path '" + path + "' because specific focus type (user, role, org, ...) is not defined for kind=" + rctx.kind + ", intent=" + rctx.intent, null, null));
            } else {
                return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_NO_OBJECT_DEFINITION,
                        "No definition for '" + path + "' in " + def.getItemName().getLocalPart(), null, null));
            }
        }
    }
}
