/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.model.api.validator.Issue;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
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
		QName defaultVariable;

		public PathResolutionContext(@NotNull PrismContext prismContext, QName defaultVariable) {
			this.prismContext = prismContext;
			this.defaultVariable = defaultVariable;
		}
	}

	public static class ResourceResolutionContext extends PathResolutionContext {
		@NotNull ResourceType resource;
		@NotNull ShadowKindType kind;
		@NotNull String intent;

		public ResourceResolutionContext(@NotNull PrismContext prismContext, QName defaultVariable, @NotNull ResourceType resource,
				@NotNull ShadowKindType kind, @NotNull String intent) {
			super(prismContext, defaultVariable);
			this.resource = resource;
			this.kind = kind;
			this.intent = intent;
		}
	}

	@Nullable		// null means not supported yet
	@SuppressWarnings("unchecked")
	public static PathResolutionResult resolvePath(@NotNull ItemPath path, @NotNull PathResolutionContext context) {
		if (!(path.first() instanceof NameItemPathSegment)) {
			return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_DOES_NOT_START_WITH_NAME,
							"Path does not start with a name: '" + path + "'", null, null));
		}
		QName varName;
		ItemPath itemPath;
		NameItemPathSegment firstNameSegment = (NameItemPathSegment) path.first();
		if (firstNameSegment.isVariable()) {
			varName = firstNameSegment.getName();
			itemPath = path.tail();
		} else {
			if (context.defaultVariable == null) {
				return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_NO_DEFAULT_VARIABLE,
								"No default variable for item path: '" + path + "'", null, null));
			}
			varName = context.defaultVariable;
			itemPath = path;
		}

		if (QNameUtil.match(ExpressionConstants.VAR_ACCOUNT, varName)) {
			if (!(context instanceof ResourceResolutionContext)) {
				return new PathResolutionResult(new Issue(Issue.Severity.WARNING, CAT_ITEM_PATH, C_ILLEGAL_USE_OF_ACCOUNT_VARIABLE,
						"Illegal use of 'account' variable: '" + path + "'", null, null));
			} else {
				// TODO implement checking of $account-based paths
				return null;
			}
		} else if (QNameUtil.match(ExpressionConstants.VAR_USER, varName) || QNameUtil.match(ExpressionConstants.VAR_FOCUS, varName)) {
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
					clazz = UserType.class;		// the default (MID-3307)
				}
			}
			return resolvePathForType(clazz, itemPath, context);
		} else if (QNameUtil.match(ExpressionConstants.VAR_ACTOR, varName)) {
			return resolvePathForType(UserType.class, itemPath, context);
		} else if (QNameUtil.match(ExpressionConstants.VAR_INPUT, varName)) {
			return null;
		} else {
			return null;		// TODO list all possible variables here and issue a warning if no one matches
		}
	}

	@Nullable		// null means not supported yet
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
						"No definition for '" + path + "' in " + def.getName().getLocalPart(), null, null));
			}
		}
	}
}
