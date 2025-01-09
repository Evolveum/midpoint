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
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.impl.*;
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

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaUtils {

    public static final Pattern PATTERN = Pattern.compile("\\{}");

    public static final String XML_OBJECTS_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<c:objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:org=\"http://midpoint.evolveum.com/xml/ns/public/common/org-3\">\n";

    public static final String XML_OBJECTS_SUFFIX = "</c:objects>\n";

    public static final String XML_DELTAS_PREFIX = "<deltas "
            + "xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/api-types-3\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:type=\"ObjectDeltaListType\">\n";

    public static final String XML_DELTAS_SUFFIX = "</deltas>\n";

    public static final QName DELTA_LIST_DELTA = new QName("http://midpoint.evolveum.com/xml/ns/public/common/api-types-3", "delta");

    public static final String JSON_OBJECTS_PREFIX = "[\n";

    public static final String JSON_OBJECTS_SUFFIX = "\n]";

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public static final long COUNT_STATUS_LOG_INTERVAL = 500;

    public static final long WAIT_FOR_EXECUTOR_FINISH = 365;

    public static final String XML_EXTENSION = "xml";

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
        jc.setUsageFormatter(new NinjaUsageFormatter(jc));

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

    public static Writer createWriter(File output, Charset charset, boolean zip, boolean overwrite, PrintStream defaultOutput) throws IOException {
        OutputStream os;
        if (output != null) {
            if (!overwrite && output.exists()) {
                throw new NinjaException("Export file '" + output.getPath() + "' already exists");
            }
            output.createNewFile();

            os = new FileOutputStream(output);
        } else {
            os = defaultOutput;
        }

        if (zip) {
            ZipOutputStream zos = new ZipOutputStream(os);

            String entryName = createZipEntryName(output);
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);

            os = zos;
        }

        return new OutputStreamWriter(os, charset);
    }

    private static String createZipEntryName(File file) {
        if (file == null) {
            return "output";
        }

        String fullName = file.getName();
        String name = FilenameUtils.removeExtension(fullName).replaceAll("\\.", "-");

        return name + "." + XML_EXTENSION ;
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

    public static File computeInstallationDirectory(File installationDirectory, NinjaContext context) throws NinjaException {
        final ConnectionOptions connectionOptions = context.getOptions(ConnectionOptions.class);
        String mpHome = connectionOptions.getMidpointHome();
        File midpointHomeDirectory = mpHome != null ? new File(connectionOptions.getMidpointHome()) : null;

        return computeInstallationDirectory(installationDirectory, midpointHomeDirectory);
    }

    public static File computeInstallationDirectory(File installationDirectory, File midpointHomeDirectory) throws NinjaException {
        File installation;
        if (installationDirectory != null) {
            installation = installationDirectory;
        } else {
            if (midpointHomeDirectory != null) {
                installation = midpointHomeDirectory.getParentFile();
            } else {
                throw new NinjaException("Neither installation directory nor midpoint.home is specified");
            }
        }

        File bin = new File(installation, "bin");
        if (!validateDirectory(bin, "midpoint\\.(sh|bat)", "start\\.(sh|bat)", "stop\\.(sh|bat)")) {
            throw new NinjaException("Installation directory " + installation.getPath() + " doesn't contain bin/ with midpoint, "
                    + "start, and stop scripts. Probably wrong installation path, or customized installation.");
        }

        File lib = new File(installation, "lib");
        if (!validateDirectory(lib, "midpoint\\.(war|jar)")) {
            throw new NinjaException("Installation directory " + installation.getPath() + " doesn't contain lib/ with midpoint "
                    + "(jar/war). Probably wrong installation path, or customized installation.");
        }

        return installation;
    }

    private static boolean validateDirectory(File dir, String... expectedFileNames) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        String[] fileNames = dir.list();
        for (String fileName : expectedFileNames) {
            if (!containsFileNames(fileNames, fileName)) {
                return false;
            }
        }

        return true;
    }

    private static boolean containsFileNames(String[] names, String filenameRegex) {
        if (names == null) {
            return false;
        }

        return Arrays.stream(names).anyMatch(s -> s.matches(filenameRegex));
    }

    public static String readInput(Log log, Function<String, Boolean> inputValidation) {
        log.logRaw(ConsoleFormat.formatInputPrompt());
        boolean first = true;

        String line = null;
        // we don't want to close this input stream (stdin), we didn't open it.
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            boolean accepted = false;
            while (!accepted) {
                if (!first) {
                    log.error("Invalid input, please try again");
                    log.logRaw(ConsoleFormat.formatInputPrompt());
                }
                first = false;

                line = br.readLine();

                accepted = inputValidation.apply(line);
            }
        } catch (IOException ex) {
            log.error("Error occurred while reading input from stdin", ex);
        }

        return line;
    }

    public static String printFormatted(String message, Object... args) {
        Matcher matcher = PATTERN.matcher(message);

        StringBuilder sb = new StringBuilder();

        int i = 0;
        while (matcher.find()) {
            Object arg = args[i++];
            if (arg == null) {
                arg = "null";
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(arg.toString()));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static String createHelp(JCommander jc, String parsedCommand) {
        StringBuilder sb = new StringBuilder();

        IUsageFormatter formatter = jc.getUsageFormatter();
        if (parsedCommand != null) {
            formatter.usage(parsedCommand, sb);
        } else {
            formatter.usage(sb);
        }

        String helpText = sb.toString();
        helpText = helpText.replace(Command.GENERATE_RBAC_DATA.getCommandName(), "");

        return helpText;
    }

    public static String printObjectNameOidAndType(PrismObject<?> object) {
        if (object == null) {
            return null;
        }

        return printFormatted("{} ({}, {})", object.getName(), object.getOid(), object.toDebugType());
    }
}
