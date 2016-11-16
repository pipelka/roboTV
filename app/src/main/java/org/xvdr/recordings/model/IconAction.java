package org.xvdr.recordings.model;

public class IconAction {

    private int actionId;
    private int resourceId;
    private String text;
    private String text2;

    public IconAction(int actionId, int resourceId, String text) {
        this(actionId, resourceId, text, null);
    }

    public IconAction(int actionId, int resourceId, String text, String text2) {
        this.actionId = actionId;
        this.resourceId = resourceId;
        this.text = text;
        this.text2 = text2;
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

    public String getText2() {
        return text2;
    }
}
