package org.foxesworld.launcher.fileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;
import org.foxesworld.engine.utils.HTTP.HttpParam;

public class JreRequest extends HTTPrequest {

    @HttpParam
    private final String sysRequest = "getJre", jreVersion, jrePlatform, bitDepth;

    public JreRequest(Engine engine, String jreVersion, String jrePlatform, String bitDepth) {
        super(engine, "POST");
        this.jreVersion = jreVersion;
        this.jrePlatform = jrePlatform;
        this.bitDepth = bitDepth;
    }

}