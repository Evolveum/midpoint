/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationObjectSelectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationParentSelectorType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

// TODO rename
class SelectorChainSegment {
    /** May be empty */
    @NotNull final ItemPath path;
    @Nullable final SelectorChainSegment next;
    @NotNull final AuthorizationObjectSelectorType selector;
    @NotNull final PathSet positives;
    @NotNull final PathSet negatives;

    private SelectorChainSegment(
            @NotNull ItemPath path,
            @Nullable SelectorChainSegment next,
            @NotNull AuthorizationObjectSelectorType selector,
            @NotNull PathSet positives,
            @NotNull PathSet negatives) {
        this.path = path;
        this.next = next;
        this.selector = selector;
        this.positives = positives;
        this.negatives = negatives;
    }

    private static SelectorChainSegment create(
            ItemPath path, SelectorChainSegment nextSegment, AuthorizationObjectSelectorType objectSelector,
            PathSet positives, PathSet negatives, AuthorizationEvaluation evaluation) throws ConfigurationException {
        configCheck(positives.isEmpty() || negatives.isEmpty(),
                "'item' and 'exceptItem' cannot be combined: %s vs %s in %s",
                positives, negatives, evaluation.getAuthorization());
        return new SelectorChainSegment(path, nextSegment, objectSelector, positives, negatives);
    }

    static Collection<SelectorChainSegment> createSelectorChains(
            PrismValue value, @NotNull AuthorizationEvaluation evaluation)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        Authorization autz = evaluation.getAuthorization();
        var positives = new PathSet(autz.getItems());
        var negatives = new PathSet(autz.getExceptItems());
        List<SelectorChainSegment> roots = new ArrayList<>();
        if (autz.getObjectSelectors().isEmpty()) {
            // Quite special case (e.g. for #all autz)
            // TODO is this OK?
            return List.of(
                    create(
                            ItemPath.EMPTY_PATH, null, new AuthorizationObjectSelectorType(),
                            positives, negatives, evaluation));
        } else {
            for (AuthorizationObjectSelectorType objectSelector : autz.getObjectSelectors()) {
                CollectionUtils.addIgnoreNull(
                        roots,
                        createSelectorChain(
                                ItemPath.EMPTY_PATH, null, objectSelector, positives, negatives, value, evaluation));
            }
        }
        return roots;
    }

    private static SelectorChainSegment createSelectorChain(
            ItemPath path,
            SelectorChainSegment nextSegment,
            AuthorizationObjectSelectorType objectSelector,
            PathSet positives,
            PathSet negatives,
            PrismValue value,
            AuthorizationEvaluation evaluation)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (evaluation.isSelectorApplicable(objectSelector, value, Set.of(), "TODO")) {
            return create(path, nextSegment, objectSelector, positives, negatives, evaluation);
        }

        ParentSelector parentSelector = getParentSelector(objectSelector);
        if (parentSelector == null) {
            return null;
        }

        var child = create(path, nextSegment, objectSelector, positives, negatives, evaluation);
        return createSelectorChain(
                parentSelector.getPath(),
                child,
                parentSelector.getSelector(),
                PathSet.of(),
                PathSet.of(),
                value,
                evaluation);
    }

    private static @Nullable ParentSelector getParentSelector(
            @NotNull AuthorizationObjectSelectorType objectSelector) throws ConfigurationException {
        AuthorizationParentSelectorType explicit = objectSelector.getParent();
        if (explicit != null) {
            return ParentSelector.forBean(explicit);
        } else {
            return ParentSelector.implicit(objectSelector.getType());
        }
    }
}
