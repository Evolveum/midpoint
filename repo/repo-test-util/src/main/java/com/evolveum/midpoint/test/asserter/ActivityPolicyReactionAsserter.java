/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import org.assertj.core.api.Assertions;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedActivityPolicyReactionType;

public class ActivityPolicyReactionAsserter<RA> extends AbstractAsserter<RA> {

    private final EvaluatedActivityPolicyReactionType reaction;

    public ActivityPolicyReactionAsserter(EvaluatedActivityPolicyReactionType reaction, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.reaction = reaction;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ActivityPolicyReactionAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(reaction));
        return this;
    }

    public EvaluatedActivityPolicyReactionType getReaction() {
        return reaction;
    }

    public ActivityPolicyReactionAsserter<RA> assertEnforced(boolean enforced) {
        Assertions.assertThat(reaction.getEnforced())
                .withFailMessage("Expected reaction '%s' to be enforced: %s, but it was not.",
                        reaction.getRef(), enforced)
                .isEqualTo(enforced);

        return this;
    }
}
