package org.foxesworld.launcher.FileLoader;

import org.foxesworld.engine.Engine;
import org.foxesworld.engine.utils.HTTP.HTTPrequest;

public class FileLoaderThread {

    private HTTPrequest GETrequest;

    public FileLoaderThread(Engine engine){
        this.GETrequest = engine.getGETrequest();
    }

    public void run(String localPath){
        System.out.println(localPath);
    }
}
