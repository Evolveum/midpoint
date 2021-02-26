package com.evolveum.midpoint.schrodinger.util;

import com.codeborne.selenide.Selenide;
import org.testng.asserts.Assertion;
import org.testng.asserts.IAssert;

public class AssertionWithScreenshot extends Assertion {

    private String screenshotFileName = "assertionFailure";

    @Override
    public void onAssertFailure(IAssert<?> var1, AssertionError var2) {
        Selenide.screenshot(screenshotFileName);
        super.onAssertFailure(var1, var2);
    }

    public AssertionWithScreenshot screenshotFileName(String screenshotFileName) {
        this.screenshotFileName = screenshotFileName;
        return this;
    }

}
