/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Support for simple index page with REST API endpoints (HTML and JSON).
 */
@RestController
@RequestMapping({ "/ws", "/rest", "/api" })
public class RestApiIndex extends AbstractRestController {

    private final String contextPath;
    private final List<OperationInfo> uiRestInfo;

    public RestApiIndex(
            // The Qualifier is needed since Boot 2.7 where two candidates became available.
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ServletContext servletContext) {
        contextPath = servletContext.getContextPath();
        uiRestInfo = operationInfoStream(handlerMapping)
                .filter(info -> info.handler.getBeanType().getName()
                        .startsWith("com.evolveum.midpoint.rest."))
                .collect(Collectors.toList());
    }

    private Stream<OperationInfo> operationInfoStream(
            RequestMappingHandlerMapping handlerMapping) {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .map(entry -> new OperationInfo(entry.getKey(), entry.getValue()));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OperationJson> index(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uiRestInfo.stream()
                .flatMap(operationInfo -> operationInfo.operationJsonStream(contextPath, uri))
                .sorted(Comparator.comparing(json -> json.urlPattern))
                .collect(Collectors.toList());
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String indexHtml(HttpServletRequest request) {
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html>"
                + "<head><meta charset='UTF-8'><title>REST-ish API</title>"
                + "<style>body {font-family: sans-serif;} form,li,p,h1 {margin: 0.4em;}"
                + " input,select {margin: 0 1em 0 0;} input {width:5em;}"
                + " #result {padding: 1em; border: solid thin red;}</style>"
                + "</head>"
                + "<body><h1>REST operations</h1>This is NOT Swagger! Click at your own risk!<ul>");
        for (OperationJson operationJson : index(request)) {
            html.append("<li>")
                    .append(operationJson.methods != null
                            ? Arrays.toString(operationJson.methods)
                            : "*")
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

        Stream<OperationJson> operationJsonStream(String contextPath, String prefix) {
            return mappingInfo.getPatternsCondition().getPatterns().stream()
                    .map(pattern -> new OperationJson(contextPath + pattern,
                            mappingInfo.getMethodsCondition().getMethods(),
                            mappingInfo.getConsumesCondition().getConsumableMediaTypes(),
                            mappingInfo.getProducesCondition().getExpressions()))
                    .filter(operationJson -> operationJson.urlPattern.startsWith(prefix));
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

    // Fallback for all unmapped resources under /ws
    // TODO this currently causes 404 instead of 406 when the path is right but Accept is not.
    // Currently I don't know how to make it right. Without this method 404 show Wicket error.
    // Not sure what is worse, really.
    // 404 does not produce any exception, regardless of spring.mvc.throw-exception-if-no-handler-found setting,
    // so it's not possible to handle it with RestExceptionHandler either.
    // The reason is that ResourceHandlerRegistry takes over / and 404 comes from there.
    // It is not much "handler not found" because the resource handler is found,
    // even for paths under /api (/ws, /rest) that are not otherwise mapped.
    @RequestMapping(value = "/**")
    public void notFoundFallback() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    // DEBUG area
    @Autowired(required = false)
    private List<WebMvcConfigurer> configurers;

    @Autowired
    private List<HttpMessageConverter<?>> converters;

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @GetMapping("/config")
    public Map<?, ?> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configurers", toStrings(configurers));
        result.put("converters", toStrings(converters));
        result.put("requestMappingHandlerAdapter-converters",
                toStrings(requestMappingHandlerAdapter.getMessageConverters()));
        return result;
    }

    private Object toStrings(Collection<?> collection) {
        return collection != null
                ? collection.stream().map(Object::toString).collect(Collectors.toList())
                : "N/A";
    }
}
