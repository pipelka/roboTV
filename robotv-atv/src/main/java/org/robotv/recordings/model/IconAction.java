package org.robotv.recordings.model;

public class IconAction {

    private int actionId;
    private int resourceId;
    private String text;

    public IconAction(int actionId, int resourceId, String text) {
        this.actionId = actionId;
        this.resourceId = resourceId;
        this.text = text;
    }

    public int getActionId() {
        return actionId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getText() {
        return text;
    }
}
