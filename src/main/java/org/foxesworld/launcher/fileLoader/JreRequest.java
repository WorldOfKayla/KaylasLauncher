package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

public class JreRequest extends HTTPrequest {

    @HttpParam
    private final String sysRequest = "getJre", jreVersion;

    public JreRequest(Engine engine, String jreVersion) {
        super(engine, "POST");
        this.jreVersion = jreVersion;
    }

}