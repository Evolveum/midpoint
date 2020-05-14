package com.evolveum.midpoint.rest.impl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Support for simple index page with REST API endpoints (HTML and JSON).
 */
@RestController
@RequestMapping("/rest2")
public class RestApiIndex {

    private final List<OperationInfo> uiRestInfo;

    public RestApiIndex(RequestMappingHandlerMapping handlerMapping) {
        uiRestInfo = operationInfoStream(handlerMapping)
                .filter(info -> info.handler.getBeanType().getName()
                        .startsWith("com.evolveum.midpoint.rest."))
                .collect(Collectors.toList());
    }

    private Stream<OperationInfo> operationInfoStream(RequestMappingHandlerMapping handlerMapping) {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .map(entry -> new OperationInfo(entry.getKey(), entry.getValue()));
    }

    @GetMapping()
    public List<OperationJson> index() {
        return uiRestInfo.stream()
                .flatMap(OperationInfo::operationJsonStream)
                .sorted(Comparator.comparing(json -> json.urlPattern))
                .collect(Collectors.toList());
    }

    @GetMapping(produces = "text/html")
    public String indexHtml() {
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html>"
                + "<head><meta charset='UTF-8'><title>REST-ish API</title>"
                + "<style>body {font-family: sans-serif;} form,li,p,h1 {margin: 0.4em;}"
                + " input,select {margin: 0 1em 0 0;} input {width:5em;}"
                + " #result {padding: 1em; border: solid thin red;}</style>"
                + "</head>"
                + "<body><h1>REST operations</h1>This is NOT Swagger! Click at your own risk!<ul>");
        for (OperationJson operationJson : index()) {
            html.append("<li>")
                    .append(Arrays.toString(operationJson.methods))
                    .append(" <a href=\"")
                    .append(operationJson.urlPattern)
                    .append("\">")
                    .append(operationJson.urlPattern)
                    .append("</a></li>");
        }
        return html.append("</ul></body>")
                .toString();
    }

    private static class OperationInfo {
        final RequestMappingInfo mappingInfo;
        final HandlerMethod handler;

        OperationInfo(RequestMappingInfo mappingInfo, HandlerMethod handler) {
            this.mappingInfo = mappingInfo;
            this.handler = handler;
        }

        Stream<OperationJson> operationJsonStream() {
            return mappingInfo.getPatternsCondition().getPatterns().stream()
                    .map(pattern -> new OperationJson(pattern,
                            mappingInfo.getMethodsCondition().getMethods(),
                            mappingInfo.getConsumesCondition().getConsumableMediaTypes(),
                            mappingInfo.getProducesCondition().getExpressions()));
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OperationJson {
        public final String urlPattern;
        public final String[] methods;
        public final String[] accepts;
        public final String[] produces;

        public OperationJson(String urlPattern,
                Set<RequestMethod> methods,
                Set<MediaType> accepts,
                Set<MediaTypeExpression> produces) {
            this.urlPattern = urlPattern;
            this.methods = toStringArray(methods);
            this.accepts = toStringArray(accepts);
            this.produces = toStringArray(produces);
        }

        private String[] toStringArray(Collection<?> collection) {
            return collection.isEmpty()
                    ? null
                    : collection.stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
        }
    }
}
