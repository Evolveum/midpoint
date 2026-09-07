/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.smart.impl.DescriptiveItemPath.asStringSimple;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.smart.api.ServiceClientFactory;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Abstract superclass for Smart Integration tests.
 */
public abstract class AbstractSmartIntegrationTest extends AbstractModelIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    private static final TestObject<UserType> USER_ADMINISTRATOR = TestObject.file(
            COMMON_DIR, "user-administrator.xml", "00000000-0000-0000-0000-000000000002");
    private static final TestObject<RoleType> ROLE_SUPERUSER = TestObject.file(
            COMMON_DIR, "role-superuser.xml", "00000000-0000-0000-0000-000000000004");

    static final QName OC_ACCOUNT_QNAME = new QName(NS_RI, "account");

    protected record AttributePair(String appAttribute, String midPointAttribute) {}

    protected record MockMapping(AttributePair pair, List<String> scripts) {

        public MockMapping {
            scripts = scripts != null ? List.copyOf(scripts) : List.of();
        }

        public MockMapping(ItemPath focusPath, ItemPath shadowPath, String... scripts) {
            this(
                    new AttributePair(asStringSimple(shadowPath), asStringSimple(focusPath)),
                    scripts != null ? Arrays.stream(scripts).filter(Objects::nonNull).toList() : List.of()
            );
        }
    }

    // Override the service client factory with our mocked version
    @TestBean(methodName = "com.evolveum.midpoint.smart.impl.TestServiceClientFactory#create")
    ServiceClientFactory clientFactoryMock;

    @Autowired
    SmartIntegrationServiceImpl smartIntegrationService;

    @Autowired
    protected com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService connectorDevelopmentService;

    protected ServiceClient createClient(MockMapping... mappings) {
        SiMatchSchemaResponseType matchResponse = new SiMatchSchemaResponseType();
        Map<AttributePair, Deque<String>> scriptsByPair = new HashMap<>();

        if (mappings != null) {
            for (MockMapping mapping : mappings) {
                matchResponse.attributeMatch(
                        new SiAttributeMatchSuggestionType()
                                .applicationAttribute(mapping.pair().appAttribute())
                                .midPointAttribute(mapping.pair().midPointAttribute())
                );

                if (!mapping.scripts().isEmpty()) {
                    scriptsByPair.put(mapping.pair(), new ArrayDeque<>(mapping.scripts()));
                }
            }
        }

        Function<Object, Object> responseFunction = request -> {
            if (request instanceof SiMatchSchemaRequestType) {
                return matchResponse;
            }
            String appName = null;
            String midName = null;
            if (request instanceof SiSuggestMappingRequestType suggestRequest) {
                appName = nameOf(suggestRequest.getApplicationAttribute());
                midName = nameOf(suggestRequest.getMidPointAttribute());
            } else if (request instanceof SiSuggestCategoricalMappingRequestType categoricalRequest) {
                appName = nameOf(categoricalRequest.getApplicationAttribute());
                midName = nameOf(categoricalRequest.getMidPointAttribute());
            }
            if (appName != null && midName != null) {
                Deque<String> queue = scriptsByPair.get(new AttributePair(appName, midName));
                String script = queue != null ? queue.poll() : null;
                if (script != null) {
                    return new SiSuggestMappingResponseType().transformationScript(script);
                }
            }
            return new SiSuggestMappingResponseType();
        };

        return new MockServiceClientImpl(responseFunction);
    }

    private static String nameOf(SiAttributeDefinitionType attribute) {
        return attribute != null ? attribute.getName() : null;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // We want logging config from logback-test.xml and not from system config object (unless suppressed)
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        repoAdd(USER_ADMINISTRATOR, initResult);
        repoAdd(ROLE_SUPERUSER, initResult);

        modelService.postInit(initResult);
        login(USER_ADMINISTRATOR.getNameOrig());
    }
}
