/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import com.beust.jcommander.JCommander;

import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;

import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@UnusedTestElement("throws NPE, see ctx.init")
public class NinjaContextTest {

    @Test
    public void setupRepositoryViaMidpointHome() throws Exception {
        JCommander jc = NinjaUtils.setupCommandLineParser();
        jc.parse("-m", "./target/midpoint-home", "-U", "jdbc:postgresql://localhost/midpoint", "-u", "midpoint", "-p", "qwe123");

        ConnectionOptions options = NinjaUtils.getOptions(jc.getObjects(), ConnectionOptions.class);

        NinjaContext ctx = new NinjaContext(List.of(options), NinjaApplicationContextLevel.FULL_REPOSITORY);

        RepositoryService service = ctx.getRepository();

        OperationResult result = new OperationResult("get user");
        PrismObject obj = service.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result);

        System.out.println(obj.debugDump());
    }
}
