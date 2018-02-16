/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.apache.wicket.core.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.file.IResourceFinder;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointResourceStreamLocator extends ResourceStreamLocator {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointResourceStreamLocator.class);

    private static final String[] EXTENSIONS = {"js", "css", "html"};
    private static final String[] MIMIFIED_EXTENSIONS = {"js", "css"};

    public MidPointResourceStreamLocator(List<IResourceFinder> finders) {
        super(finders);
    }

    @Override
    public IResourceStream locate(Class<?> clazz, String path, String style, String variation, Locale locale, String extension, boolean strict) {
        IResourceStream stream = null;

        // If path contains a locale, then it'll replace the locale provided to this method
        ResourceUtils.PathLocale data = ResourceUtils.getLocaleFromFilename(path);
        if ((data != null) && (data.locale != null)) {
            path = data.path;
            locale = data.locale;
        }

        // Try the various combinations of style, locale and extension to find the resource.
        IResourceNameIterator iter = newResourceNameIterator(path, locale, style, variation,
                extension, strict);
        while (iter.hasNext()) {
            String newPath = iter.next();

            stream = locate(clazz, newPath);

            if (stream != null) {
                stream.setLocale(iter.getLocale());
                stream.setStyle(iter.getStyle());
                stream.setVariation(iter.getVariation());
                break;
            }
        }

        return stream;
    }

    @Override
    public IResourceNameIterator newResourceNameIterator(String path, Locale locale, String style, String variation, String extension, boolean strict) {
        String pathWithoutExtension = path;
        String ext = extension;
        if (ext == null && path != null) {
            String[] array = path.split("\\.");
            ext = array.length > 1 ? array[array.length - 1] : null;

            int extLength = ext != null ? ext.length() + 1 : 0;
            pathWithoutExtension = StringUtils.left(path, path.length() - extLength);
        }
        if (!containsIgnoreCase(EXTENSIONS, ext)) {
            return super.newResourceNameIterator(path, locale, style, variation, extension, strict);
        }

        List<String> extensions = new ArrayList<>();

        if (containsIgnoreCase(MIMIFIED_EXTENSIONS, ext)
                && Application.exists() && Application.get().getResourceSettings().getUseMinifiedResources()
                && !pathWithoutExtension.endsWith(".min")) {
            extensions.add("min." + ext);
        }

        extensions.add(ext);

        return new SimpleResourceNameIterator(pathWithoutExtension, extensions);
    }

    private boolean containsIgnoreCase(String[] array, String item) {
        for (String ext : array) {
            if (ext.equalsIgnoreCase(item)) {
                return true;
            }
        }

        return false;
    }

    private static class SimpleResourceNameIterator implements IResourceNameIterator {

        private String path;
        private Iterator<String> extensions;

        private String current;

        public SimpleResourceNameIterator(String path, List<String> extensions) {
            this.path = path;
            this.extensions = extensions.iterator();
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public String getStyle() {
            return null;
        }

        @Override
        public String getVariation() {
            return null;
        }

        @Override
        public String getExtension() {
            return current;
        }

        @Override
        public boolean hasNext() {
            return extensions.hasNext();
        }

        @Override
        public String next() {
            current = extensions.next();

            String ext = current;
            if (ext != null) {
                if (ext.startsWith(".")) {
                    ext = ext.substring(1);
                }
            }

            return path + "." + ext;
        }
    }
}
