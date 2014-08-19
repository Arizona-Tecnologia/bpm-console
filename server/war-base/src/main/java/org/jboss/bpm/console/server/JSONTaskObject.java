package org.jboss.bpm.console.server;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;

class JSONTaskObject {
	  
	  private Map<String,JsonNode> extraProperties = new HashMap<String, JsonNode>();
	  
	  public JSONTaskObject() {}
	  
	  @JsonAnyGetter
	  @SuppressWarnings("unused") Map<String, JsonNode> getExtraProperties() {
		  return extraProperties;
	  }

	  @JsonAnySetter
	  @SuppressWarnings("unused")
	  private void setUnknownProperty(String key, JsonNode value) {
		  extraProperties.put(key, value);
	  }
  }