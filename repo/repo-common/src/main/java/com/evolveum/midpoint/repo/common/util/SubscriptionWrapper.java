package com.evolveum.midpoint.repo.common.util;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wrapper for subscription. Contains information about type and validity.
 */
public class SubscriptionWrapper {

    private final SubscriptionType type;
    private final SubscriptionValidity validity;

    SubscriptionWrapper(SubscriptionType type, SubscriptionValidity validity) {
        this.type = type;
        this.validity = resolveValidity(type, validity);
    }

    private SubscriptionValidity resolveValidity(SubscriptionType type, SubscriptionValidity originalValidity) {
        if (type == SubscriptionType.DEMO_SUBSCRIPTION
                && (originalValidity == SubscriptionValidity.INVALID_FIRST_MONTH
                || originalValidity == SubscriptionValidity.INVALID_SECOND_MONTH
                || originalValidity == SubscriptionValidity.INVALID_THIRD_MONTH)) {
            return SubscriptionValidity.INVALID;
        }
        return originalValidity;
    }

    SubscriptionWrapper(SubscriptionValidity subscriptionValidity) {
        this(null, subscriptionValidity);
    }

    public boolean isCorrect() {
        if (type != null && (validity == SubscriptionValidity.VALID
                || validity == SubscriptionValidity.INVALID_FIRST_MONTH
                || validity == SubscriptionValidity.INVALID_SECOND_MONTH
                || validity == SubscriptionValidity.INVALID_THIRD_MONTH)) {
            return true;
        }
        return false;
    }

    public SubscriptionType getType() {
        return type;
    }

    public SubscriptionValidity getValidity() {
        return validity;
    }

    /**
     * Enumeration for the type of subscription.
     */
    public enum SubscriptionType {

        ANNUAL_SUBSCRIPTION("01"),
        PLATFORM_SUBSCRIPTION("02"),
        DEPLOYMENT_SUBSCRIPTION("03"),
        DEVELOPMENT_SUBSCRIPTION("04"),
        DEMO_SUBSCRIPTION("05");

        private final String subscriptionType;

        SubscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

//        public boolean isCorrect() {
//            return subscriptionType != null;
//        }

        private static final Map<String, SubscriptionType> CODE_TO_TYPE;

        static {
            CODE_TO_TYPE = Arrays.stream(values())
                    .filter(v -> v.subscriptionType != null)
                    .collect(Collectors.toUnmodifiableMap(v -> v.subscriptionType, Function.identity()));
        }

        @Nullable
        public static SubscriptionType resolveType(String code) {
            return CODE_TO_TYPE.get(code);
        }
    }

    /**
     * Enumeration for the validity of subscription.
     */
    public enum SubscriptionValidity {
        NONE,
        INVALID,
        INVALID_FIRST_MONTH,
        INVALID_SECOND_MONTH,
        INVALID_THIRD_MONTH,
        VALID
    }
}
