package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.ninja.impl.ConfigurationException;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.DeleteOptions;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeleteRepositoryAction extends RepositoryAction<DeleteOptions> {

    private static final String DOT_CLASS = DeleteRepositoryAction.class.getName() + ".";

    private static final String OPERATION_DELETE = DOT_CLASS + "delete";

    private enum State {

        DELETE, SKIP, STOP;
    }

    @Override
    public void execute() throws Exception {
        String oid = options.getOid();
        FileReference filter = options.getFilter();

        if (oid == null && filter == null) {
            throw new ConfigurationException("Either filer or oid must not be null");
        }

        if (oid != null) {
            deleteByOid();
        } else {
            deleteByFilter();
        }
    }

    private void deleteByOid() throws SchemaException {
        InOidFilter filter = InOidFilter.createInOid(options.getOid());

        deleteByFilter(filter);
    }

    private void deleteByFilter() throws SchemaException, IOException {
        ApplicationContext appContext = context.getApplicationContext();
        PrismContext prismContext = appContext.getBean(PrismContext.class);

        ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), prismContext);

        deleteByFilter(filter);
    }

    private void deleteByFilter(ObjectFilter filter) throws SchemaException {
        ObjectTypes type = options.getType();
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        OperationResult result = new OperationResult(OPERATION_DELETE);

        ResultHandler handler = (prismObject, operationResult) -> {

            try {
                State state = options.isAsk() ? askForState(prismObject) : State.DELETE;

                switch (state) {
                    case SKIP:
                        return true;
                    case STOP:
                        return false;
                    case DELETE:
                    default:
                }

                RepositoryService repository = context.getRepository();
                repository.deleteObject(prismObject.getCompileTimeClass(), prismObject.getOid(), operationResult);
            } catch (ObjectNotFoundException ex) {
                // object was already gone
            } catch (IOException ex) {
                throw new NinjaException("Couldn't delete object '" + prismObject.toDebugName() + "'", ex);
            }

            return true;
        };

        Collection<SelectorOptions<GetOperationOptions>> opts = new ArrayList<>();
        if (options.isRaw()) {
            opts.add(new SelectorOptions(GetOperationOptions.createRaw()));
        }

        RepositoryService repository = context.getRepository();
        repository.searchObjectsIterative(type.getClassDefinition(), query, handler, opts, false, result);
    }

    private State askForState(PrismObject object) throws IOException {
        System.out.println("Do you really want to delete object '" + object.toDebugName() + "'? Yes/No/Cancel");

        State state = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (state == null) {
                String strState = br.readLine();

                if (StringUtils.isEmpty(strState)) {
                    continue;
                }

                strState = strState.toLowerCase();

                if ("y".equals(strState) || "yes".equals(strState)) {
                    state = State.DELETE;
                }

                if ("n".equals(strState) || "no".equals(strState)) {
                    state = State.SKIP;
                }

                if ("c".equals(strState) || "cancel".equals(strState)) {
                    state = State.STOP;
                }
            }
        }

        return state;
    }
}
