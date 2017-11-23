package com.evolveum.midpoint.ninja.util;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParserNoIO;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaUtils {

    public static final String XML_OBJECTS_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<c:objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"\n" +
            "\txmlns:org=\"http://midpoint.evolveum.com/xml/ns/public/common/org-3\">\n";

    public static final String XML_OBJECTS_SUFFIX = "</c:objects>";

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
        jc.setProgramName("java [-cp <jdbc_driver_jar>] -jar ninja.jar");
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

    public static ObjectFilter createObjectFilter(FileReference strFilter, PrismContext prismContext)
            throws IOException, SchemaException {
        ObjectQuery query = createObjectQuery(strFilter, prismContext);
        return query != null ? query.getFilter() : null;
    }

    public static ObjectQuery createObjectQuery(FileReference ref, PrismContext context)
            throws IOException, SchemaException {

        if (ref == null) {
            return null;
        }

        String filterStr = ref.getValue();
        if (ref.getReference() != null) {
            File file = ref.getReference();
            filterStr = FileUtils.readFileToString(file);
        }

        PrismParserNoIO parser = context.parserFor(filterStr);
        RootXNode root = parser.parseToXNode();

        ObjectFilter filter = QueryConvertor.parseFilter(root.toMapXNode(), context);
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
}
