package org.foxesworld.launcher.user.loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;
import org.foxesworld.launcher.user.User;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroupLoader extends HTTPrequest  {
    @HttpParam
    private final String sysRequest = "getGroups";
    private final  User user;
    private GroupObject userGroupObject;
    public GroupLoader(User user) {
        super(user.getLauncher(), "POST");
        this.user = user;
    }

    public void getUserGroup(){
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        CompletableFuture<Void> future = sendAsyncCF(Collections.emptyMap())
                .thenAccept(this::handleGroupLoad)
                .exceptionally(error -> {
                    handleGroupLoadError(error);
                    return null;
                });
        futures.add(future);
    }

    private void handleGroupLoad(Object response) {
        int userGroup = Integer.parseInt(String.valueOf(this.user.getUserAttributes().getGroup()));
        this.findGroupByNum(String.valueOf(response), userGroup);
    }

    /**
     * Метод поиска группы по номеру
     * @param json JSON-строка
     * @param targetGroupNum номер группы для поиска
     * @return объект Group с совпадающим groupNum, либо null
     */
    public void findGroupByNum(String json, int targetGroupNum) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<GroupObject>>(){}.getType();
        List<GroupObject> groups = gson.fromJson(json, listType);

        for (GroupObject group : groups) {
            if (group.getGroupNum() == targetGroupNum) {
                this.userGroupObject = group;
            }
        }
    }

    private void handleGroupLoadError(Throwable error) {
        Engine.getLOGGER().error("Group load request failed for {}: {}",  user.getLogin(), error.getMessage());
    }

    public GroupObject getUserGroupObject() {
        return userGroupObject;
    }
}
