package com.evolveum.midpoint.schrodinger.util;

import com.codeborne.selenide.Selenide;
import org.testng.asserts.Assertion;
import org.testng.asserts.IAssert;

public class AssertionWithScreenshot extends Assertion {

    private String screenshotFileName;

    @Override
    public void onAssertFailure(IAssert<?> var1, AssertionError var2) {
        Selenide.screenshot(screenshotFileName != null ? screenshotFileName : generateScreenshotName());
        super.onAssertFailure(var1, var2);
    }

    private String generateScreenshotName() {
        StackTraceElement[] stack = new Throwable().fillInStackTrace().getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement el = stack[i];
            if (el.getClassName().contains("testing.schrodinger")) {
                return el.getClassName().substring(el.getClassName().lastIndexOf(".") + 1) + "_" + el.getMethodName() + "_line" + el.getLineNumber();
            }
        }
        return "assertionError";
    }

    public AssertionWithScreenshot screenshotFileName(String screenshotFileName) {
        this.screenshotFileName = screenshotFileName;
        return this;
    }

}
