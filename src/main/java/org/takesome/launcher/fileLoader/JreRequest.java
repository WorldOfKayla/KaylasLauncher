package org.takesome.launcher.fileLoader;

import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.utils.HTTP.HTTPrequest;
import org.takesome.kaylasEngine.utils.HTTP.HttpParam;

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