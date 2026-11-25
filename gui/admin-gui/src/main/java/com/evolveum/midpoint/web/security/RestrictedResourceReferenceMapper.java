/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.security;

import java.util.List;
import java.util.function.Supplier;

import org.apache.wicket.Application;
import org.apache.wicket.core.request.mapper.BasicResourceReferenceMapper;
import org.apache.wicket.core.request.mapper.ResourceReferenceMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.mapper.ICompoundRequestMapper;
import org.apache.wicket.request.mapper.ParentPathReferenceRewriter;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.caching.IResourceCachingStrategy;
import org.apache.wicket.util.lang.Packages;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Replacement for Wicket's package resource mapper (wicket/resource/[class]/[file]) that serves
 * only resources of known UI libraries.
 *
 * The stock mapper accepts any class on the classpath as a scope and allows to walk up from its package,
 * which makes the whole classpath readable (for allowed file types) and lets unauthenticated clients
 * find out which classes and files exist. Here requests with other scope, resolving to a file outside
 * of the allowed locations or failing during lookup are not handled at all, so they are answered exactly
 * as any unknown path.
 */
public class RestrictedResourceReferenceMapper extends ParentPathReferenceRewriter {

    private static final Trace LOGGER = TraceManager.getTrace(RestrictedResourceReferenceMapper.class);

    /** Packages of classes that UI libraries use as scope of their package resources. */
    private static final List<String> ALLOWED_SCOPE_PREFIXES = List.of(
            "org.apache.wicket.",
            "jp.try0.wicket.honeypot.",
            // chart.js, taken from webjar by walking up from the panel package
            "com.evolveum.wicket.chartjs.",
            // webjars references, used by select2 for its language files
            "de.agilecoders.wicket.webjars.");

    /**
     * Classpath locations that may be served. It is the resolved location that is checked, so it does not
     * matter whether the request names the file directly or walks up from the scope package.
     */
    private static final List<String> ALLOWED_PATH_PREFIXES = List.of(
            "org/apache/wicket/",
            "jp/try0/wicket/honeypot/",
            "webjars/chartjs/",
            // webjars references are requested relative to the package of the reference class
            "de/agilecoders/wicket/webjars/request/resource/webjars/select2/");

    public RestrictedResourceReferenceMapper(
            Supplier<String> parentFolderPlaceholder, Supplier<IResourceCachingStrategy> cachingStrategy) {
        super(new RestrictedBasicMapper(cachingStrategy), parentFolderPlaceholder);
    }

    /**
     * Replaces the stock package resource mapper of the application with the restricted one.
     */
    public static void install(Application application) {
        ICompoundRequestMapper root = application.getRootRequestMapperAsCompound();

        IRequestMapper stock = null;
        for (IRequestMapper mapper : root) {
            if (mapper instanceof ResourceReferenceMapper) {
                stock = mapper;
                break;
            }
        }
        if (stock == null) {
            throw new IllegalStateException("Wicket package resource mapper not found, can't restrict it");
        }

        root.remove(stock);
        root.add(new RestrictedResourceReferenceMapper(
                () -> application.getResourceSettings().getParentFolderPlaceholder(),
                () -> application.getResourceSettings().getCachingStrategy()));
    }

    private static class RestrictedBasicMapper extends BasicResourceReferenceMapper {

        private RestrictedBasicMapper(Supplier<IResourceCachingStrategy> cachingStrategy) {
            super(new PageParametersEncoder(), cachingStrategy);
        }

        @Override
        public IRequestHandler mapRequest(Request request) {
            try {
                IRequestHandler handler = super.mapRequest(request);
                return isServable(handler) ? handler : null;
            } catch (RuntimeException ex) {
                LOGGER.debug("Couldn't map package resource request {}, not handling it", request.getUrl(), ex);
                return null;
            }
        }

        /**
         * Only files from allowed locations are served. Files denied by the package resource guard
         * would fail later while being served, which would make them distinguishable from the missing ones.
         */
        private boolean isServable(IRequestHandler handler) {
            if (!(handler instanceof ResourceReferenceRequestHandler resourceHandler)) {
                return handler != null;
            }
            ResourceReference reference = resourceHandler.getResourceReference();
            if (!(reference instanceof PackageResourceReference)) {
                return true;
            }
            String path = Packages.absolutePath(reference.getScope(), reference.getName());
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (ALLOWED_PATH_PREFIXES.stream().noneMatch(path::startsWith)) {
                return false;
            }
            return Application.get().getResourceSettings().getPackageResourceGuard().accept(path);
        }

        @Override
        protected Class<?> resolveClass(String name) {
            if (name == null || ALLOWED_SCOPE_PREFIXES.stream().noneMatch(name::startsWith)) {
                return null;
            }
            return super.resolveClass(name);
        }
    }
}
