package org.robotv.recordings.model;

public class IconAction {

    private final int actionId;
    private final int resourceId;
    private final String text;

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
