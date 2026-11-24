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
 * find out which classes and files exist. Here requests with other scope, with parent folder references
 * or failing during lookup are not handled at all, so they are answered exactly as any unknown path.
 */
public class RestrictedResourceReferenceMapper extends ParentPathReferenceRewriter {

    private static final Trace LOGGER = TraceManager.getTrace(RestrictedResourceReferenceMapper.class);

    /** Packages of libraries that serve their scripts, styles and images as package resources. */
    private static final List<String> ALLOWED_SCOPE_PREFIXES = List.of(
            "org.apache.wicket.",
            "jp.try0.wicket.honeypot.");

    private static final String PARENT_FOLDER = "..";

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
            if (request.getUrl().getSegments().contains(PARENT_FOLDER)) {
                return null;
            }
            try {
                IRequestHandler handler = super.mapRequest(request);
                return isServable(handler) ? handler : null;
            } catch (RuntimeException ex) {
                LOGGER.debug("Couldn't map package resource request {}, not handling it", request.getUrl(), ex);
                return null;
            }
        }

        /**
         * Files denied by the package resource guard would fail later while being served,
         * which would make them distinguishable from the missing ones.
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
