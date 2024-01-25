package com.evolveum.midpoint.repo.common;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.subscription.SubscriptionId;
import com.evolveum.midpoint.repo.common.subscription.SubscriptionState.TimeValidity;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * Tests for date validity of subscription.
 */
public class SubscriptionIdDateValidityTest extends AbstractUnitTest {

    protected static final LocalDate JAN_01_2024 = LocalDate.of(2024, 1, 1);
    protected static final LocalDate JAN_31_2024 = LocalDate.of(2024, 1, 31);
    protected static final LocalDate MAY_01_2024 = LocalDate.of(2024, 5, 1);
    protected static final LocalDate MAY_10_2024 = LocalDate.of(2024, 5, 10);
    protected static final LocalDate MAY_31_2024 = LocalDate.of(2024, 5, 31);
    protected static final LocalDate JUN_01_2024 = LocalDate.of(2024, 6, 1);
    protected static final LocalDate DEC_31_2024 = LocalDate.of(2024, 12, 31);

    @Test
    public void beforeExpiration() {
        assertMonthsAfter("0125", JAN_01_2024, 0);
        assertMonthsAfter("0125", DEC_31_2024, 0);
    }

    @Test
    public void inFirstMonthAfter() {
        assertMonthsAfter("0424", MAY_01_2024, 1);
        assertMonthsAfter("0424", MAY_10_2024, 1);
        assertMonthsAfter("0424", MAY_31_2024, 1);
        // Grace period is gone on Aug 1st, 2024
        assertDaysToGracePeriodGone("0424", MAY_01_2024, 92);
        assertDaysToGracePeriodGone("0424", MAY_10_2024, 83);
        assertDaysToGracePeriodGone("0424", MAY_31_2024, 62);
        assertDaysToGracePeriodGone("0424", JUN_01_2024, 61);
    }

    @Test
    public void inSecondMonthAfter() {
        assertMonthsAfter("0324", MAY_01_2024, 2);
        assertMonthsAfter("0324", MAY_10_2024, 2);
        assertMonthsAfter("0324", MAY_31_2024, 2);
        // Grace period is gone on July 1st, 2024
        assertDaysToGracePeriodGone("0324", MAY_01_2024, 61);
        assertDaysToGracePeriodGone("0324", MAY_10_2024, 52);
        assertDaysToGracePeriodGone("0324", MAY_31_2024, 31);
        assertDaysToGracePeriodGone("0324", JUN_01_2024, 30);
    }

    @Test
    public void inThirdMonthAfter() {
        assertMonthsAfter("0224", MAY_01_2024, 3);
        assertMonthsAfter("0224", MAY_10_2024, 3);
        assertMonthsAfter("0224", MAY_31_2024, 3);
        // Grace period is gone on June 1st, 2024
        assertDaysToGracePeriodGone("0224", MAY_01_2024, 31);
        assertDaysToGracePeriodGone("0224", MAY_10_2024, 22);
        assertDaysToGracePeriodGone("0224", MAY_31_2024, 1);
        assertDaysToGracePeriodGone("0224", JUN_01_2024, 0);
    }

    @Test
    public void inFourthMonthAfter() {
        assertMonthsAfter("0124", MAY_01_2024, 4);
    }

    @Test(expectedExceptions = { DateTimeException.class })
    public void wrongMonth() {
        assertMonthsAfter("2123", JAN_01_2024, 0);
    }

    @Test
    public void inFirstMonthAfterAcrossYears() {
        assertMonthsAfter("1223", JAN_01_2024, 1);
        assertMonthsAfter("1223", JAN_31_2024, 1);
    }

    @Test
    public void inSecondMonthAfterAcrossYears() {
        assertMonthsAfter("1123", JAN_01_2024, 2);
        assertMonthsAfter("1123", JAN_31_2024, 2);
    }

    @Test
    public void inThirdMonthAfterAcrossYears() {
        assertMonthsAfter("1023", JAN_01_2024, 3);
    }

    @Test
    public void inFourthMonthAfterAcrossYears() {
        assertMonthsAfter("0923", JAN_01_2024, 4);
    }

    @Test
    public void afterYear() {
        assertMonthsAfter("0123", JAN_01_2024, 12);
    }

    private void assertMonthsAfter(String subscriptionSpec, LocalDate currentDate, int expectedMonths) {
        TimeValidity validity = TimeValidity.determine(
                SubscriptionId.forTesting(SubscriptionId.Type.ANNUAL, subscriptionSpec));
        assert validity != null;
        Assertions.assertThat(validity.getMonthsAfter(currentDate))
                .as("computed months for subs: %s, current: %s", subscriptionSpec, currentDate)
                .isEqualTo(expectedMonths);
    }

    private void assertDaysToGracePeriodGone(String subscriptionSpec, LocalDate currentDate, int expectedDays) {
        TimeValidity validity = TimeValidity.determine(
                SubscriptionId.forTesting(SubscriptionId.Type.ANNUAL, subscriptionSpec));
        assert validity != null;
        Assertions.assertThat(validity.daysToGracePeriodGone(currentDate))
                .as("computed days to grace period gone for subs: %s, current: %s", subscriptionSpec, currentDate)
                .isEqualTo(expectedDays);
    }
}
