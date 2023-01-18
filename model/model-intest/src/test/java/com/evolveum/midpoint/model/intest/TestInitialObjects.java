/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Test that initial objects are parseable, correct, that they are not deprecated and so on.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestInitialObjects extends AbstractIntegrationTest {

    private static final String INITIAL_OBJECTS_RESOURCE_PATTERN = "classpath*:/initial-objects/**/*.xml";

    @Test
    public void testInitialObjects() throws Exception {
        ObjectValidator validator = new ObjectValidator(prismContext);
        validator.setAllWarnings();

        StringBuilder errorsSb = new StringBuilder();

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(INITIAL_OBJECTS_RESOURCE_PATTERN);

        for (Resource resource : resources) {
            try (InputStream is = resource.getInputStream()) {
                testInitialObject(validator, errorsSb, is, resource.getFilename());
            } catch (Throwable e) {
                String msg = "Error processing file " + resource.getFilename() + ": " + e.getMessage();
                logger.error(msg, e);
                displayException(msg, e);
                throw e;
            }
        }

        if (errorsSb.length() != 0) {
            throw new SchemaException(errorsSb.toString());
        }
    }

    private <O extends ObjectType> void testInitialObject(ObjectValidator validator, StringBuilder errorsSb, InputStream is, String fileName) throws SchemaException, IOException {
        PrismObject<O> object = prismContext.parserFor(is).xml().parse();
        ValidationResult validationResult = validator.validate(object);
        if (validationResult.isEmpty() || isIgnoredWarning(validationResult)) {
            display("Checked " + object + ": no warnings");
            return;
        }
        displayDumpable("Validation warnings for " + object, validationResult);
        for (ValidationItem valItem : validationResult.getItems()) {
            errorsSb.append(fileName);
            errorsSb.append(" ");
            errorsSb.append(object);
            errorsSb.append(" ");
            valItem.shortDump(errorsSb);
            errorsSb.append("\n");
        }
    }

    private boolean isIgnoredWarning(ValidationResult validationResult) {
        for (ValidationItem item : validationResult.getItems()) {
            if (!item.getStatus().equals(OperationResultStatus.WARNING)) {
                return false;
            }
        }
        return true;
    }
}
