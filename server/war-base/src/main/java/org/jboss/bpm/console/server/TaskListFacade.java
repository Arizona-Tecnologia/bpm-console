/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.bpm.console.server;

import com.google.gson.Gson;
import com.google.protobuf.ExtensionRegistry;
import org.apache.commons.lang3.StringUtils;
import org.drools.common.InternalRuleBase;
import org.drools.common.InternalWorkingMemory;
import org.drools.impl.InternalKnowledgeBase;
import org.drools.marshalling.impl.MarshallerReaderContext;
import org.drools.marshalling.impl.PersisterHelper;
import org.drools.marshalling.impl.ProtobufMarshaller;
import org.drools.marshalling.impl.ProtobufMessages;
import org.drools.runtime.KnowledgeRuntime;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jboss.bpm.console.client.model.KeyValue;
import org.jboss.bpm.console.client.model.TaskRef;
import org.jboss.bpm.console.client.model.TaskRefWrapper;
import org.jboss.bpm.console.server.gson.GsonFactory;
import org.jboss.bpm.console.server.integration.ManagementFactory;
import org.jboss.bpm.console.server.integration.ProcessManagement;
import org.jboss.bpm.console.server.integration.TaskManagement;
import org.jboss.bpm.console.server.plugin.PluginMgr;
import org.jboss.bpm.console.server.plugin.FormAuthorityRef;
import org.jboss.bpm.console.server.plugin.FormDispatcherPlugin;
import org.jboss.bpm.console.server.util.ProjectName;
import org.jboss.bpm.console.server.util.RsComment;
import org.jbpm.integration.console.StatefulKnowledgeSessionUtil;
import org.jbpm.marshalling.impl.JBPMMessages;
import org.jbpm.marshalling.impl.ProcessInstanceMarshaller;
import org.jbpm.marshalling.impl.ProcessMarshallerRegistry;
import org.jbpm.marshalling.impl.ProtobufProcessMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST server module for accessing task related data.
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
@Path("tasks")
@RsComment(
    title = "Task Lists",
    description = "Access task lists",
    project = {ProjectName.JBPM}
)
public class TaskListFacade
{
  private static final Logger log = LoggerFactory.getLogger(TaskMgmtFacade.class);

  private TaskManagement taskManagement;
  private ProcessManagement processManagement;
  private FormDispatcherPlugin formPlugin;

  /**
   * Lazy load the {@link org.jboss.bpm.console.server.integration.TaskManagement}
   */
  private TaskManagement getTaskManagement()
  {
    if(null==this.taskManagement)
    {
      ManagementFactory factory = ManagementFactory.newInstance();
      this.taskManagement = factory.createTaskManagement();
      log.debug("Using ManagementFactory impl:" + factory.getClass().getName());
    }

    return this.taskManagement;
  }

  private ProcessManagement getProcessManagement()
  {
    if(null==this.processManagement)
    {
      ManagementFactory factory = ManagementFactory.newInstance();
      this.processManagement = factory.createProcessManagement();
      log.debug("Using ManagementFactory impl:" + factory.getClass().getName());
    }

    return this.processManagement;
  }


    /**
   * Lazy load the {@link org.jboss.bpm.console.server.plugin.FormDispatcherPlugin}.
   * Can be null if the plugin is not available.
   */
  private FormDispatcherPlugin getFormDispatcherPlugin()
  {
    if(null==this.formPlugin)
    {
      this.formPlugin = PluginMgr.load(FormDispatcherPlugin.class);
    }

    return this.formPlugin;
  }

  @GET
  @Path("{idRef}")
  @Produces("application/json")
  public Response getTasksForIdRef(
      @PathParam("idRef")
      String idRef,
      @QueryParam("instanceData")
      String instanceData,
      @QueryParam("fields")
      String fields
  )
  {
      List<TaskRef> assignedTasks = getTaskManagement().getAssignedTasks(idRef);
      if (instanceData != null && assignedTasks.size() > 0) {
          Set<String> fieldSet = null;
          if (fields != null) {
              fieldSet = new HashSet<String>();
              Collections.addAll(fieldSet, fields.split(","));
          }
          if (fields == null || !fieldSet.isEmpty()) {
            fetchDataset(assignedTasks, fieldSet);
          }
      }
      return processTaskListResponse(assignedTasks);
  }

  @GET
  @Path("{idRef}/participation")
  @Produces("application/json")
  public Response getTasksForIdRefParticipation(
      @PathParam("idRef")
      String idRef,
      @QueryParam("instanceData")
      String instanceData,
      @QueryParam("fields")
      String fields
  )
  {
    List<TaskRef> participationTasks = getTaskManagement().getUnassignedTasks(idRef, null);
    if (instanceData != null && participationTasks.size() > 0) {
        Set<String> fieldSet = null;
        if (fields != null) {
            fieldSet = new HashSet<String>();
            Collections.addAll(fieldSet, fields.split(","));
        }
        if (fields == null || !fieldSet.isEmpty()) {
            fetchDataset(participationTasks, fieldSet);
        }
    }
    return processTaskListResponse(participationTasks);
  }

  // enriches the task refs with dataset information
  private void fetchDataset(List<TaskRef> tasks, Set<String> fieldSet) {

      Connection connection = null;
      Statement statement = null;
      ResultSet resultSet = null;

      try {
          // Group tasks by processInstanceId
          Map<String, Collection<TaskRef>> tasksByProcessInstanceId = new HashMap<String, Collection<TaskRef>>();
          for (TaskRef task : tasks) {
              final String processInstanceId = task.getProcessInstanceId();
              if (!tasksByProcessInstanceId.containsKey(processInstanceId)) {
                  tasksByProcessInstanceId.put(processInstanceId, new LinkedList<TaskRef>());
              }
              tasksByProcessInstanceId.get(processInstanceId).add(task);
          }

          // Fetches the datasets (they're actually stored as protobuf blobs on the db)
          final InitialContext ctx = new InitialContext();
          final DataSource datasource = (DataSource) ctx.lookup("java:jboss/datasources/jbpmDS");
          connection = datasource.getConnection();
          statement = connection.createStatement();
          resultSet = statement.executeQuery("SELECT" +
                  " p.id processInstanceId" +
                  ", p.processInstanceByteArray processInstanceByteArray" +
                  " FROM ProcessInstanceInfo p" +
                  " WHERE p.id IN (" + StringUtils.join(tasksByProcessInstanceId.keySet(), ",") + ")");

          StatefulKnowledgeSession session = null;
          while (resultSet.next()) {
              final String processInstanceId = resultSet.getString("processInstanceId");
              final byte[] processInstanceBytes = resultSet.getBytes("processInstanceByteArray");

              // Dataset unmarshalling voodoo
              if (session == null) {
                  session = StatefulKnowledgeSessionUtil.getStatefulKnowledgeSession();
              }
              final Collection<KeyValue> datasetList = unmarshallProcessInstanceVariables(processInstanceBytes,
                      fieldSet,
                      session);

              // Sets the dataset on each task ref
              final KeyValue[] dataset = datasetList.toArray(new KeyValue[datasetList.size()]);
              for (TaskRef taskRef : tasksByProcessInstanceId.get(processInstanceId)) {
                  taskRef.setDataset(dataset);
              }
          }
      } catch (NamingException e) {
          throw new RuntimeException(e);
      } catch (SQLException e) {
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

  private Response processTaskListResponse(List<TaskRef> taskList)
  {

    TaskRefWrapper wrapper = new TaskRefWrapper(taskList);
    return createJsonResponse(wrapper);
  }

  private Response createJsonResponse(Object wrapper)
  {
    Gson gson = GsonFactory.createInstance();
    String json = gson.toJson(wrapper);
    return Response.ok(json).type("application/json").build();
  }

    // Shameless copied from ProcessInstanceInfo.java
    private Collection<KeyValue> unmarshallProcessInstanceVariables(byte[] processInstanceByteArray,
                                                                    Set<String> fieldSet,
                                                                    KnowledgeRuntime kruntime) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(processInstanceByteArray);
            MarshallerReaderContext context = new MarshallerReaderContext(bais,
                    (InternalRuleBase) ((InternalKnowledgeBase) kruntime.getKnowledgeBase()).getRuleBase(),
                    null,
                    null,
                    ProtobufMarshaller.TIMER_READERS,
                    kruntime.getEnvironment()
            );
            getMarshallerFromContext(context); // can't touch this!
            Collection<KeyValue> processInstanceVariables = unmarshallProcessInstanceVariables(context, fieldSet);
            context.close();
            return processInstanceVariables;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("IOException while loading process instance: " + e.getMessage(), e);
        }
    }

    // Shameless copied from ProcessInstanceInfo.java
    private ProcessInstanceMarshaller getMarshallerFromContext(MarshallerReaderContext context) throws IOException {
        ObjectInputStream stream = context.stream;
        String processInstanceType = stream.readUTF();
        return ProcessMarshallerRegistry.INSTANCE.getMarshaller( processInstanceType );
    }

    // Shameless copied from AbstractProtobufProcessInstanceMarshaller.java
    private Collection<KeyValue> unmarshallProcessInstanceVariables(MarshallerReaderContext context, Set<String> fieldSet) throws IOException {
        InternalRuleBase ruleBase = context.ruleBase;

        // try to parse from the stream
        ExtensionRegistry registry = PersisterHelper.buildRegistry(context, null);
        ProtobufMessages.Header _header;
        try {
          _header = PersisterHelper.readFromStreamWithHeader( context, registry );
        } catch ( ClassNotFoundException e ) {
          // Java 5 does not accept [new IOException(String, Throwable)]
          IOException ioe =  new IOException( "Error deserializing process instance." );
          ioe.initCause(e);
          throw ioe;
        }
        JBPMMessages.ProcessInstance _instance = JBPMMessages.ProcessInstance.parseFrom(_header.getPayload(), registry);

        final LinkedList<KeyValue> datasetList = new LinkedList<KeyValue>();
        if ( _instance.getVariableCount() > 0 ) {
            String processId = _instance.getProcessId();
            org.drools.definition.process.Process process = ruleBase.getProcess( processId );
            for ( JBPMMessages.Variable _variable : _instance.getVariableList() ) {
                if (fieldSet == null || fieldSet.contains(_variable.getName())) {
                    try {
                        Object _value = ProtobufProcessMarshaller.unmarshallVariableValue(context, _variable);
                        datasetList.add(new KeyValue(_variable.getName(), _value, _value.getClass().getName()));
                    } catch ( ClassNotFoundException e ) {
                        throw new IllegalArgumentException( "Could not reload variable " + _variable.getName() );
                    }
                }
            }
        }

        return datasetList;
    }
}
