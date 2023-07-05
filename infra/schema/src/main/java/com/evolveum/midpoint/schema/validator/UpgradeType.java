package com.evolveum.midpoint.schema.validator;

public enum UpgradeType {

    /**
     * No specific action needed from administrator, upgrade of specific item should be handled automatically and
     * deprecated feature was replaced by new one without change in behaviour.
     */
    SEAMLESS,

    /**
     * Administrator should preview upgrade change that's planned by upgrade process.
     * Deprecated feature has replacement by new one, however there could be some changes in behavior or configuration that need review.
     */
    PREVIEW,

    /**
     * Upgrade tool will notify about validation issue, however such item needs to be resolved by administrator manually.
     */
    MANUAL
}
