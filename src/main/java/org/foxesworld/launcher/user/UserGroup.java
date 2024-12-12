package org.foxesworld.launcher.user;

public enum UserGroup {
    ADMIN(1),
    USER(4),
    GUEST(5),
    TESTER(3);

    private final int groupId;

    UserGroup(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupId() {
        return groupId;
    }

    public static UserGroup fromGroupId(int groupId) {
        for (UserGroup group : values()) {
            if (group.getGroupId() == groupId) {
                return group;
            }
        }
        throw new IllegalArgumentException("Invalid group ID: " + groupId);
    }
}
