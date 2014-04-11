package org.jboss.bpm.console.client.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the node information of a human task
 * <p/>
 * Created on 09/04/14 12:10
 * @author Paola Bueno(paolabueno+dev@gmail.com)
 */
@XmlRootElement(name = "humanTaskNode")
public class HumanTaskNodeRef {

    private String id;
    private String name;
    private String taskName;
    private String priority;
    private String actors;
    private String groups;

    @XmlElement(name = "nodeId")
    public String getId() {
        return id;
    }

    public HumanTaskNodeRef setId(String id) {
        this.id = id;
        return this;
    }

    @XmlElement(name = "nodeName")
    public String getName() {
        return name;
    }

    public HumanTaskNodeRef setName(String name) {
        this.name = name;
        return this;
    }

    @XmlElement(name = "taskName")
    public String getTaskName() {
        return taskName;
    }

    public HumanTaskNodeRef setTaskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    @XmlElement(name = "priority")
    public String getPriority() {
        return priority;
    }

    public HumanTaskNodeRef setPriority(String priority) {
        this.priority = priority;
        return this;
    }

    @XmlElement(name = "actors")
    public String getActors() {
        return actors;
    }

    public HumanTaskNodeRef setActors(String actors) {
        this.actors = actors;
        return this;
    }

    @XmlElement(name = "groups")
    public String getGroups() {
        return groups;
    }

    public HumanTaskNodeRef setGroups(String groups) {
        this.groups = groups;
        return this;
    }



    @Override
    public String toString() {
        return new StringBuilder("HumanTaskNodeRef{id:")
                .append(id).append(", name:")
                .append(name).append(", taskName:")
                .append(taskName)
                .append("}").toString();
    }
}
