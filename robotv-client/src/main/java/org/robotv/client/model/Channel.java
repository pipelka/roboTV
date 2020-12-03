package org.robotv.client.model;

public class Channel {

    private final int number;
    private final String name;
    private final int uid;
    private final int caid;
    private final String iconURL;
    private final String serviceReference;
    private String groupName;
    private boolean radio = false;
    private String channelUri;
    private String inputId = null;

    public Channel(int number, String name, int uid, int caid, String iconUrl, String serviceReference) {
        this.number = number;
        this.name = name;
        this.uid = uid;
        this.caid = caid;
        this.iconURL = iconUrl;
        this.serviceReference = serviceReference;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getUid() {
        return uid;
    }

    public int getCaid() {
        return caid;
    }

    public String getIconURL() {
        return iconURL;
    }

    public String getServiceReference() {
        return serviceReference;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setRadio(boolean radio) {
        this.radio = radio;
    }

    public boolean isRadio() {
        return radio;
    }

    public String getChannelUri() {
        return channelUri;
    }

    public void setChannelUri(String channelUri) {
        this.channelUri = channelUri;
    }

    public String getInputId() {
        return inputId;
    }

    public void setInputId(String inputId) {
        this.inputId = inputId;
    }
}
