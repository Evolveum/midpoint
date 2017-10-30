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

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
public class EvaluatedPolicyRuleUtil {

	//region --------------- Native evaluated triggers (EvaluatedPolicyRuleTrigger) ---------------------------------------------
	/**
	 * Arranges triggers into trees according to presentation instructions (hidden, final, displayOrder).
	 */
	public static List<TreeNode<EvaluatedPolicyRuleTrigger<?>>> arrangeForPresentationInt(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
		TreeNode<EvaluatedPolicyRuleTrigger<?>> root = new TreeNode<>();
		for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
			arrangeForPresentationInt(root, trigger);
		}
		sortTriggersInt(root);
		return root.getChildren();
	}

	private static void arrangeForPresentationInt(TreeNode<EvaluatedPolicyRuleTrigger<?>> root, EvaluatedPolicyRuleTrigger<?> trigger) {
		PolicyConstraintPresentationType presentation = trigger.getConstraint().getPresentation();
		boolean hidden = isHidden(presentation, trigger.getConstraintKind());
		boolean isFinal = presentation != null && Boolean.TRUE.equals(presentation.isFinal());
		if (!hidden) {
			TreeNode<EvaluatedPolicyRuleTrigger<?>> newNode = new TreeNode<>(trigger);
			root.add(newNode);
			root = newNode;
		}
		if (!isFinal) {
			for (EvaluatedPolicyRuleTrigger<?> innerTrigger : trigger.getInnerTriggers()) {
				arrangeForPresentationInt(root, innerTrigger);
			}
		}
	}

	private static boolean isHidden(PolicyConstraintPresentationType presentation, PolicyConstraintKindType kind) {
		if (presentation != null && presentation.isHidden() != null) {
			return presentation.isHidden();
		} else {
			return isHiddenByDefault(kind);
		}
	}

	private static boolean isHiddenByDefault(PolicyConstraintKindType kind) {
		// consider also "not"
		return kind == PolicyConstraintKindType.TRANSITION || kind == PolicyConstraintKindType.SITUATION
				|| kind == PolicyConstraintKindType.AND || kind == PolicyConstraintKindType.OR;
	}

	private static void sortTriggersInt(TreeNode<EvaluatedPolicyRuleTrigger<?>> node) {
		Comparator<? super TreeNode<EvaluatedPolicyRuleTrigger<?>>> comparator = (t1, t2) -> {
			PolicyConstraintPresentationType p1 = t1.getUserObject().getConstraint().getPresentation();
			PolicyConstraintPresentationType p2 = t2.getUserObject().getConstraint().getPresentation();
			int o1 = p1 != null && p1.getDisplayOrder() != null ? p1.getDisplayOrder() : Integer.MAX_VALUE;
			int o2 = p2 != null && p2.getDisplayOrder() != null ? p2.getDisplayOrder() : Integer.MAX_VALUE;
			return Integer.compare(o1, o2);
		};
		node.getChildren().sort(comparator);
		node.getChildren().forEach(child -> sortTriggersInt(child));
	}

	@NotNull
	public static List<TreeNode<LocalizableMessage>> extractMessages(Collection<EvaluatedPolicyRuleTrigger<?>> triggers, MessageKind kind) {
		return extractMessages(arrangeForPresentationInt(triggers), kind);
	}

	@NotNull
	public static List<TreeNode<LocalizableMessage>> extractMessages(List<TreeNode<EvaluatedPolicyRuleTrigger<?>>> triggerTreeList,
			MessageKind kind) {
		List<TreeNode<LocalizableMessage>> messageTreeList = new ArrayList<>();
		for (TreeNode<EvaluatedPolicyRuleTrigger<?>> tree : triggerTreeList) {
			messageTreeList.add(tree.tranform(trigger -> getMessage(trigger, kind)));
		}
		return messageTreeList;
	}

	private static LocalizableMessage getMessage(EvaluatedPolicyRuleTrigger<?> trigger, MessageKind kind) {
		switch (kind) {
			case NORMAL: return trigger.getMessage();
			case SHORT: return trigger.getMessage();
			default: throw new AssertionError(kind);
		}
	}

	public enum MessageKind { NORMAL, SHORT, /*LONG*/ }
	//endregion

 	//region --------------- Externalized evaluated triggers (EvaluatedPolicyRuleTriggerType) ---------------------------------------------
	/**
	 * Arranges externalized triggers into trees according to presentation instructions (hidden, final, displayOrder).
	 * We support attaching additional data (e.g. highlighting in the GUI case) and additional filtering (e.g. to eliminate redundant triggers).
	 */

	public interface AdditionalData {
	}

	// This is externalized trigger + additional data.
	public static class AugmentedTrigger<AD extends AdditionalData> {
		public final EvaluatedPolicyRuleTriggerType trigger;
		public final AD additionalData;

		public AugmentedTrigger(EvaluatedPolicyRuleTriggerType trigger, AD additionalData) {
			this.trigger = trigger;
			this.additionalData = additionalData;
		}
	}

	// Used e.g. for eliminating redundant (already shown) triggers.
	public interface AdditionalFilter<AD extends AdditionalData> {
		boolean accepts(AugmentedTrigger<AD> newAugmentedTrigger);
	}

	// Main entry point for plain triggers: tree-izes and sorts the triggers.
	public static List<TreeNode<EvaluatedPolicyRuleTriggerType>> arrangeForPresentationExt(List<EvaluatedPolicyRuleTriggerType> triggers) {
		// augment
		List<AugmentedTrigger<AdditionalData>> augmentedTriggers = triggers.stream()
				.map(t -> new AugmentedTrigger<>(t, null))
				.collect(Collectors.toList());
		List<TreeNode<AugmentedTrigger<AdditionalData>>> trees = arrangeForPresentationExt(augmentedTriggers, null);
		// de-augment
		return trees.stream()
				.map(tree -> tree.tranform(at -> at.trigger))
				.collect(Collectors.toList());
	}

	// Main entry point for augmented triggers: tree-izes and sorts the triggers.
	public static <AD extends AdditionalData> List<TreeNode<AugmentedTrigger<AD>>> arrangeForPresentationExt(
			List<AugmentedTrigger<AD>> triggers, AdditionalFilter<AD> additionalFilter) {
		TreeNode<AugmentedTrigger<AD>> root = new TreeNode<>();
		for (AugmentedTrigger<AD> trigger : triggers) {
			arrangeForPresentationExt(root, trigger, additionalFilter);
		}
		sortTriggersExt(root);
		return root.getChildren();
	}

	private static <AD extends AdditionalData> void arrangeForPresentationExt(TreeNode<AugmentedTrigger<AD>> root,
			AugmentedTrigger<AD> trigger, AdditionalFilter<AD> additionalFilter) {
		boolean hidden = isHidden(trigger.trigger);
		boolean isFinal = Boolean.TRUE.equals(trigger.trigger.isFinal());
		if (!hidden) {
			if (additionalFilter != null && !additionalFilter.accepts(trigger)) {
				return;
			}
			TreeNode<AugmentedTrigger<AD>> newNode = new TreeNode<>(trigger);
			root.add(newNode);
			root = newNode;
		}
		if (!isFinal) { // it's possible that this was pre-filtered e.g. by policy enforcer hook
			for (EvaluatedPolicyRuleTriggerType innerTrigger : getChildTriggers(trigger.trigger)) {
				arrangeForPresentationExt(root, new AugmentedTrigger<>(innerTrigger, trigger.additionalData), additionalFilter);
			}
		}
	}

	private static boolean isHidden(EvaluatedPolicyRuleTriggerType trigger) {
		if (trigger.isHidden() != null) {
			return trigger.isHidden();
		} else {
			return isHiddenByDefault(trigger.getConstraintKind());
		}
	}

	private static <AD extends AdditionalData> void sortTriggersExt(TreeNode<AugmentedTrigger<AD>> node) {
		Comparator<? super TreeNode<AugmentedTrigger<AD>>> comparator = (t1, t2) -> {
			int o1 = defaultIfNull(t1.getUserObject().trigger.getPresentationOrder(), Integer.MAX_VALUE);
			int o2 = defaultIfNull(t2.getUserObject().trigger.getPresentationOrder(), Integer.MAX_VALUE);
			return Integer.compare(o1, o2);
		};
		node.getChildren().sort(comparator);
		node.getChildren().forEach(child -> sortTriggersExt(child));
	}

	private static List<EvaluatedPolicyRuleTriggerType> getChildTriggers(EvaluatedPolicyRuleTriggerType trigger) {
		if (trigger instanceof EvaluatedEmbeddingTriggerType) {
			return ((EvaluatedEmbeddingTriggerType) trigger).getEmbedded();
		} else if (trigger instanceof EvaluatedSituationTriggerType) {
			List<EvaluatedPolicyRuleTriggerType> rv = new ArrayList<>();
			for (EvaluatedPolicyRuleType rule : ((EvaluatedSituationTriggerType) trigger).getSourceRule()) {
				rv.addAll(rule.getTrigger());
			}
			return rv;
		} else {
			return Collections.emptyList();
		}
	}
	//endregion
}
