/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.perf;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlRecorder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.PerformanceTestMethodMixin;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test is not part of automatically run tests (it is not mentioned in suite XMLs).
 * This tests creates data in the repository and then tries various queries.
 * Data doesn't need to be business-realistic, full object representation can be dummy.
 */
public class GetUserTest extends SqaleRepoBaseTest
        implements PerformanceTestMethodMixin {



    private  Collection<SelectorOptions<GetOperationOptions>> getOptions = null;
    private final List<String> memInfo = new ArrayList<>();

    boolean skipOriginal = false;


    public static int USERS = 10;
    public static int GETS_TO_MEASURE = 15000;

    private static final String OPERATION_EXECUTION_EXAMPLE = """
            <operationExecution id="4" xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
                      xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
                      xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                      xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3"
                      xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                    <recordType>simple</recordType>
                    <timestamp>2024-01-02T22:22:54.738Z</timestamp>
                    <operation>
                        <objectDelta>
                            <t:changeType>modify</t:changeType>
                            <t:objectType>c:UserType</t:objectType>
                        </objectDelta>
                        <executionResult>
                            <operation>com.evolveum.midpoint.model.impl.lens.ChangeExecutor.executeDelta</operation>
                            <status>success</status>
                            <importance>normal</importance>
                            <token>1000000000000002248</token>
                        </executionResult>
                        <objectName>donatello</objectName>
                    </operation>
                    <operation>
                        <objectDelta>
                            <t:changeType>modify</t:changeType>
                            <t:objectType>c:UserType</t:objectType>
                        </objectDelta>
                        <executionResult>
                            <operation>com.evolveum.midpoint.model.impl.lens.ChangeExecutor.linkShadow</operation>
                            <status>success</status>
                            <importance>normal</importance>
                            <token>1000000000000002249</token>
                        </executionResult>
                        <objectName>donatello</objectName>
                        <resourceOid>8844dcca-775d-11e2-a0ac-001e8c717e5b</resourceOid>
                        <resourceName>HR Feed</resourceName>
                    </operation>
                    <status>success</status>
                    <initiatorRef oid="00000000-0000-0000-0000-000000000002" relation="org:default" type="c:UserType">
                        <!-- administrator -->
                    </initiatorRef>
                    <taskRef oid="486f0af0-990b-4f15-b81e-17f9a8e90d1a" relation="org:default" type="c:TaskType">
                        <!-- HR import -->
                    </taskRef>
                    <activityPath/>
                    <channel>http://midpoint.evolveum.com/xml/ns/public/common/channels-3#import</channel>
                </operationExecution>
                """;

    private static final String ASSIGNMENT_METADATA_EXAMPLE = """
            <metadata xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
                      xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
                      xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                      xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3"
                      xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                        <requestTimestamp>2024-01-02T22:22:54.622Z</requestTimestamp>
                        <requestorRef oid="00000000-0000-0000-0000-000000000002" relation="org:default" type="c:UserType">
                            <!-- administrator -->
                        </requestorRef>
                        <createTimestamp>2024-01-02T22:22:54.667Z</createTimestamp>
                        <creatorRef oid="00000000-0000-0000-0000-000000000002" relation="org:default" type="c:UserType">
                            <!-- administrator -->
                        </creatorRef>
                        <createChannel>http://midpoint.evolveum.com/xml/ns/public/common/channels-3#import</createChannel>
                        <createTaskRef oid="486f0af0-990b-4f15-b81e-17f9a8e90d1a" relation="org:default" type="c:TaskType">
                            <!-- HR import -->
                        </createTaskRef>
                        <originMappingName>Main role by employee type</originMappingName>
                    </metadata>
            """;

    private static final String ROLE_MEMBERSHIP_EXAMPLE = """
                    <roleMembershipRef  xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
                      xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
                      xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                      xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3"
                      xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      oid="8d62985c-7797-11e2-94a6-001e8c717e5b" relation="org:default" type="c:RoleType">
                    <!-- contractor -->
                    <_metadata>
                        <storage>
                            <createTimestamp>2024-01-05T03:25:21.685Z</createTimestamp>
                        </storage>
                        <provenance>
                            <assignmentPath>
                                <sourceRef oid="00ca9321-7298-45c6-9d22-f41ff71ea234" relation="org:default" type="c:UserType"/>
                                <segment>
                                    <segmentOrder>1</segmentOrder>
                                    <assignmentId>5</assignmentId>
                                    <targetRef oid="8d62985c-7797-11e2-94a6-001e8c717e5b" relation="org:default" type="c:RoleType"/>
                                    <matchingOrder>true</matchingOrder>
                                </segment>
                            </assignmentPath>
                        </provenance>
                    </_metadata>
                    </roleMembershipRef>
         """;

    private static final String USER_BASE_EXAMPLE = """
            <user xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
                      xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
                      xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
                      xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
                      xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3"
                      xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <metadata>
                          <requestTimestamp>2024-01-02T22:22:49.888Z</requestTimestamp>
                          <createTimestamp>2024-01-02T22:22:49.987Z</createTimestamp>
                          <createChannel>http://midpoint.evolveum.com/xml/ns/public/common/channels-3#init</createChannel>
                          <modifyTimestamp>2024-01-11T10:51:16.690Z</modifyTimestamp>
                          <modifierRef oid="00000000-0000-0000-0000-000000000002" relation="org:default" type="c:UserType">
                              <!-- administrator -->
                          </modifierRef>
                          <modifyChannel>http://midpoint.evolveum.com/xml/ns/public/common/channels-3#user</modifyChannel>
                          <lastProvisioningTimestamp>2024-01-11T10:51:16.690Z</lastProvisioningTimestamp>
                      </metadata>
                  <emailAddress>donatello@leonardo-workshop.org</emailAddress>
                  <telephoneNumber>+1111</telephoneNumber>
                  <credentials>
                      <password>
                          <metadata>
                              <createTimestamp>2024-01-02T22:22:49.893Z</createTimestamp>
                              <createChannel>http://midpoint.evolveum.com/xml/ns/public/common/channels-3#init</createChannel>
                          </metadata>
                          <value>
                              <t:encryptedData>
                                  <t:encryptionMethod>
                                      <t:algorithm>http://www.w3.org/2001/04/xmlenc#aes256-cbc</t:algorithm>
                                  </t:encryptionMethod>
                                  <t:keyInfo>
                                      <t:keyName>MlVOMUMA6n4f1zdOkEfEZFrmifI=</t:keyName>
                                  </t:keyInfo>
                                  <t:cipherData>
                                      <t:cipherValue>JJv2Cj+mmO7aIbz2cbdO3IVoDrA1EIVCRgetM9QOzsA=</t:cipherValue>
                                  </t:cipherData>
                              </t:encryptedData>
                          </value>
                      </password>
                  </credentials>
                  <fullName>Donatelloo di Niccolo di Betto Bardi</fullName>
                  <givenName>Donatelloo</givenName>
                  <familyName>di Niccolo di Betto Bardi</familyName>
                  <nickName>Donatello</nickName>
            </user>
            """;

    private ObjectReferenceType roleMembershipBase;
    private MetadataType assignmentMetadata;

    private UserType userBase;

    private OperationExecutionType operationExecutionBase;

    @BeforeClass
    public void initData() throws Exception {
        this.roleMembershipBase = PrismContext.get().parserFor(ROLE_MEMBERSHIP_EXAMPLE).parseRealValue(ObjectReferenceType.class);
        this.assignmentMetadata = PrismContext.get().parserFor(ASSIGNMENT_METADATA_EXAMPLE).parseRealValue(MetadataType.class);
        this.userBase = PrismContext.get().parserFor(USER_BASE_EXAMPLE).parseRealValue(UserType.class);
        this.operationExecutionBase = PrismContext.get().parserFor(OPERATION_EXECUTION_EXAMPLE).parseRealValue(OperationExecutionType.class);


        this.getOptions = GetOperationOptionsBuilder.create()
                .item(UserType.F_OPERATION_EXECUTION).dontRetrieve()
                .item(UserType.F_ASSIGNMENT).dontRetrieve()
                .item(UserType.F_ROLE_MEMBERSHIP_REF).dontRetrieve()
                .item(UserType.F_LINK_REF).dontRetrieve()
                .build();
        this.getOptions = null;
    }


    private ObjectReferenceType roleMembershipRef(String oid) {
        var ret = roleMembershipBase.clone();
        ret.oid(oid);
        return ret;
    }

    private AssignmentType assignment(String oid) {
        var ret = new AssignmentType()
                .activation(new ActivationType().effectiveStatus(ActivationStatusType.ENABLED))
                .targetRef(oid, RoleType.COMPLEX_TYPE);
        ret.metadata(assignmentMetadata.clone());
        return ret;
    }

    private UserType userBase(String name) {
        var ret = userBase.clone();
        ret.name(name);
        return ret;
    }

    private OperationExecutionType operationExecution(int i) {
        var ret = operationExecutionBase.clone();
        ret.asPrismContainerValue().setId(null);
        ret.taskRef(UUID.randomUUID().toString(), TaskType.COMPLEX_TYPE);
        return ret;
    }

    private String createUser(String prefix, int suffix,  int assignmentCount, int roleMembershipCount, int linkRefCount, int operationExecutionCount) throws SchemaException, ObjectAlreadyExistsException {

        var name = new StringBuilder(prefix).append("-")
                .append("a-").append(assignmentCount)
                .append("r-").append(roleMembershipCount)
                .append("l-").append(linkRefCount)
                .append("o-").append(operationExecutionCount)
                .append("-").append(suffix)
                .toString();
        var user = userBase(name);

        for (int i = 0; i < assignmentCount; i++) {
            var uuid = UUID.randomUUID().toString();
            user.assignment(assignment(uuid));
            if (i < roleMembershipCount) {
                user.roleMembershipRef(roleMembershipRef(uuid));
            }
        }
        // ROle membership are added in two steps - first for direct assignments, later additional.
        for (int i = assignmentCount; i < roleMembershipCount; i++) {
            user.roleMembershipRef(roleMembershipRef(UUID.randomUUID().toString()));
        }

        for (int i = 0; i < operationExecutionCount; i++) {
            user.operationExecution(operationExecution(i));
        }

        for (int i = 0; i < linkRefCount; i++) {
            user.linkRef(UUID.randomUUID().toString(), ShadowType.COMPLEX_TYPE);
        }
        return repositoryService.addObject(user.asPrismObject(), null, createOperationResult());

    }

    @BeforeMethod
    public void reportBeforeTest() {
        Runtime.getRuntime().gc();
        memInfo.add(String.format("%-40.40s before: %,15d",
                contextName(), Runtime.getRuntime().totalMemory()));
        queryRecorder.clearBuffer();
        queryRecorder.stopRecording(); // each test starts recording as needed
    }

    @AfterMethod
    public void reportAfterTest() {
        Collection<SqlRecorder.QueryEntry> sqlBuffer = queryRecorder.getQueryBuffer();
        if (!sqlBuffer.isEmpty()) {
            display("Recorded SQL queries:");
            for (SqlRecorder.QueryEntry entry : sqlBuffer) {
                display(entry.toString());
            }
        }

        memInfo.add(String.format("%-40.40s  after: %,15d",
                contextName(), Runtime.getRuntime().totalMemory()));
    }

    @Test
    public void test100_Original_PureOverhead() throws Exception {
        if (!skipOriginal) {
            original(0, 0, 0, 0);
        }
    }


    @Test
    public void test200_Original_10a_10r_5l_10o() throws Exception {
        if (!skipOriginal) {
            original(10, 10, 5, 10);
        }
    }


    @Test
    public void test300_Original_100a_100r_5l_10o() throws Exception {
        if (!skipOriginal) {
            original(100, 100, 5, 10);
        }
    }


    @Test
    public void test400_Original_1000a_1000r_5l_10o() throws Exception {
        if (!skipOriginal) {
            original(1000, 1000, 5, 10);
        }
    }






    public void original(int assignments, int roleMemberships, int linkRefs, int operationExecutions) throws Exception {
            perform( "orig", assignments, roleMemberships, linkRefs,  operationExecutions);
   }

    public void perform(String userPrefix, int assignments, int roleMemberships, int linkRefs, int operationExecutions) throws Exception {

        List<String> userOidsToGet = new ArrayList<>();

        for (int i = 0; i < USERS; i++) {
            userOidsToGet.add(createUser(userPrefix, i, assignments, roleMemberships, linkRefs, operationExecutions));
        }

        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("get", "Repository getObject() -> user");

        queryRecorder.startRecording();

        for (int i = 0; i < GETS_TO_MEASURE; i++) {
            var userOid = userOidsToGet.get(i % USERS);
            try (Split ignored = stopwatch.start()) {
                assertThat(repositoryService.getObject(
                        UserType.class, userOid, getOptions(), operationResult))
                        .isNotNull();
            }
            if (queryRecorder.isRecording()) {
                // stop does not clear entries, so it will not be started again above
                queryRecorder.stopRecording();
            }
        }
        queryRecorder.stopRecording();
    }

    private Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return getOptions;
    }

}
