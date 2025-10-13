/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DeleteTestSimple extends BaseSQLRepoTest {

    @Test
    public void delete001() throws Exception {
        PrismObject<UserType> user = prismContext.parseObject(new File(FOLDER_BASIC, "user0.xml"));

        OperationResult result = new OperationResult("Delete Test");
        String oid = repositoryService.addObject(user, null, result);
        logger.info("*** deleteObject ***");

        repositoryService.deleteObject(UserType.class, oid, result);
    }
}
