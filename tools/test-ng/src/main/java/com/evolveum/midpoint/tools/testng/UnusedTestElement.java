package com.evolveum.midpoint.tools.testng;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates unintentionally unused test, test method or any other test supporting method.
 * For test classes it mostly means they are not in suite.
 * Use comments, not this annotation to indicate that method/field is  prepared for future.
 * Marking something with this annotation means a problem that should be eventually resolved.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface UnusedTestElement {

    /**
     * Typically documents the reason.
     */
    String value() default "";
}
