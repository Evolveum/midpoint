/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.impl.DefaultReferencableImpl;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.CheckedConsumer;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;

/**
 * Tests prism serialization of various objects.
 * (This is, in fact, a simplification of complex parse-serialize-parse tests.)
 */
public class TestPrismSerialization extends AbstractSchemaTest {

    private static final File TEST_DIR = new File("src/test/resources/serialization");

    private static final File ROLE_WITH_RAW_PATH_FILE = new File(TEST_DIR, "role-with-raw-path.xml");

    @Test
    public void testSerializeTrace() throws Exception {
        PrismContext prismContext = getPrismContext();
        ValueTransformationTraceType trace = new ValueTransformationTraceType(prismContext);
        NamedValueType input1 = new NamedValueType();
        input1.setName(new QName("myRef"));
        input1.setValue(new ObjectReferenceType().oid("123").type(UserType.COMPLEX_TYPE));
        trace.getInput().add(input1);
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "trace");
        String xml = prismContext.xmlSerializer().serializeAnyData(trace, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeDeltaSetTripleType() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismReferenceValue refValue = new PrismReferenceValueImpl("123456");
        DefaultReferencableImpl referencable = new DefaultReferencableImpl(refValue);
        DeltaSetTripleType triple = new DeltaSetTripleType();
        triple.getPlus().add(referencable);

        QName fakeQName = new QName(PrismConstants.NS_TYPES, "triple");
        String xml = prismContext.xmlSerializer().serializeAnyData(triple, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeNamedReference() throws Exception {
        PrismContext prismContext = getPrismContext();
        ObjectReferenceType reference = new ObjectReferenceType()
                .oid("66662a3b-76eb-4465-8374-742f6e2f54b4")
                .type(UserType.COMPLEX_TYPE)
                .targetName("joe");
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "ref");
        String xml = prismContext.xmlSerializer().serializeAnyData(reference, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeFullReference() throws Exception {
        PrismContext prismContext = getPrismContext();

        QName fakeQName = new QName(PrismConstants.NS_TYPES, "ref");
        MutablePrismReferenceDefinition definition = prismContext.definitionFactory().createReferenceDefinition(fakeQName, ObjectReferenceType.COMPLEX_TYPE);
        definition.setComposite(true);
        PrismReference reference = definition.instantiate();

        UserType joe = new UserType(prismContext)
                .name("joe")
                .oid("66662a3b-76eb-4465-8374-742f6e2f54b4")
                .description("description");
        ObjectReferenceType referenceRealValue = ObjectTypeUtil.createObjectRefWithFullObject(joe);
        reference.add(referenceRealValue.asReferenceValue());

        String xml = prismContext.xmlSerializer().serialize(reference);
        System.out.println(xml);
    }

    @Test
    public void testSerializeMessage() throws Exception {
        PrismContext prismContext = getPrismContext();
        SingleLocalizableMessage localizableMessage = new SingleLocalizableMessage("execute.reset.credential.bad.request", null, "Failed to execute reset password. Bad request.");
        LocalizableMessageType localizableMessageBean = LocalizationUtil.createLocalizableMessageType(localizableMessage);
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");
        String xml = prismContext.xmlSerializer().serializeAnyData(localizableMessageBean, fakeQName);
        System.out.println(xml);
    }

    @Test
    public void testSerializeExecuteCredentialResetResponseType() throws Exception {
        PrismContext prismContext = getPrismContext();

        SingleLocalizableMessage localizableMessage = new SingleLocalizableMessage("execute.reset.credential.bad.request", null, "Failed to execute reset password. Bad request.");
        LocalizableMessageType localizableMessageBean = LocalizationUtil.createLocalizableMessageType(localizableMessage);
        ExecuteCredentialResetResponseType response = new ExecuteCredentialResetResponseType();
        response.setMessage(localizableMessageBean);
        QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");

        prismContext.adopt(response);

        String xml = prismContext.xmlSerializer().serializeAnyData(response, fakeQName);
        System.out.println(xml);
    }

    /**
     * See MID-6320.
     */
    @Test
    public void testSerializeObjectCollectionWithFilter() throws Exception {
        PrismContext prismContext = getPrismContext();
        ObjectCollectionType collection = new ObjectCollectionType(prismContext)
                .name("collection")
                .type(UserType.COMPLEX_TYPE);

        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .item(UserType.F_COST_CENTER).eq("100")
                .buildFilter();
        SearchFilterType filterBean = prismContext.getQueryConverter().createSearchFilterType(filter);

        List<ItemDelta<?, ?>> deltas2 = prismContext.deltaFor(ObjectCollectionType.class)
                .item(ObjectCollectionType.F_DESCRIPTION).replace("description")
                .item(ObjectCollectionType.F_FILTER).replace(filterBean)
                .asItemDeltas();

        ItemDeltaCollectionsUtil.applyTo(deltas2, collection.asPrismObject());
        checkRoundTrip(collection.asPrismObject(), PrismContext.LANG_XML, null);
        checkRoundTrip(collection.asPrismObject(), PrismContext.LANG_JSON, null);
        checkRoundTrip(collection.asPrismObject(), PrismContext.LANG_YAML, null);
    }

    /**
     * Another issue with item path serialization. If the namespace prefix is defined in upper
     * parts of the DOM tree, serializer does not see it.
     *
     * MID-6321
     */
    @Test(enabled = false)
    public void testSerializeRawItemPathWithPrefix() throws Exception {
        PrismContext prismContext = getPrismContext();

        PrismObject<RoleType> role = prismContext.parserFor(ROLE_WITH_RAW_PATH_FILE).parse();

        displayValue("Role", role);
        SearchFilterType filterBean = role.asObjectable().getAssignment().get(0).getTargetRef().getFilter();
        displayValue("Filter", filterBean);

        checkRoundTrip(role, PrismContext.LANG_XML, this::checkPath);
        checkRoundTrip(role, PrismContext.LANG_JSON, this::checkPath);
        checkRoundTrip(role, PrismContext.LANG_YAML, this::checkPath);
    }

    private void checkPath(PrismObject<?> role) throws SchemaException {
        SearchFilterType filterBean = ((RoleType) role.asObjectable()).getAssignment().get(0).getTargetRef().getFilter();
        ObjectFilter filter = getPrismContext().getQueryConverter().parseFilter(filterBean, RoleType.class);
        System.out.println("Filter: " + filter);
    }

    private void checkRoundTrip(PrismObject<?> prismObject, String language, CheckedConsumer<PrismObject<?>> consumer) throws CommonException {
        PrismContext prismContext = getPrismContext();

        String serialized = prismContext.serializerFor(language).serialize(prismObject);
        displayValue("Serialized to " + language, serialized);

        PrismObject<?> reparsed = prismContext.parserFor(serialized).language(language).parse();
        displayValue("Re-parsed", reparsed);

        if (consumer != null) {
            consumer.accept(reparsed);
        }
    }
}
