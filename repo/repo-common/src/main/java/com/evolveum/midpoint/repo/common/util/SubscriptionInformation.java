package com.evolveum.midpoint.repo.common.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The complete (parsed) subscription information: type and validity.
 *
 * TODO ... or just class name of "Subscription"?
 */
public class SubscriptionInformation {

    /** Null for invalid or missing subscription ID. */
    @Nullable private final SubscriptionType type;

    /** This is the adjusted value, see {@link #adjustValidity(SubscriptionType, SubscriptionValidity)}. */
    @NotNull private final SubscriptionValidity validity;

    SubscriptionInformation(@NotNull SubscriptionValidity subscriptionValidity) {
        this(null, subscriptionValidity);
    }

    SubscriptionInformation(@Nullable SubscriptionType type, @NotNull SubscriptionValidity validity) {
        this.type = type;
        this.validity = adjustValidity(type, validity);
    }

    /** Expired demo subscriptions are considered invalid. */
    private @NotNull SubscriptionValidity adjustValidity(SubscriptionType type, @NotNull SubscriptionValidity originalValidity) {
        if (type == SubscriptionType.DEMO_SUBSCRIPTION
                && (originalValidity == SubscriptionValidity.INVALID_FIRST_MONTH
                || originalValidity == SubscriptionValidity.INVALID_SECOND_MONTH
                || originalValidity == SubscriptionValidity.INVALID_THIRD_MONTH)) {
            return SubscriptionValidity.INVALID;
        }
        return originalValidity;
    }

    public boolean isCorrect() {
        return type != null && isTimeValid();
    }

    private boolean isTimeValid() {
        return validity == SubscriptionValidity.VALID
                || validity == SubscriptionValidity.INVALID_FIRST_MONTH
                || validity == SubscriptionValidity.INVALID_SECOND_MONTH
                || validity == SubscriptionValidity.INVALID_THIRD_MONTH;
    }

    public boolean isDemo() {
        return type == SubscriptionType.DEMO_SUBSCRIPTION;
    }

    public boolean isDemoOrIncorrect() {
        return isDemo() || !isCorrect();
    }

    public @Nullable SubscriptionType getType() {
        return type;
    }

    public @NotNull SubscriptionValidity getValidity() {
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

        @NotNull private final String code;

        SubscriptionType(@NotNull String code) {
            this.code = code;
        }

        private static final Map<String, SubscriptionType> CODE_TO_TYPE;

        static {
            CODE_TO_TYPE = Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(v -> v.code, Function.identity()));
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
