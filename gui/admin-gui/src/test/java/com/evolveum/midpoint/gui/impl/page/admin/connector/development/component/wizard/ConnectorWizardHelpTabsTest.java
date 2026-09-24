/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.impl.component.wizard.collapse.HelpChapter;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.HelpTab;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnDevDocumentationTopic;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevApplicationInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevCreateConnectorResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverConnectivityEndpointResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverDocumentationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverGlobalInformationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverObjectClassAttributesResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverObjectClassEndpointsResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverObjectClassInformationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevExportConnectorResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevFixObjectClassResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevGenerateArtifactResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevIntegrationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevProcessDocumentationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshSchemaResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the mapping of conndev documentation topics onto the wizard help tabs, including
 * the screen-specific key to generic key fallback and the integration protocol resolution.
 */
public class ConnectorWizardHelpTabsTest {

    @Test
    public void mapsTopicsToSingleTabWithOneChapterPerTopic() {
        var service = new StubConnectorDevelopmentService(Map.of(
                "search-all", List.of(
                        new ConnDevDocumentationTopic("search-all", "scim", "Search all (SCIM)", "<p>scim</p>"),
                        new ConnDevDocumentationTopic("search-all", null, "Search all (generic)", "<p>generic</p>"))));

        var tabs = ConnectorDevelopmentWizardUtil.helpTabs(service, "scim", "search-all", null);

        Assert.assertEquals(tabs.size(), 1);
        var chapters = tabs.get(0).getChapters();
        Assert.assertEquals(chapters.size(), 2);
        Assert.assertEquals(chapters.get(0).getTitle(), "Search all (SCIM)");
        Assert.assertEquals(chapters.get(0).getContentModel().getObject(), "<p>scim</p>");
        Assert.assertEquals(chapters.get(1).getTitle(), "Search all (generic)");
        Assert.assertEquals(chapters.get(1).getContentModel().getObject(), "<p>generic</p>");
        for (HelpChapter chapter : chapters) {
            Assert.assertTrue(chapter.isHtml(), "Conndev docs are fragment HTML and must render as-is");
        }
    }

    @Test
    public void fallsBackToFallbackKeyWhenPrimaryIsEmpty() {
        var service = new StubConnectorDevelopmentService(Map.of(
                "endpoint-selection", List.of(
                        new ConnDevDocumentationTopic("endpoint-selection", null, "Choosing endpoints", "<p>how to</p>"))));

        var tabs = ConnectorDevelopmentWizardUtil.helpTabs(service, "rest", "search-all-endpoint", "endpoint-selection");

        Assert.assertEquals(tabs.size(), 1);
        var chapters = tabs.get(0).getChapters();
        Assert.assertEquals(chapters.size(), 1);
        Assert.assertEquals(chapters.get(0).getTitle(), "Choosing endpoints");
    }

    @Test
    public void primaryKeyWinsOverFallback() {
        var service = new StubConnectorDevelopmentService(Map.of(
                "search-all-endpoint", List.of(
                        new ConnDevDocumentationTopic("search-all-endpoint", "rest", "Search all endpoints", "<p>specific</p>")),
                "endpoint-selection", List.of(
                        new ConnDevDocumentationTopic("endpoint-selection", null, "Choosing endpoints", "<p>generic</p>"))));

        var tabs = ConnectorDevelopmentWizardUtil.helpTabs(service, "rest", "search-all-endpoint", "endpoint-selection");

        var chapters = tabs.get(0).getChapters();
        Assert.assertEquals(chapters.size(), 1);
        Assert.assertEquals(chapters.get(0).getTitle(), "Search all endpoints");
    }

    @Test
    public void returnsEmptyListWhenNoTopicMatches() {
        var service = new StubConnectorDevelopmentService(Map.of());

        Assert.assertTrue(ConnectorDevelopmentWizardUtil.helpTabs(service, "scim", "no-such-key", "no-such-fallback").isEmpty());
        Assert.assertTrue(ConnectorDevelopmentWizardUtil.helpTabs(service, "scim", "no-such-key", null).isEmpty());
    }

    @Test
    public void usesKeyAsTitleWhenTitleMissing() {
        var service = new StubConnectorDevelopmentService(Map.of(
                "create", List.of(new ConnDevDocumentationTopic("create", null, null, "<p>c</p>"))));

        var tabs = ConnectorDevelopmentWizardUtil.helpTabs(service, null, "create", null);

        Assert.assertEquals(tabs.get(0).getChapters().get(0).getTitle(), "create");
    }

    @Test
    public void integrationProtocolResolution() {
        Assert.assertEquals(ConnectorDevelopmentWizardUtil.integrationProtocol(ConnDevIntegrationType.SCIM, null), "scim");
        Assert.assertEquals(ConnectorDevelopmentWizardUtil.integrationProtocol(ConnDevIntegrationType.REST, ConnDevIntegrationType.SQL), "rest");
        Assert.assertEquals(ConnectorDevelopmentWizardUtil.integrationProtocol(null, ConnDevIntegrationType.SQL), "sql");
        Assert.assertNull(ConnectorDevelopmentWizardUtil.integrationProtocol(null, null));
    }

    /** Minimal {@link ConnectorDevelopmentService} double; only {@code getDocumentationTopics} is exercised. */
    private static final class StubConnectorDevelopmentService implements ConnectorDevelopmentService {

        private final Map<String, List<ConnDevDocumentationTopic>> topicsByKey;

        private StubConnectorDevelopmentService(Map<String, List<ConnDevDocumentationTopic>> topicsByKey) {
            this.topicsByKey = topicsByKey;
        }

        @Override
        public List<ConnDevDocumentationTopic> getDocumentationTopics(String key, String protocol) {
            return topicsByKey.getOrDefault(key, List.of());
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not used in this test");
        }

        @Override
        public ConnectorDevelopmentOperation startFromNew(ConnDevApplicationInfoType basicInfo, OperationResult result) {
            throw unsupported();
        }

        @Override
        public ConnectorDevelopmentOperation continueFrom(ConnectorDevelopmentType type) {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevCreateConnectorResultType> getCreateConnectorStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevDiscoverGlobalInformationResultType> getDiscoverBasicInformationStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevDiscoverDocumentationResultType> getDiscoverDocumentationStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public void removeDiscoveredDocumentation(String token, String name, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevProcessDocumentationResultType> getProcessDocumentationStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevGenerateArtifactResultType> getGenerateArtifactStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevFixObjectClassResultType> getFixObjectClassStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevDiscoverObjectClassInformationResultType> getDiscoverObjectClassInformationStatus(
                String token, Task task, OperationResult result) throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevDiscoverObjectClassAttributesResultType> getDiscoverObjectClassAttributesStatus(
                String token, Task task, OperationResult result) throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevDiscoverObjectClassEndpointsResultType> getDiscoverObjectClassEndpointsStatus(
                String token, Task task, OperationResult result) throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevRefreshSchemaResultType> getRefreshSchemaStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevDiscoverConnectivityEndpointResultType> getDiscoverConnectivityEndpointStatus(
                String token, Task task, OperationResult result) throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevExportConnectorResultType> getExportConnectorStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public StatusInfo<ConnDevExportConnectorResultType> getUploadConnectorStatus(String token, Task task, OperationResult result)
                throws CommonException {
            throw unsupported();
        }

        @Override
        public InputStream getExportedConnectorFileStream(String fileName, String nodeOid, Task task, OperationResult result)
                throws CommonException, IOException {
            throw unsupported();
        }
    }
}
