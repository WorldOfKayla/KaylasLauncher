package org.takesome.launcher.user.loader;

public class GroupObject {
    private String id;
    private String groupName;
    private String groupColor;
    private int groupNum;
    private String groupType;

    public GroupObject() {
    }

    public GroupObject(String id, String groupName, String groupColor, int groupNum, String groupType) {
        this.id = id;
        this.groupName = groupName;
        this.groupColor = groupColor;
        this.groupNum = groupNum;
        this.groupType = groupType;
    }

    public static GroupObject fromRole(String role, int groupNum) {
        String resolvedRole = role == null || role.isBlank() ? "USER" : role.toUpperCase();
        String color = "ADMIN".equals(resolvedRole) ? "#ff5555" : "#aaaaaa";
        return new GroupObject(resolvedRole.toLowerCase(), resolvedRole, color, groupNum, "backend");
    }

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
