package org.jboss.bpm.console.server.util;

import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import javax.ws.rs.QueryParam;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessHistoryQueryDao {

    private DataSource datasource;

    public ProcessHistoryQueryDao(DataSource datasource) {
        this.datasource = datasource;
    }

    public Map<Long, Map<String, Object>> queryFinishedProcessHistory(
                                                                      String filterProcessNameLike,
                                                                      String filterStatus,
                                                                      String filterFileNameLike,
                                                                      Long filterEndDateIntervalFrom,
                                                                      Long filterEndDateIntervalTo,
                                                                      Long filterStartDateIntervalFrom,
                                                                      Long filterStartDateIntervalTo,
                                                                      String[] filterProcessDefinitionId,
                                                                      String[] filterTemplateId,
                                                                      String[] excludeProcessIds,
                                                                      String[] fields,
                                                                      long offset, long limit, String creator) {
        if (fields == null) {
            fields = new String[0];
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Query process instance history
            String queryProcessInstanceIds = QUERY_FINISHED_PROCESS_INSTANCE_ID;

            if (excludeProcessIds.length > 0) {
                queryProcessInstanceIds = queryProcessInstanceIds.replace(EXCLUDE_PROCESS_IDS,
                        " and processId not in (" + StringUtils.repeat("?", ",", excludeProcessIds.length) + ")");
            } else {
                queryProcessInstanceIds = queryProcessInstanceIds.replace(EXCLUDE_PROCESS_IDS, "");
            }


            if (StringUtils.isNotBlank(creator)) {
                queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_CREATOR,
                        " and exists (select 1 from VariableInstanceLog where" +
                                " processInstanceId = pil.processInstanceId" +
                                " and variableId = 'r_creator' and value = ?)");
            } else {
                queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_CREATOR, "");
            }

            //visto_processTemplate
            String queryTemplate = "";
            if (filterTemplateId.length > 0) {
                queryTemplate =  " and exists (select 1 from VariableInstanceLog where" +
                        " processInstanceId = pil.processInstanceId" +
                        " and variableId = 'visto_processTemplate' and ( value like ? ";

                if(filterTemplateId.length > 1) {
                    queryTemplate = queryTemplate + StringUtils.repeat(" or value like ? ", filterTemplateId.length -1);
                }

                queryTemplate = queryTemplate + "))";
            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_TEMPLATE,queryTemplate);
            //fim visto_processTemplate

            //r_processDefinitionName
            String queryProcessName = "";
            if (StringUtils.isNotBlank(filterProcessNameLike)) {
                queryProcessName =  " and exists (select 1 from VariableInstanceLog where" +
                        " processInstanceId = pil.processInstanceId" +
                        " and variableId = 'r_processDefinitionName' and value like ? ESCAPE '!')";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_PROCESS_NAME_LIKE,queryProcessName);
            //fim r_processDefinitionName

            //fileName
            String queryFileName = "";
            if (StringUtils.isNotBlank(filterFileNameLike)) {
                queryFileName =  " and exists (select 1 from VariableInstanceLog where" +
                        " processInstanceId = pil.processInstanceId" +
                        " and variableId = 'fileName' and value like ? ESCAPE '!')";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_FILE_NAME_LIKE,queryFileName);
            //fim fileName

            //r_status
            String queryStatus = "";
            if (StringUtils.isNotBlank(filterStatus)) {
                queryStatus =  " and exists (select 1 from VariableInstanceLog where" +
                        " processInstanceId = pil.processInstanceId" +
                        " and variableId = 'r_status' and value = ?)";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_STATUS,queryStatus);
            //fim r_status

            //start_date
            String queryStartDateFrom = "";
            if (filterStartDateIntervalFrom != null) {
                queryStartDateFrom =  " and start_date >= ?";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_START_DATE_FROM,queryStartDateFrom);
            String queryStartDateTo = "";
            if (filterStartDateIntervalTo != null) {
                queryStartDateTo =  " and start_date <= ?";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_START_DATE_TO,queryStartDateTo);
            //fim start_date

            //end_date
            String queryEndDateFrom = "";
            if (filterEndDateIntervalFrom != null) {
                queryEndDateFrom =  " and end_date >= ?";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_END_DATE_FROM,queryEndDateFrom);
            String queryEndDateTo = "";
            if (filterEndDateIntervalTo != null) {
                queryEndDateTo =  " and end_date <= ?";

            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace(FILTER_END_DATE_TO,queryEndDateTo);
            //fim end_date


            //filterProcessDefinitionId
            if (filterProcessDefinitionId.length > 0) {
                queryProcessInstanceIds = queryProcessInstanceIds.replace(INCLUDE_ONLY_PROCESS_IDS,
                        " and processId in (" + StringUtils.repeat("?", ",", filterProcessDefinitionId.length) + ")");
            } else {
                queryProcessInstanceIds = queryProcessInstanceIds.replace(INCLUDE_ONLY_PROCESS_IDS, "");
            }

            connection = datasource.getConnection();
            statement = connection.prepareStatement(queryProcessInstanceIds);

            int paramQueryProcessInstanceIds = 1;
            for (String excludeProcessId : excludeProcessIds) {
                statement.setString(paramQueryProcessInstanceIds++, excludeProcessId);
            }
            if (StringUtils.isNotBlank(creator)) {
                statement.setString(paramQueryProcessInstanceIds++, creator);
            }
            for (String templateId : filterTemplateId) {
                statement.setString(paramQueryProcessInstanceIds++, "{\"id\":"+templateId+",\"name\":%");
            }

            if (StringUtils.isNotBlank(filterProcessNameLike)) {
                filterProcessNameLike = filterProcessNameLike
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                        .replace("[", "![");
                statement.setString(paramQueryProcessInstanceIds++, "%"+filterProcessNameLike+"%");
            }
            if (StringUtils.isNotBlank(filterFileNameLike)) {
                filterFileNameLike = filterFileNameLike
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                        .replace("[", "![");
                statement.setString(paramQueryProcessInstanceIds++, "%"+filterFileNameLike+"%");
            }

            if (StringUtils.isNotBlank(filterStatus)) {
                statement.setString(paramQueryProcessInstanceIds++, filterStatus);
            }

            if (filterStartDateIntervalFrom != null) {
                statement.setTimestamp(paramQueryProcessInstanceIds++, new Timestamp(filterStartDateIntervalFrom));
            }
            if (filterStartDateIntervalTo != null) {
                statement.setTimestamp(paramQueryProcessInstanceIds++, new Timestamp(filterStartDateIntervalTo));
            }
            if (filterEndDateIntervalFrom != null) {
                statement.setTimestamp(paramQueryProcessInstanceIds++, new Timestamp(filterEndDateIntervalFrom));
            }
            if (filterEndDateIntervalTo != null) {
                statement.setTimestamp(paramQueryProcessInstanceIds++, new Timestamp(filterEndDateIntervalTo));
            }



            for (String processId : filterProcessDefinitionId) {
                statement.setString(paramQueryProcessInstanceIds++, processId);
            }
            statement.setLong(paramQueryProcessInstanceIds++, offset);
            statement.setLong(paramQueryProcessInstanceIds, limit);

            resultSet = statement.executeQuery();

            LinkedHashMap<Long, Map<String, Object>> processInstances = new LinkedHashMap<Long, Map<String, Object>>();
            while (resultSet.next()) {
                final HashMap<String, Object> processInstance = new HashMap<String, Object>();
                final ResultSetMetaData metaData = resultSet.getMetaData();
                final int columnCount = metaData.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    processInstance.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                }
                final Long processInstanceId = resultSet.getLong("processInstanceId");
                processInstances.put(processInstanceId, processInstance);
            }

            // Query process instance variables
            if (!processInstances.isEmpty()) {

                resultSet.close();
                statement.close();

                String queryVariables = QUERY_PROCESS_INSTANCE_VARIABLES;
                queryVariables = queryVariables.replace("__PIIDS__", StringUtils.repeat("?", ",", processInstances.size()));

                if (fields.length > 0) {
                    queryVariables = queryVariables.replace("__FIELDS__",
                            "  and variableId in (" + StringUtils.repeat("?", ",", fields.length) + ")");
                } else {
                    queryVariables = queryVariables.replace("__FIELDS__", "");
                }
                statement = connection.prepareStatement(queryVariables);

                int paramQueryVariables = 1;
                for (Long piid : processInstances.keySet()) {
                    statement.setLong(paramQueryVariables++, piid);
                }
                for (String field : fields) {
                    statement.setString(paramQueryVariables++, field);
                }
                resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    final long processInstanceId = resultSet.getLong(1);
                    final Map<String, Object> processInstance = processInstances.get(processInstanceId);
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> variables = (HashMap<String, Object>) processInstance.get("variables");
                    if (variables == null) {
                        variables = new HashMap<String, Object>();
                        processInstance.put("variables", variables);
                    }
                    final String variableId = resultSet.getString(2);
                    final Object variableValue = resultSet.getObject(3);
                    variables.put(variableId, variableValue);
                }
            }

            return processInstances;

        } catch (SQLException e) {
            System.out.println(statement);
            throw new RuntimeException(e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    // swallow!
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    // swallow!
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // swallow!
                }
            }
        }
    }

    private static final String EXCLUDE_PROCESS_IDS = "__EXCLUDE_PROCESS_IDS__";
    private static final String FILTER_CREATOR = "__FILTER_CREATOR__";
    private static final String FILTER_TEMPLATE = "__FILTER_TEMPLATE__";
    private static final String FILTER_PROCESS_NAME_LIKE = "__FILTER_PROCESS_NAME_LIKE__";
    private static final String FILTER_FILE_NAME_LIKE = "__FILTER_FILE_NAME_LIKE__";
    private static final String FILTER_STATUS = "__FILTER_STATUS__";
    private static final String FILTER_START_DATE_FROM = "__FILTER_START_DATE_FROM__";
    private static final String FILTER_START_DATE_TO = "__FILTER_START_DATE_TO__";
    private static final String FILTER_END_DATE_FROM = "__FILTER_END_DATE_FROM__";
    private static final String FILTER_END_DATE_TO = "__FILTER_END_DATE_TO__";
    private static final String INCLUDE_ONLY_PROCESS_IDS = "__INCLUDE_ONLY_PROCESS_IDS__";
    /**
     * Processos terminados.
     */
    private static final String QUERY_FINISHED_PROCESS_INSTANCE_ID = "select pil.* from ProcessInstanceLog pil" +
            " where pil.end_date is not null and pil.status = 2 " +
            EXCLUDE_PROCESS_IDS +
            FILTER_CREATOR +
            FILTER_TEMPLATE +
            FILTER_PROCESS_NAME_LIKE +
            FILTER_FILE_NAME_LIKE +
            FILTER_STATUS +
            FILTER_START_DATE_FROM +
            FILTER_START_DATE_TO +
            FILTER_END_DATE_FROM +
            FILTER_END_DATE_TO +
            INCLUDE_ONLY_PROCESS_IDS +

            " order by pil.processInstanceId desc limit ?, ?";

    /**
     * Variáveis de instância de processo.
     */
    private static final String QUERY_PROCESS_INSTANCE_VARIABLES = "select vil.processInstanceId, vil.variableId, vil.VALUE from VariableInstanceLog vil" +
            " where 1=1" +
            " and vil.processInstanceId in (__PIIDS__)" +
            " and vil.id in (" +
            "  select max(id) from VariableInstanceLog" +
            "  where processInstanceId = vil.processInstanceId" +
            "__FIELDS__" +
            "  group by variableId)";
}
