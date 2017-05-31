/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.ninja.action;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.cli.common.ToolsUtils;
import com.evolveum.midpoint.cli.ninja.command.Export;
import com.evolveum.midpoint.cli.ninja.util.LogWriter;
import com.evolveum.midpoint.cli.ninja.util.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipOutputStream;

/**
 * @author lazyman
 */
public class ExportAction extends Action<Export> {

    private static final int SEARCH_PAGE_SIZE = 100;

    public ExportAction(Export params, JCommander commander) {
        super(params, commander);
    }

    @Override
    protected void executeAction() throws Exception {
        ModelPortType port = createModelPort();

        Writer writer = null;
        try {
            writer = createWriter();
            writeHeader(writer);

            if (StringUtils.isNotEmpty(getParams().getOid())) {
                //get
                executeGet(port, writer);
            } else {
                //search
                executeSearch(port, writer);
            }

            writeFooter(writer);
            writer.flush();
        } catch (SAXException | JAXBException | IOException | FaultMessage ex) {
            handleError("Couldn't export objects", ex);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private void executeGet(ModelPortType port, Writer writer) throws FaultMessage, IOException, JAXBException {
        SelectorQualifiedGetOptionsType options = createOptions();

        QName type = ObjectType.getType(getParams().getType());
        if (type == null) {
            type = ObjectType.OBJECT.getType();
        }

        Holder<com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType> object = new Holder<>();
        Holder<OperationResultType> result = new Holder<>();

        port.getObject(type, getParams().getOid(), options, object, result);

        ToolsUtils.serializeObject(object.value, writer);
    }

    private SelectorQualifiedGetOptionsType createOptions() {
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        if (getParams().isRaw()) {
            SelectorQualifiedGetOptionType raw = new SelectorQualifiedGetOptionType();
            GetOperationOptionsType option = new GetOperationOptionsType();
            option.setRaw(true);
            raw.setOptions(option);
            options.getOption().add(raw);
        }

        return options;
    }

    private void executeSearch(ModelPortType port, Writer writer)
            throws FaultMessage, IOException, JAXBException, SAXException {

        SelectorQualifiedGetOptionsType options = createOptions();

        QName type = ObjectType.getType(getParams().getType());
        if (type == null) {
            type = ObjectType.OBJECT.getType();
        }

        int count = 0;
        int currentSize = 1;
        Holder<ObjectListType> list = new Holder<>();
        Holder<OperationResultType> result = new Holder<>();
        while (currentSize > 0) {
            QueryType query = createQuery(count);
            port.searchObjects(type, query, options, list, result);

            OperationResultType res = result.value;
            if (!OperationResultStatusType.SUCCESS.equals(res.getStatus())
                    && !getParams().isIgnore()) {
                printInfoMessage("Search returned {}, reason: ", res.getStatus(), res.getMessage());
                if (getParams().isVerbose()) {
                    printInfoMessage("Operation result:\n{}", ToolsUtils.serializeObject(res));
                }
                break;
            }

            ObjectListType objList = list.value;
            if (getParams().isVerbose()) {
                printInfoMessage("Search returned {}, status: {}/{}", res.getStatus(), count, objList.getCount());
            }

            List<com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType> objects = objList.getObject();
            currentSize = objects.size();

            for (com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType object : objects) {
                ToolsUtils.serializeObject(object, writer);
                writer.write('\n');
            }
            count += currentSize;
        }
    }

    private QueryType createQuery(int from) throws IOException, SAXException, JAXBException {
        QueryType query = new QueryType();
        query.setFilter(loadQuery());

        PagingType paging = new PagingType();
        paging.setOffset(from);
        paging.setMaxSize(SEARCH_PAGE_SIZE);
        paging.setOrderBy(ModelClientUtil.createItemPathType("name"));
        paging.setOrderDirection(OrderDirectionType.ASCENDING);
        query.setPaging(paging);

        return query;
    }

    private SearchFilterType loadQuery() throws IOException, SAXException, JAXBException {
        File query = getParams().getQuery();
        if (query == null) {
            return null;
        }

        if (!query.exists() || !query.canRead()) {
            String msg = "Couldn't read query file '" + query.getPath() + "'";
            STD_ERR.info(msg);
            throw new IOException(msg);
        }

        String xml = IOUtils.toString(new FileInputStream(query), StandardCharsets.UTF_8.name());
        return ModelClientUtil.parseSearchFilterType(xml);
    }

    private Writer createWriter() throws IOException {
        File file = getParams().getFile();
        if (file != null) {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            OutputStream os = new FileOutputStream(file);
            if (getParams().isZip()) {
                os = new ZipOutputStream(os);
            }
            Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            return new BufferedWriter(writer);
        }

        return new LogWriter(STD_OUT);
    }

    private void writeHeader(Writer writer) throws IOException {
        String header = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<c:objects xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">\n";
        writer.write(header);
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("</c:objects>\n");
    }

    private void printInfoMessage(String message, Object... args) {
        if (getParams().getFile() != null) {
            STD_OUT.info(message, args);
        } else {
            STD_ERR.info(message, args);
        }
    }
}
