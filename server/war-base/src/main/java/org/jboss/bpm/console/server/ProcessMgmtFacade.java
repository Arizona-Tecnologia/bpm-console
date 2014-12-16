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

import org.jboss.bpm.console.client.model.*;
import org.jboss.bpm.console.server.gson.GsonFactory;
import org.jboss.bpm.console.server.integration.ManagementFactory;
import org.jboss.bpm.console.server.integration.ProcessManagement;
import org.jboss.bpm.console.server.plugin.*;
import org.jboss.bpm.console.server.util.Payload2XML;
import org.jboss.bpm.console.server.util.ProjectName;
import org.jboss.bpm.console.server.util.RsComment;
import org.jbpm.persistence.processinstance.ProcessInstanceComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST server module for accessing process related data.
 *
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
@Path("process")
@RsComment(
    title = "Process Management",
    description = "Process related data.")
public class ProcessMgmtFacade
{
  private static final Logger log = LoggerFactory.getLogger(ProcessMgmtFacade.class);

  private ProcessManagement processManagement;
  private GraphViewerPlugin graphViewerPlugin;
  private ProcessActivityPlugin activityPlugin;

  private FormDispatcherPlugin formPlugin;

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

  private GraphViewerPlugin getGraphViewerPlugin()
  {
    if(graphViewerPlugin==null)
    {
      graphViewerPlugin = PluginMgr.load(
          GraphViewerPlugin.class
      );
    }

    return graphViewerPlugin;
  }

    private ProcessActivityPlugin getActivityPlugin()
  {
    if(activityPlugin==null)
    {
      activityPlugin = PluginMgr.load(
          ProcessActivityPlugin.class
      );
    }

    return activityPlugin;
  }


  /**
   * Enriches {@link org.jboss.bpm.console.client.model.ProcessDefinitionRef} with
   * form and diagram URLs if applicable. 
   */
  private Response decorateProcessDefintions( List<ProcessDefinitionRef> processDefinitions)
  {
    // decorate process form URL if plugin available
    FormDispatcherPlugin formPlugin = getFormDispatcherPlugin();
    if(formPlugin!=null)
    {
      for(ProcessDefinitionRef def : processDefinitions)
      {
        URL processFormURL = formPlugin.getDispatchUrl(
            new FormAuthorityRef(def.getId(), FormAuthorityRef.Type.PROCESS)
        );
        if(processFormURL!=null)
        {
          def.setFormUrl(processFormURL.toExternalForm());
        }
      }
    }

    // decorate the diagram URL if available
    GraphViewerPlugin graphViewer = getGraphViewerPlugin();
    if(graphViewer!=null)
    {
      for(ProcessDefinitionRef def : processDefinitions)
      {
        URL diagramUrl = graphViewer.getDiagramURL(def.getId());
        if(diagramUrl!=null)
        {
          def.setDiagramUrl(diagramUrl.toExternalForm());
        }
      }
    }

    ProcessDefinitionRefWrapper wrapper =
        new ProcessDefinitionRefWrapper(processDefinitions);
    return createJsonResponse(wrapper);
  } 

  private Response createJsonResponse(Object wrapper)  {
    Gson gson = GsonFactory.createInstance();
    String json = gson.toJson(wrapper);
    return Response.ok(json).type("application/json").build();
  }

  @GET
  @Path("definitions")
  @Produces("application/json")
  public Response getDefinitionsJSON()
  {
    List<ProcessDefinitionRef> processDefinitions = getProcessManagement().getProcessDefinitions();
    return decorateProcessDefintions(processDefinitions);
  }

  @POST
  @Path("definition/{id}/remove")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response removeDefinitionsJSON(
      @PathParam("id")
      String definitionId
  )
  {
    ProcessDefinitionRefWrapper wrapper =
        new ProcessDefinitionRefWrapper( getProcessManagement().removeProcessDefinition(definitionId));
    return createJsonResponse(wrapper);
  }

  @GET
  @Path("definition/{id}/instances")
  @Produces("application/json")
  public Response getInstancesJSON(
      @PathParam("id")
      String definitionId
  )
  {
    ProcessInstanceRefWrapper wrapper =
        new ProcessInstanceRefWrapper(getProcessManagement().getProcessInstances(definitionId));
    return createJsonResponse(wrapper);
  }

  @POST
  @Path("definition/{id}/new_instance")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response newInstance(
      @PathParam("id")
      String definitionId)
  {

    ProcessInstanceRef instance = null;
    try
    {
      instance = getProcessManagement().newInstance(definitionId);
      return createJsonResponse(instance);
    }
    catch (Throwable t)
    {
      throw new WebApplicationException(t, 500);
    }

  }

    @GET
    @Path("definition/{id}/nodes/human")
    @Produces("application/json")
    public Response getHumanTaskNodesJSON(
            @PathParam("id")
            String definitionId ){
        HumanTaskNodeRefWrapper wrapper =
                new HumanTaskNodeRefWrapper(getProcessManagement().getHumanTaskNodeListForProcess(definitionId));
        return createJsonResponse(wrapper);
    }

  @GET
  @Path("instance/{id}/dataset")
  @Produces("text/xml")
  public Response getInstanceData(
      @PathParam("id")
      String instanceId
  )
  {
	try {
	  Map<String, Object> javaPayload = getProcessManagement().getInstanceData(instanceId);
      Payload2XML payload2XML = new Payload2XML();
      StringBuffer sb = payload2XML.convert(instanceId, javaPayload);
      return Response.ok(sb.toString()).build();
	} catch (Exception e) {
	  log.error("Error when getting instance data", e);	
	  ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
	  builder.status(Status.INTERNAL_SERVER_ERROR);
      return builder.build();
	}
    
  }

  @POST
  @Path("instance/{id}/state/{next}")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response changeState(
      @PathParam("id")
      String executionId,
      @PathParam("next")
      String next)
  {
    try {
	  ProcessInstanceRef.STATE state = ProcessInstanceRef.STATE.valueOf(next);
	  log.debug("Change instance (ID "+executionId+") to state " +state);
	  getProcessManagement().setProcessState(executionId, state);
	  return Response.ok().type("application/json").build();
    } catch (Exception e) {
	  log.error("Error when getting changing state of process instance", e);	
	  ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
	  builder.status(Status.INTERNAL_SERVER_ERROR);
		
	  return builder.build();
	}
  }

  @POST
  @Path("instance/{id}/end/{result}")
  @Produces("application/json")
  public Response endInstance(
      @PathParam("id")
      String executionId,
      @PathParam("result")
      String resultValue)
  {
	try {
	  ProcessInstanceRef.RESULT result = ProcessInstanceRef.RESULT.valueOf(resultValue);
	  log.debug("Change instance (ID "+executionId+") to state " + ProcessInstanceRef.STATE.ENDED);
	  getProcessManagement().endInstance(executionId, result);
	  return Response.ok().type("application/json").build();
  	} catch (Exception e) {
	  log.error("Error when getting ending of process instance", e);	
	  ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
	  builder.status(Status.INTERNAL_SERVER_ERROR);
		
	  return builder.build();
	}
  }

  @POST
  @Path("instance/{id}/delete")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response deleteInstance(
      @PathParam("id")
      String executionId
  )
  {
	try {
	  log.debug("Delete instance (ID "+executionId+")");
      getProcessManagement().deleteInstance(executionId);
      return Response.ok().type("application/json").build();
  	} catch (Exception e) {
	  log.error("Error when getting ending of process instance", e);	
	  ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
	  builder.status(Status.INTERNAL_SERVER_ERROR);
		
	  return builder.build();
	}
  }

  @POST
  @Path("tokens/{id}/transition")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response signalExecution(
      @PathParam("id")
      String id,
      @QueryParam("signal")
      String signalName)
  {
    try {
      id = URLDecoder.decode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    log.debug("Signal token " + id + " -> " + signalName);

    if ("default transition".equals(signalName))
      signalName = null;

    getProcessManagement().signalExecution(id, signalName);
    return Response.ok().type("application/json").build();
  }

  @POST
  @Path("tokens/{id}/transition/default")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response signalExecutionDefault(
      @PathParam("id")
      String id)
  {
    try {
      id = URLDecoder.decode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    log.debug("Signal token " + id);

    getProcessManagement().signalExecution(id, null);
    return Response.ok().type("application/json").build();
  }

  @GET
  @Path("definition/{id}/image")
  @Produces("image/*")
  public Response getProcessImage(
      @Context
      HttpServletRequest request,
      @PathParam("id")
      String id
  )
  {
    GraphViewerPlugin plugin = getGraphViewerPlugin();
    if(plugin !=null)
    {
      byte[] processImage = plugin.getProcessImage(id);
      if(processImage!=null)
        return Response.ok(processImage).type("image/png").build();
      else
        return Response.status(404).build();
    }

    throw new RuntimeException(
        GraphViewerPlugin.class.getName()+ " not available."
    );
  }

  @GET
  @Path("definition/{id}/image/{instance}")
  @Produces("image/*")
  public Response getProcessInstanceImage(
      @Context
      HttpServletRequest request,
      @PathParam("id")
      String id,
      @PathParam("instance")
      String instance
  )
  {
    ProcessActivityPlugin plugin = getActivityPlugin();
    if(plugin !=null)
    {
      byte[] processImage = plugin.getProcessInstanceImage(id, instance);
      if(processImage!=null)
        return Response.ok(processImage).type("image/png").build();
      else
        return Response.status(404).build();
    }

    throw new RuntimeException(
        ProcessActivityPlugin.class.getName()+ " not available."
    );
  }

  @GET
  @Path("instance/{id}/activeNodeInfo")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response getActiveNodeInfo(
      @PathParam("id")
      String id)
  {

    GraphViewerPlugin plugin = getGraphViewerPlugin();
    if(plugin !=null)
    {
      List<ActiveNodeInfo> info = plugin.getActiveNodeInfo(id);
      return createJsonResponse(info);
    }

    throw new RuntimeException(
        GraphViewerPlugin.class.getName()+ " not available."
    );
  }
  
  @GET
  @Path("definition/history/{id}/nodeInfo")
  @Produces("application/json")
  @RsComment(project = {ProjectName.JBPM})
  public Response getNodeInfoForActivities(
      @PathParam("id") String id,
      @QueryParam("activity") String[] activities)
  {

    GraphViewerPlugin plugin = getGraphViewerPlugin();
    if(plugin !=null)
    {
      List<ActiveNodeInfo> info = plugin.getNodeInfoForActivities(id, Arrays.asList(activities));
      return createJsonResponse(info);
    }

    throw new RuntimeException(
        GraphViewerPlugin.class.getName()+ " not available."
    );
  }
  
  
	@GET
	@Path("instance/{id}/comments")
	@Produces("application/json")
	public Response getInstanceComments(
			@PathParam("id") Long instanceId
	)
	{
		log.debug("GET getInstanceComments (id="+instanceId+")");
		
		try {
			
			List<ProcessInstanceComment> list = getProcessManagement().getProcessInstanceComments(instanceId);
			
			return Response.ok(list).type("application/json").build();
			
		} catch (Exception e) {
			log.error("Error when getting process instance comments for (id="+instanceId+")", e);	
			ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
			builder.status(Status.INTERNAL_SERVER_ERROR);
			
			return builder.build();
		}
	    
	}
	
	@GET
	@Path("instance/{id}/comments/{channel}")
	@Produces("application/json")
	public Response getInstanceComments(
			@PathParam("id") Long instanceId,
			@PathParam("channel") String channel
	)
	{
		log.debug("GET getInstanceComments (id="+instanceId+", channel="+channel+")");
		
		try {
			
			List<ProcessInstanceComment> list = getProcessManagement().getProcessInstanceComments(instanceId, channel);
			
			return Response.ok(list).type("application/json").build();
			
		} catch (Exception e) {
			log.error("Error when getting process instance comments for (id="+instanceId+", channel="+channel+")", e);	
			ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
			builder.status(Status.INTERNAL_SERVER_ERROR);
			
			return builder.build();
		}
	}
	
	@POST
	@Path("instance/{id}/comments")
	@Consumes("application/json")
	@Produces("application/json")
	public Response createInstanceComment(
			@PathParam("id") Long instanceId,
			ProcessInstanceComment comment)
	{
		log.debug("POST createInstanceComment (id="+instanceId+", comment="+comment+")");
		
		comment.setProcessInstanceId(instanceId);
		
		try {
			
			getProcessManagement().createProcessInstanceComment(comment);
			
			return Response.ok().type("application/json").build();
			
		} catch (Exception e) {
			log.error("Error when createInstanceComment (id="+instanceId+", comment="+comment+")", e);	
			ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
			builder.status(Status.INTERNAL_SERVER_ERROR);
			
			return builder.build();
		}
	}
	
	@PUT
	@Path("instance/{id}/comments/{commentId}")
	@Consumes("application/json")
	@Produces("application/json")
	public Response updateInstanceComment(
			@PathParam("id") Long instanceId,
			@PathParam("commentId") Long commentId,
			ProcessInstanceComment comment)
	{
		log.debug("PUT updateInstanceComment (id="+instanceId+", commentId="+commentId+", comment="+comment+")");
		
		comment.setId(commentId);
		comment.setProcessInstanceId(instanceId);
		
		try {
			
			getProcessManagement().updateProcessInstanceComment(comment);
			
			return Response.ok().build();
			
		} catch (Exception e) {
			log.error("Error when updateInstanceComment (id="+instanceId+", commentId="+commentId+", comment="+comment+")", e);	
			ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
			builder.status(Status.INTERNAL_SERVER_ERROR);
			
			return builder.build();
		}
	}
	
	@DELETE
	@Path("instance/{id}/comments/{commentId}")
	@Produces("application/json")
	public Response deleteInstanceComment(
			@PathParam("id") Long instanceId,
			@PathParam("commentId") Long commentId)
	{
		log.debug("DELETE deleteInstanceComment (id="+instanceId+", commentId="+commentId+")");
		
		ProcessInstanceComment comment = new ProcessInstanceComment();
		comment.setId(commentId);
		comment.setProcessInstanceId(instanceId);
		
		try {
			
			getProcessManagement().deleteProcessInstanceComment(comment);
			
			return Response.ok().build();
			
		} catch (Exception e) {
			log.error("Error when deleteInstanceComment (id="+instanceId+", commentId="+commentId+")", e);	
			ResponseBuilder builder = Response.fromResponse(Response.ok(e.getMessage()).build());
			builder.status(Status.INTERNAL_SERVER_ERROR);
			
			return builder.build();
		}
	}
}