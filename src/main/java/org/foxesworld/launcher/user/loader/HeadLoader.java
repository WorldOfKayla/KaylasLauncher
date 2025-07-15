package org.foxesworld.launcher.user.loader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;
import org.foxesworld.engine.utils.HTTP.OnFailure;
import org.foxesworld.engine.utils.HTTP.OnSuccess;
import org.foxesworld.launcher.user.User;

import java.util.Map;


/**
 * HeadLoader is responsible for asynchronously fetching user head data.
 * <p>
 * This class extends {@link HTTPrequest} to leverage the modern CompletableFuture-based
 * asynchronous HTTP API. It uses annotations to automatically include HTTP parameters
 * and sends requests using the updated {@link HTTPrequest#sendAsyncCF(Map)} method.
 * </p>
 */
public class HeadLoader extends HTTPrequest {

    @HttpParam
    private String login;

    @HttpParam
    private final String sysRequest = "userHead";

    private Engine engine;
    /**
     * Constructs a new HeadLoader instance.
     *
     * @param user        the Engine instance used for configuration and logging
     * @param requestMethod the HTTP request method (e.g., "GET")
     */
    public HeadLoader(User user, String requestMethod) {
        super(user.getLauncher(), requestMethod);
        this.engine = user.getLauncher().getEngine();
    }

    /**
     * Asynchronously fetches the user head data for the specified login.
     * <p>
     * This method validates the login parameter and sends an HTTP request using the updated
     * CompletableFuture-based API. On success, the {@code onSuccess} callback is invoked with
     * the server response; on failure, the {@code onFailure} callback is triggered.
     * </p>
     *
     * @param login     the user login identifier
     * @param onSuccess callback to be executed if the request is successful
     * @param onFailure callback to be executed if the request fails
     */
    public void getUserHeadAsync(String login, OnSuccess<String> onSuccess, OnFailure onFailure) {
        this.login = login;
        if (login == null || login.isEmpty()) {
            Engine.getLOGGER().warn("Login is null or empty in getUserHead");
            if (onFailure != null) {
                onFailure.onFailure(new IllegalArgumentException("Login cannot be null or empty"));
            }
            return;
        }
        this.sendAsyncCF(Map.of())
                .thenAccept(response -> {
                    if (response != null && !response.isEmpty()) {
                        onSuccess.onSuccess(response);
                    } else {
                        Engine.getLOGGER().warn("Received empty or null response for user head request for login: {}", login);
                        if (onFailure != null) {
                            onFailure.onFailure(new Exception("Received empty or null response"));
                        }
                    }
                })
                .exceptionally(e -> {
                    Engine.getLOGGER().error("Error while sending user head request for login: {}", login, e);
                    if (onFailure != null) {
                        onFailure.onFailure((Exception) e);
                    }
                    return null;
                });
    }
}
