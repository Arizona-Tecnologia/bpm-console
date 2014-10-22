package org.jboss.bpm.console.client.model;

/**
 * Key/value of a process instance dataset (used for JSON serialization purposes).
 */
public class KeyValue {

    private String key;

    private Object value;

    private String type;

    public KeyValue() {
    }

    public KeyValue(final String key, final Object value, final String type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
