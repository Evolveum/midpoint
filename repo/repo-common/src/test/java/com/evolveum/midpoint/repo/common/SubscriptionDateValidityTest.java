package com.evolveum.midpoint.repo.common;

import com.evolveum.midpoint.repo.common.util.SubscriptionUtil;
import com.evolveum.midpoint.repo.common.util.SubscriptionWrapper.SubscriptionValidity;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Tests for date validity of subscription.
 */
public class SubscriptionDateValidityTest extends AbstractUnitTest{

    @Test
    public void invalidYear() throws Exception{
        validateDate("050123", "0124", SubscriptionValidity.INVALID);
    }

    @Test
    public void validDate() throws Exception{
        validateDate("050125", "0124", SubscriptionValidity.VALID);
    }

    @Test
    public void firstMonthInvalid() throws Exception{
        validateDate("050424", "0524", SubscriptionValidity.INVALID_FIRST_MONTH);
    }

    @Test
    public void secondMonthInvalid() throws Exception{
        validateDate("050324", "0524", SubscriptionValidity.INVALID_SECOND_MONTH);
    }

    @Test
    public void thirdMonthInvalid() throws Exception{
        validateDate("050224", "0524", SubscriptionValidity.INVALID_THIRD_MONTH);
    }

    @Test
    public void fourthMonthInvalid() throws Exception{
        validateDate("050124", "0524", SubscriptionValidity.INVALID);
    }

    @Test
    public void wrongMonth() throws Exception{
        validateDate("052123", "0124", SubscriptionValidity.INVALID);
    }

    @Test
    public void firstMonthInvalidThroughEndOfYear() throws Exception{
        validateDate("051223", "0124", SubscriptionValidity.INVALID_FIRST_MONTH);
    }

    @Test
    public void secondMonthInvalidThroughEndOfYear() throws Exception{
        validateDate("051123", "0124", SubscriptionValidity.INVALID_SECOND_MONTH);
    }

    @Test
    public void thirdMonthInvalidThroughEndOfYear() throws Exception{
        validateDate("051023", "0124", SubscriptionValidity.INVALID_THIRD_MONTH);
    }

    @Test
    public void fourthMonthInvalidThroughEndOfYear() throws Exception{
        validateDate("050923", "0124", SubscriptionValidity.INVALID);
    }

    private void validateDate(String testedDatePartOfSubscriptionId, String currentDate, SubscriptionValidity expectedValidity) throws ParseException {

        SubscriptionValidity validity = SubscriptionUtil.resolveValidityForSubscriptionId(
                testedDatePartOfSubscriptionId,
                new SimpleDateFormat("MMyy").parse(currentDate));
        Assertions.assertThat(validity).isEqualByComparingTo(expectedValidity);
    }
}
