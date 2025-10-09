/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api.authorization;

import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for the page which contains url address, on which the page is available,
 * and authorization urls for page, which logged user have to have.
 *
 * @author lazyman
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PageDescriptor {

    Url[] urls() default {};

    Class<? extends IPageParametersEncoder> encoder() default PageParametersEncoder.class;

    AuthorizationAction[] action() default {};

    /**
     * Permit access to all users (even non-authenticated users)
     */
    boolean permitAll() default false;

    /**
     * Indicate login page, Permit access to all users (even non-authenticated users)
     */
    boolean loginPage() default false;

    /**
     * If set to true, page is available only if the experimental features are turned on.
     * Also, the link in the sidebar panel (on the left) is visible for experimental pages only
     * if the experimental featires are on.
     */
    boolean experimental() default false;

    /**
     * If isn't empty, page is available only for authentication module identify by module name.
     */
    String authModule() default "";

}
