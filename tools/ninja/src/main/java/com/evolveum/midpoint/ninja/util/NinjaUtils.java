/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.util;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaUtils {

    public static final String XML_OBJECTS_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<c:objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:org=\"http://midpoint.evolveum.com/xml/ns/public/common/org-3\">\n";

    public static final String XML_OBJECTS_SUFFIX = "</c:objects>";

    public static final String JSON_OBJECTS_PREFIX = "[\n";

    public static final String JSON_OBJECTS_SUFFIX = "\n]";

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public static final long COUNT_STATUS_LOG_INTERVAL = 2 * 1000; // two seconds

    public static final long WAIT_FOR_EXECUTOR_FINISH = 365;

    public static JCommander setupCommandLineParser() {
        BaseOptions base = new BaseOptions();
        ConnectionOptions connection = new ConnectionOptions();

        JCommander.Builder builder = JCommander.newBuilder()
                .expandAtSign(false)
                .addObject(base)
                .addObject(connection);

        for (Command cmd : Command.values()) {
            builder.addCommand(cmd.getCommandName(), cmd.createOptions());
        }

        JCommander jc = builder.build();
        jc.setProgramName("java [-Dloader.path=<jdbc_driver_jar_path>] -jar ninja.jar");
        jc.setColumnSize(110);
        jc.setAtFileCharset(Charset.forName(base.getCharset()));

        return jc;
    }

    public static <T> T getOptions(List<Object> options, Class<T> type) {
        for (Object object : options) {
            if (type.equals(object.getClass())) {
                //noinspection unchecked
                return (T) object;
            }
        }

        return null;
    }

    public static ObjectFilter createObjectFilter(FileReference strFilter, NinjaContext context, Class<? extends Containerable> objectClass)
            throws IOException, SchemaException {
        ObjectQuery query = createObjectQuery(strFilter, context, objectClass);
        return query != null ? query.getFilter() : null;
    }

    public static ObjectQuery createObjectQuery(FileReference ref, NinjaContext context, Class<? extends Containerable> objectClass)
            throws IOException, SchemaException {

        if (ref == null) {
            return null;
        }

        String filterStr = ref.getValue();
        if (ref.getReference() != null) {
            File file = ref.getReference();
            filterStr = FileUtils.readFileToString(file, context.getCharset());
        }

        PrismContext prismContext = context.getPrismContext();
        // Experimental Axiom filter support, % is chosen as a marker and will be skipped.
        if (filterStr.startsWith("%")) {
            ObjectFilter objectFilter = prismContext.createQueryParser().parseFilter(objectClass, filterStr.substring(1));
            return prismContext.queryFactory().createQuery(objectFilter);
        } else {
            PrismParserNoIO parser = prismContext.parserFor(filterStr);
            RootXNode root = parser.parseToXNode();

            ObjectFilter filter = context.getQueryConverter().parseFilter(root.toMapXNode(), objectClass);
            return prismContext.queryFactory().createQuery(filter);
        }
    }

    public static String printStackToString(Exception ex) {
        if (ex == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    public static Writer createWriter(File output, Charset charset, boolean zip, boolean overwrite) throws IOException {
        OutputStream os;
        if (output != null) {
            if (!overwrite && output.exists()) {
                throw new NinjaException("Export file '" + output.getPath() + "' already exists");
            }
            output.createNewFile();

            os = new FileOutputStream(output);
        } else {
            os = System.out;
        }

        if (zip) {
            ZipOutputStream zos = new ZipOutputStream(os);

            String entryName = output != null ? output.getName().replaceAll("\\.", "-") + ".xml" : "objects.xml";
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);

            os = zos;
        }

        return new OutputStreamWriter(os, charset);
    }

    public static GetOperationOptionsBuilder addIncludeOptionsForExport(GetOperationOptionsBuilder optionsBuilder,
            Class<? extends Containerable> type) {
        // todo fix this brutal hack (related to checking whether to include particular options)
        boolean all = type == null
                || Objectable.class.equals(type)
                || com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(type)
                || ObjectType.class.equals(type);

        if (all || UserType.class.isAssignableFrom(type)) {
            optionsBuilder = optionsBuilder.item(UserType.F_JPEG_PHOTO).retrieve();
        }
        if (all || LookupTableType.class.isAssignableFrom(type)) {
            optionsBuilder = optionsBuilder.item(LookupTableType.F_ROW)
                    .retrieveQuery().asc(PrismConstants.T_ID).end();
        }
        if (all || AccessCertificationCampaignType.class.isAssignableFrom(type)) {
            optionsBuilder = optionsBuilder.item(AccessCertificationCampaignType.F_CASE).retrieve();
        }
        return optionsBuilder;
    }

    public static List<ObjectTypes> getTypes(Set<ObjectTypes> selected) {
        List<ObjectTypes> types = new ArrayList<>();

        if (selected != null && !selected.isEmpty()) {
            types.addAll(selected);
        } else {
            for (ObjectTypes type : ObjectTypes.values()) {
                Class<? extends ObjectType> clazz = type.getClassDefinition();
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                types.add(type);
            }
        }

        Collections.sort(types);

        return types;
    }

    public static File computeInstallationDirectory(File installationDirectory, NinjaContext context) {
        final ConnectionOptions connectionOptions = context.getOptions(ConnectionOptions.class);
        File midpointHomeDirectory = new File(connectionOptions.getMidpointHome());

        return computeInstallationDirectory(installationDirectory, midpointHomeDirectory);
    }

    public static File computeInstallationDirectory(File installationDirectory, File midpointHomeDirectory) {
        if (installationDirectory != null) {
            return installationDirectory;
        }

        return midpointHomeDirectory.getParentFile();
    }
}
