package org.takesome.launcher.user.loader;

import org.takesome.kaylasEngine.Engine;
import org.takesome.launcher.auth.AuthResponse;
import org.takesome.launcher.user.User;

public class GroupLoader {
    private final User user;

    public GroupLoader(User user) {
        this.user = user;
        this.getUserGroup();
    }

    public void getUserGroup() {
        AuthResponse response = user.getAuth().getAuthResponse();
        String role = response == null ? user.getUserAttributes().getGroupName() : response.getGroupName();
        int groupNum = parseGroupNum(user.getUserAttributes().getGroup());
        GroupObject groupObject = GroupObject.fromRole(role, groupNum);
        Engine.getLOGGER().info("Loaded backend-managed group for {}: {}", user.getLogin(), groupObject);
        user.setUserGroupLabel(groupObject);
    }

    private int parseGroupNum(Object group) {
        if (group == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(group));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
