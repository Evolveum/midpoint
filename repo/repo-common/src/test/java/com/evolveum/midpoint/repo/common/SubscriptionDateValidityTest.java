package com.evolveum.midpoint.repo.common;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.subscription.Subscription;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * Tests for date validity of subscription.
 */
public class SubscriptionDateValidityTest extends AbstractUnitTest {

    protected static final LocalDate JAN_01_2024 = LocalDate.of(2024, 1, 1);
    protected static final LocalDate JAN_31_2024 = LocalDate.of(2024, 1, 31);
    protected static final LocalDate MAY_01_2024 = LocalDate.of(2024, 5, 1);
    protected static final LocalDate MAY_10_2024 = LocalDate.of(2024, 5, 10);
    protected static final LocalDate MAY_31_2024 = LocalDate.of(2024, 5, 31);
    protected static final LocalDate DEC_31_2024 = LocalDate.of(2024, 12, 31);

    @Test
    public void beforeExpiration() {
        validateDate("0125", JAN_01_2024, 0);
        validateDate("0125", DEC_31_2024, 0);
    }

    @Test
    public void inFirstMonthAfter() {
        validateDate("0424", MAY_01_2024, 1);
        validateDate("0424", MAY_10_2024, 1);
        validateDate("0424", MAY_31_2024, 1);
    }

    @Test
    public void inSecondMonthAfter() {
        validateDate("0324", MAY_01_2024, 2);
        validateDate("0324", MAY_10_2024, 2);
        validateDate("0324", MAY_31_2024, 2);
    }

    @Test
    public void inThirdMonthAfter() {
        validateDate("0224", MAY_01_2024, 3);
        validateDate("0224", MAY_10_2024, 3);
        validateDate("0224", MAY_31_2024, 3);
    }

    @Test
    public void inFourthMonthAfter() {
        validateDate("0124", MAY_01_2024, 4);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void wrongMonth() {
        validateDate("2123", JAN_01_2024, 0);
    }

    @Test
    public void inFirstMonthAfterAcrossYears() {
        validateDate("1223", JAN_01_2024, 1);
        validateDate("1223", JAN_31_2024, 1);
    }

    @Test
    public void inSecondMonthAfterAcrossYears() {
        validateDate("1123", JAN_01_2024, 2);
        validateDate("1123", JAN_31_2024, 2);
    }

    @Test
    public void inThirdMonthAfterAcrossYears() {
        validateDate("1023", JAN_01_2024, 3);
    }

    @Test
    public void inFourthMonthAfterAcrossYears() {
        validateDate("0923", JAN_01_2024, 4);
    }

    @Test
    public void afterYear() {
        validateDate("0123", JAN_01_2024, 12);
    }

    private void validateDate(String subscriptionSpec, LocalDate currentDate, int expectedMonths) {
        int months = Subscription.testTimeValidity(currentDate, subscriptionSpec);
        Assertions.assertThat(months)
                .as("computed months for subs: %s, current: %s", subscriptionSpec, currentDate)
                .isEqualTo(expectedMonths);
    }
}
