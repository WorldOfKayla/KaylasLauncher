package org.foxesworld.launcher.user.loader;

public class GroupObject {
    private String id;
    private String groupName;
    private String groupColor;
    private int groupNum;
    private String groupType;

    @Override
    public String toString() {
        return "Group{" +
                "id='" + id + '\'' +
                ", groupName='" + groupName + '\'' +
                ", groupNum='" + groupNum + '\'' +
                ", groupType='" + groupType + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getGroupNum() {
        return groupNum;
    }

    public String getGroupType() {
        return groupType;
    }

    public String getGroupColor() {
        return groupColor;
    }
}
