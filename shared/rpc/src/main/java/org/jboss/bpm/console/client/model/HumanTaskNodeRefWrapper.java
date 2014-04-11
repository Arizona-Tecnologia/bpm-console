package org.jboss.bpm.console.client.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Wrapper of {@link org.jboss.bpm.console.client.model.HumanTaskNodeRef}
 * <p/>
 * Created on 09/04/14 12:11
 * @author Paola Bueno(paolabueno+dev@gmail.com)
 */
@XmlRootElement(name = "wrapper")
public class HumanTaskNodeRefWrapper {
    List<HumanTaskNodeRef> nodes;

    public HumanTaskNodeRefWrapper() {
    }
    public HumanTaskNodeRefWrapper(List<HumanTaskNodeRef> nodes) {
        this.nodes = nodes;
    }

    @XmlElement(name = "humanTaskNodes")
    public List<HumanTaskNodeRef> getNodes() {
        return nodes;
    }

    public void setNodes(List<HumanTaskNodeRef> nodes) {
        this.nodes = nodes;
    }

    @XmlElement(name = "totalCount")
    public int getTotalCount(){
        return nodes.size();
    }


}
