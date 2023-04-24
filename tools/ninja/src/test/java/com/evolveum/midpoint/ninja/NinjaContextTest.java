/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.ninja.impl.RestService;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import jakarta.xml.ws.Holder;

/**
 * Created by Viliam Repan (lazyman).
 */
@UnusedTestElement("throws NPE, see ctx.init")
public class NinjaContextTest {

    @Test
    public void setupModelClient() throws Exception {
        NinjaContext ctx = new NinjaContext(null);

        ctx.init(null);
        RestService service = ctx.getRestService();

        Holder object = new Holder();
        Holder result = new Holder();
//        service.get(UserType.COMPLEX_TYPE, SystemObjectsType.USER_ADMINISTRATOR.value(), null, object, result);

        AssertJUnit.assertNotNull(object.value);
    }

    @Test
    public void setupRepositoryViaMidpointHome() throws Exception {
        JCommander jc = NinjaUtils.setupCommandLineParser();
        jc.parse("-m", "./target/midpoint-home", "-U", "jdbc:postgresql://localhost/midpoint", "-u", "midpoint", "-p", "qwe123");

        ConnectionOptions options = NinjaUtils.getOptions(jc, ConnectionOptions.class);

        NinjaContext ctx = new NinjaContext(null);
        ctx.init(options);

        RepositoryService service = ctx.getRepository();

        OperationResult result = new OperationResult("get user");
        PrismObject obj = service.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result);

        System.out.println(obj.debugDump());
    }
}
