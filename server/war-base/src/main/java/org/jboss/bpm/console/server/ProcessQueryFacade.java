package org.jboss.bpm.console.server;

import org.jboss.bpm.console.server.util.ProcessHistoryQueryDao;
import org.jboss.bpm.console.server.util.ProjectName;
import org.jboss.bpm.console.server.util.RsComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.Collection;
import java.util.Map;

@Path("processQuery")
@RsComment(
        title = "Process Query",
        description = "Process Query",
        project = {ProjectName.JBPM}
)
public class ProcessQueryFacade {

    private static Logger log = LoggerFactory.getLogger(ProcessQueryFacade.class);


    @GET
    @Path("finished")
    @Produces("application/json")
    public Collection<Map<String, Object>> getFinishedProcesses(
            @QueryParam("filterProcessNameLike")
            String filterProcessNameLike,
            @QueryParam("filterStatus")
            String filterStatus, //status de negocio (ex. Aprovado, Reprovado)
            @QueryParam("filterFileNameLike")
            String filterFileNameLike,
            @QueryParam("filterEndDateIntervalFrom")
            Long filterEndDateIntervalFrom,
            @QueryParam("filterEndDateIntervalTo")
            Long filterEndDateIntervalTo,
            @QueryParam("filterStartDateIntervalFrom")
            Long filterStartDateIntervalFrom,
            @QueryParam("filterStartDateIntervalTo")
            Long filterStartDateIntervalTo,
            @QueryParam("filterProcessDefinitionId")
            String[] filterProcessDefinitionId,
            @QueryParam("filterTemplateId")
            String[] filterTemplateId,
            @QueryParam("excludeProcessId")
            String[] excludeProcessIds,
            @QueryParam("field")
            String[] fields,
            @QueryParam("creator")
            String creator,
            @QueryParam("offset")
            Long offset,
            @QueryParam("limit")
            Long limit
    ) {
        if (offset == null) {
            offset = 0L;
        }
        if (limit == null) {
            limit = 10L;
        }

        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!FINISHED PROCESSES - NEW FILTERS LOG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        log.info("filterProcessNameLike:" + filterProcessNameLike);
        log.info("filterStatus:" + filterStatus);
        log.info("filterFileNameLike:" + filterFileNameLike);
        log.info("filterEndDateIntervalFrom:" + filterEndDateIntervalFrom);
        log.info("filterEndDateIntervalTo:" + filterEndDateIntervalTo);
        log.info("filterStartDateIntervalFrom:" + filterStartDateIntervalFrom);
        log.info("filterStartDateIntervalTo:" + filterStartDateIntervalTo);
        log.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        try {
            final InitialContext ctx = new InitialContext();
            final DataSource datasource = (DataSource) ctx.lookup("java:jboss/datasources/jbpmDS");
            return new ProcessHistoryQueryDao(datasource).queryFinishedProcessHistory(
                    filterProcessNameLike,
                    filterStatus,
                    filterFileNameLike,
                    filterEndDateIntervalFrom ,
                    filterEndDateIntervalTo,
                    filterStartDateIntervalFrom,
                    filterStartDateIntervalTo,
                    filterProcessDefinitionId,
                    filterTemplateId, excludeProcessIds, fields, offset, limit, creator).values();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }


}
