/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.boot;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.http.HttpStatus;

import com.evolveum.midpoint.web.security.MidPointApplication;

public class MidPointErrorPageRegistrar implements ErrorPageRegistrar {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {

        registry.addErrorPages(
                new ErrorPage(HttpStatus.UNAUTHORIZED, MidPointApplication.MOUNT_UNAUTHORIZED_ERROR),
                new ErrorPage(HttpStatus.FORBIDDEN, MidPointApplication.MOUNT_FORBIDDEN_ERROR),
                new ErrorPage(HttpStatus.NOT_FOUND, MidPointApplication.MOUNT_NOT_FOUND_ERROR),
                new ErrorPage(HttpStatus.GONE, MidPointApplication.MOUNT_GONE_ERROR),
                new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, MidPointApplication.MOUNT_INTERNAL_SERVER_ERROR));
    }
}
