/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.schema.util.SimpleExpressionUtil.velocityExpression;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizedMessageTemplateContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

/**
 * This partially mirrors the SqaleRepoAddDeleteObjectTest, but works for both repositories.
 * Related disadvantage is that it can't check the repo on the low level, only via API.
 * TODO: Currently, it's merely a smoke test for a single type, but it can be extended.
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
public class RepoCommonAddGetTest extends AbstractRepoCommonTest {

    @Test
    public void test200MessageTemplate() throws Exception {
        OperationResult result = createOperationResult();

        given("message template");
        String objectName = "messageTemplate" + getTestNumber();
        var messageTemplate = new MessageTemplateType()
                .name(objectName)
                .defaultContent(new MessageTemplateContentType()
                        .subjectExpression(velocityExpression("subject-prefix")))
                .localizedContent(new LocalizedMessageTemplateContentType()
                        .language("sk_SK")
                        .subjectExpression(velocityExpression("Oné")));

        when("adding it to the repository");
        String oid = repositoryService.addObject(messageTemplate.asPrismObject(), null, result);
        assertThatOperationResult(result).isSuccess();

        then("it can be obtained using getObject");
        MessageTemplateType objectFromRepo =
                repositoryService.getObject(MessageTemplateType.class, oid, null, result)
                        .asObjectable();
        // not null implied by the contract
        assertThat(objectFromRepo.getDefaultContent()).isNotNull();
        ExpressionType subjectExpression = objectFromRepo.getDefaultContent().getSubjectExpression();
        assertThat(subjectExpression).isNotNull();
        assertThatOperationResult(result).isSuccess();

        and("it can be deleted");
        repositoryService.deleteObject(MessageTemplateType.class, oid, result);
        assertThatOperationResult(result).isSuccess();
        assertThatThrownBy(() -> repositoryService.getObject(MessageTemplateType.class, oid, null, result))
                .isInstanceOf(ObjectNotFoundException.class);
    }
}
