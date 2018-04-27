package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParserNoIO;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaUtils {

    public static final String XML_OBJECTS_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<c:objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:org=\"http://midpoint.evolveum.com/xml/ns/public/common/org-3\">\n";

    public static final String XML_OBJECTS_SUFFIX = "</c:objects>";

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(".##");

    public static final long COUNT_STATUS_LOG_INTERVAL = 2 * 1000; // two seconds

    public static final long WAIT_FOR_EXECUTOR_FINISH = 365;

    public static JCommander setupCommandLineParser() {
        BaseOptions base = new BaseOptions();
        ConnectionOptions connection = new ConnectionOptions();

        JCommander.Builder builder = JCommander.newBuilder()
                .addObject(base)
                .addObject(connection);

        for (Command cmd : Command.values()) {
            builder.addCommand(cmd.getCommandName(), cmd.createOptions());
        }

        JCommander jc = builder.build();
        jc.setProgramName("java [-Dloader.path=<jdbc_driver_jar_path>] -jar ninja.jar");
        jc.setColumnSize(150);
        jc.setAtFileCharset(Charset.forName(base.getCharset()));

        return jc;
    }

    public static <T> T getOptions(JCommander jc, Class<T> type) {
        List<Object> objects = jc.getObjects();
        for (Object object : objects) {
            if (type.equals(object.getClass())) {
                return (T) object;
            }
        }

        return null;
    }

    public static ObjectFilter createObjectFilter(FileReference strFilter, NinjaContext context)
            throws IOException, SchemaException {
        ObjectQuery query = createObjectQuery(strFilter, context);
        return query != null ? query.getFilter() : null;
    }

    public static ObjectQuery createObjectQuery(FileReference ref, NinjaContext context)
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
        PrismParserNoIO parser = prismContext.parserFor(filterStr);
        RootXNode root = parser.parseToXNode();

        ObjectFilter filter = QueryConvertor.parseFilter(root.toMapXNode(), prismContext);
        return ObjectQuery.createObjectQuery(filter);
    }

    public static String printStackToString(Exception ex) {
        if (ex == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    public static OperationResult parseResult(String result) {
        if (result == null) {
            return null;
        }

        //todo implement

        return null;
    }

    public static Writer createWriter(File output, Charset charset, boolean zip) throws IOException {
        OutputStream os;
        if (output != null) {
            if (output.exists()) {
                throw new NinjaException("Export file '" + output.getPath() + "' already exists");
            }
            output.createNewFile();

            os = new FileOutputStream(output);
        } else {
            os = System.out;
        }

        if (zip) {
            os = new ZipOutputStream(os);
        }

        return new OutputStreamWriter(os, charset);
    }

    public static void addIncludeOptionsForExport(Collection<SelectorOptions<GetOperationOptions>> options,
                                                  Class<? extends ObjectType> type) {
        // todo fix this brutal hack (related to checking whether to include particular options)
        boolean all = type == null
                || Objectable.class.equals(type)
                || com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(type)
                || ObjectType.class.equals(type);

        if (all || UserType.class.isAssignableFrom(type)) {
            options.add(SelectorOptions.create(UserType.F_JPEG_PHOTO,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        }
        if (all || LookupTableType.class.isAssignableFrom(type)) {
            options.add(SelectorOptions.create(LookupTableType.F_ROW,
                    GetOperationOptions.createRetrieve(
                            new RelationalValueSearchQuery(
                                    ObjectPaging.createPaging(PrismConstants.T_ID, OrderDirection.ASCENDING)))));
        }
        if (all || AccessCertificationCampaignType.class.isAssignableFrom(type)) {
            options.add(SelectorOptions.create(AccessCertificationCampaignType.F_CASE,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        }
    }

    public static List<ObjectTypes> getTypes(ObjectTypes selected) {
        List<ObjectTypes> types = new ArrayList<>();

        if (selected != null) {
            types.add(selected);
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
}
