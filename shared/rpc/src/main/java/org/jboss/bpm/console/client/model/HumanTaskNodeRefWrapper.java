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
    List<HumanTaskNodeRef> humanTaskNodeRefList;

    public HumanTaskNodeRefWrapper() {
    }
    public HumanTaskNodeRefWrapper(List<HumanTaskNodeRef> humanTaskNodeRefList) {
        this.humanTaskNodeRefList = humanTaskNodeRefList;
    }

    @XmlElement(name="humanTaskNodes")
    public List<HumanTaskNodeRef> getHumanTaskNodeRefList() {
        return humanTaskNodeRefList;
    }

    public void setHumanTaskNodeRefList(List<HumanTaskNodeRef> humanTaskNodeRefList) {
        this.humanTaskNodeRefList = humanTaskNodeRefList;
    }

    @XmlElement(name = "totalCount")
    public int getTotalCount(){
        return humanTaskNodeRefList.size();
    }


}
