/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.repo.sqlbase.NativeOnlySupportedException;
import com.evolveum.midpoint.schema.query.TypedQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.google.common.io.CharSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestBugreports extends AbstractInternalModelIntegrationTest {



    @Test
    public void bug9876ReindexShouldNotFailWhenInducementAdded() throws Exception {
        skipIfNotNativeRepository();
        var result = createOperationResult();
        var task = createTask();
        var options = new ImportOptionsType()
                .compatMode(false)
                .fetchResourceSchema(false)
                .keepOid(false)
                .referentialIntegrity(false)
                .overwrite(true)
                .summarizeErrors(true)
                .summarizeErrors(true)
                .modelExecutionOptions(new ModelExecuteOptionsType()
                        .raw(false)
                );
        var onlyInducement = asStream("""
                <?xml version="1.0" encoding="UTF-8"?>
                <role xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                  xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                  xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
                  xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
                  xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                  xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3"
                  xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
                  oid="90df4d54-4ae9-11ef-9454-0242ac120002">
                    <name>test-role</name>
                    <inducement>
                        <targetRef oid="00000000-0000-0000-0000-000000000004" relation="org:default" type="c:RoleType">
                        </targetRef>
                    </inducement>
                </role>
                """);

        modelService.importObjectsFromStream(onlyInducement, PrismContext.LANG_XML, options ,task, result);
        var inducementAndAssignment = asStream("""
                <?xml version="1.0" encoding="UTF-8"?>
                <role xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                  xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                  xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
                  xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
                  xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                  xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3"
                  xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
                  oid="90df4d54-4ae9-11ef-9454-0242ac120002">
                    <name>test-role</name>
                    <inducement>
                        <targetRef oid="00000000-0000-0000-0000-000000000004" relation="org:default" type="c:RoleType">
                        </targetRef>
                    </inducement>
                    <assignment>
                        <targetRef oid="00000000-0000-0000-0000-000000000004" relation="org:default" type="c:RoleType">
                        </targetRef>
                    </assignment>
                </role>
                """);
        modelService.importObjectsFromStream(inducementAndAssignment, PrismContext.LANG_XML, options ,task, result);
        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void bug9854DetailedErrorShouldBeThrown() throws Exception {
        var task = createTask();
        var result = createOperationResult();
        var query = TypedQuery.parse(ResourceType.class, """
                operationalState/lastAvailabilityStatus = "up"
                            and archetypeRef not matches (oid = "00000000-0000-0000-0000-000000000703")
                            and template != true and abstract != true
                """);
        try {
            modelService.searchObjects(query, task, result);
        } catch (SystemException e) {
            assertThat(e).hasCauseInstanceOf(NativeOnlySupportedException.class);
            

        }

    }

    private InputStream asStream(String s) throws IOException {
        return CharSource.wrap(s).asByteSource(StandardCharsets.UTF_8).openStream();

    }
}
