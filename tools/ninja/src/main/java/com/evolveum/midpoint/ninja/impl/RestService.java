package com.evolveum.midpoint.ninja.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.ninja.util.BasicAuthenticationInterceptor;
import com.evolveum.midpoint.ninja.util.LoggingInterceptor;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.ProxyCreator;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RestService {

    private RestTemplate template;

    private PrismContext prismContext;

    private String url;

    public RestService(String url, String username, String password) {
        this.url = url;

        template = new RestTemplate();

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

        if (username != null) {
            interceptors.add(new BasicAuthenticationInterceptor(username, password));
        }

        interceptors.add(new LoggingInterceptor());

        template.setRequestFactory(
                new InterceptingClientHttpRequestFactory(template.getRequestFactory(), interceptors));

        prismContext = ProxyCreator.getProxy(PrismContext.class, () -> {

            try {
                PrismContextFactory factory = new MidPointPrismContextFactory() {

                    @Override
                    protected void registerExtensionSchemas(SchemaRegistryImpl schemaRegistry)
                            throws SchemaException, FileNotFoundException {
                        super.registerExtensionSchemas(schemaRegistry);

                        RestService.this.registerExtensionSchemas(schemaRegistry);
                    }
                };

                return factory.createPrismContext();
            } catch (SchemaException | FileNotFoundException ex) {
                throw new NinjaException("Couldn't load prism context", ex);
            }
        });
    }

    private void registerExtensionSchemas(SchemaRegistryImpl schemaRegistry) {
        try {
            List<String> schemas = getSchemaList();
            for (String name : schemas) {
                String xsd = fetchSchema(name);

                ByteArrayInputStream bis = new ByteArrayInputStream(xsd.getBytes());
                schemaRegistry.registerPrismSchema(bis, "rest " + name);
            }
        } catch (Exception ex) {
            throw new NinjaException("Couldn't register extension schema", ex);
        }
    }

    private String fetchSchema(String name) {
        return template.getForObject(url + "/ws/schema/{name}", String.class, name);
    }

    private List<String> getSchemaList() {
        String result = template.getForObject(url + "/ws/schema", null, String.class);
        if (result == null) {
            return Collections.emptyList();
        }

        String[] array = result.split("\n");
        return Arrays.asList(array);
    }

    public <T extends ObjectType> void delete(QName type, String oid, List<String> options) {
        Validate.notNull(type, "Object type must not be null");
        Validate.notNull(oid, "Oid must not be null");

        if (options == null) {
            options = new ArrayList<>();
        }

        ObjectTypes object = ObjectTypes.getObjectTypeFromTypeQName(type);

        ResponseEntity<String> resp = template.execute(url + "/{type}/{oid}", HttpMethod.DELETE, null,
                null,
                object.getRestType(), oid);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            OperationResult result = NinjaUtils.parseResult(resp.getBody());
            throw new RestServiceException("Couldn't delete object '" + type.getLocalPart() + "', with oid: '"
                    + oid + "'", result);
        }
    }

    public <T extends ObjectType> PrismObject<T> get(QName type, String oid) {
        return null;
    }

    public <T extends ObjectType> List<PrismObject<T>> search() {
        return null;
    }

    public void modify() {

    }

    public OperationResult testResource(String oid) {
        ResponseEntity<String> resp = template.execute(url + "/resources/{oid}/test", HttpMethod.POST, null,
                null, oid);

        String body = resp.getBody();

        //todo return result

        return null;
    }
}
