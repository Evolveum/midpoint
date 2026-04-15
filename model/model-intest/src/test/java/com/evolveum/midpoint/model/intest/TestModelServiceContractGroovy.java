/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Uses dummy resource with Groovy expressions.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelServiceContractGroovy extends TestModelServiceContract {

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_GROOVY_FILE;
    }

}
