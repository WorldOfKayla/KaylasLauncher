package org.takesome.launcher.auth;

import org.takesome.kaylasEngine.utils.HTTP.RequestState;

/**
 * Lightweight auth request state holder retained for UI compatibility.
 *
 * <p>HTTP auth is disabled; authentication should use KaylasLauncherBackend.</p>
 */
public class AuthRequest {
    private RequestState requestState = RequestState.PENDING;

    public RequestState getRequestState() {
        return requestState;
    }

    public void setRequestState(RequestState requestState) {
        this.requestState = requestState == null ? RequestState.PENDING : requestState;
    }
}
