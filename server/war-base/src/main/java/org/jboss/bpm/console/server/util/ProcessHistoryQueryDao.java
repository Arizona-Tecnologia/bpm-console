package org.jboss.bpm.console.server.util;

import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessHistoryQueryDao {

    private DataSource datasource;

    public ProcessHistoryQueryDao(DataSource datasource) {
        this.datasource = datasource;
    }

    public Map<Long, Map<String, Object>> queryFinishedProcessHistory(String[] listTemplateId,
                                                                      String[] excludeProcessIds,
                                                                      String[] fields,
                                                                      String creator,
                                                                      long offset,
                                                                      long limit) {
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
                queryProcessInstanceIds = queryProcessInstanceIds.replace("__EXCLUDE_PROCESS_IDS__",
                        " and processId not in (" + StringUtils.repeat("?", ",", excludeProcessIds.length) + ")");
            } else {
                queryProcessInstanceIds = queryProcessInstanceIds.replace("__EXCLUDE_PROCESS_IDS__", "");
            }


            if (StringUtils.isNotBlank(creator)) {
                queryProcessInstanceIds = queryProcessInstanceIds.replace("__FILTER_CREATOR__",
                        " and exists (select 1 from VariableInstanceLog where" +
                                " processInstanceId = pil.processInstanceId" +
                                " and variableId = 'r_creator' and value = ?)");
            } else {
                queryProcessInstanceIds = queryProcessInstanceIds.replace("__FILTER_CREATOR__", "");
            }

            //visto_processTemplate
            String queryTemplate = "";
            if (listTemplateId.length > 0) {
                queryTemplate =  " and exists (select 1 from VariableInstanceLog where" +
                        " processInstanceId = pil.processInstanceId" +
                        " and variableId = 'visto_processTemplate' and ( value like ? ";

                if(listTemplateId.length > 1) {
                    queryTemplate = queryTemplate + StringUtils.repeat(" or value like ? ", listTemplateId.length -1);
                }
            }

            queryProcessInstanceIds = queryProcessInstanceIds.replace("__FILTER_TEMPLATE__",
                    queryTemplate + "))");


            connection = datasource.getConnection();
            statement = connection.prepareStatement(queryProcessInstanceIds);

            int paramQueryProcessInstanceIds = 1;
            for (String excludeProcessId : excludeProcessIds) {
                statement.setString(paramQueryProcessInstanceIds++, excludeProcessId);
            }
            if (StringUtils.isNotBlank(creator)) {
                statement.setString(paramQueryProcessInstanceIds++, creator);
            }
            for (String templateId : listTemplateId) {
                statement.setString(paramQueryProcessInstanceIds++, "{\"id\":"+templateId+",\"name\":%");
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

    /**
     * Processos terminados.
     */
    private static final String QUERY_FINISHED_PROCESS_INSTANCE_ID = "select pil.* from ProcessInstanceLog pil" +
            " where pil.end_date is not null and pil.status = 2 " +
            "__EXCLUDE_PROCESS_IDS__" +
            "__FILTER_CREATOR__" +
            "__FILTER_TEMPLATE__" +
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
