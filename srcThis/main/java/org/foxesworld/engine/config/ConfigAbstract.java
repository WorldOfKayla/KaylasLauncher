package org.foxesworld.engine.config;

import com.foxesworld.cfgProvider.cfgProvider;

import java.util.Map;

public abstract class ConfigAbstract {

    private String cfgFileExtension = "";

    protected void addCfgFiles(String[] configFiles){
        for(String cfgUnit: configFiles){
            String cfgFileName = cfgUnit + cfgFileExtension;
            new cfgProvider(cfgFileName);
        }
    }

    protected void setDirPathIndex(int index){
        cfgProvider.setBaseDirPathIndex(index);
    }
    protected  void setCfgExportDir(String dir){
        cfgProvider.setCfgExportDirName(dir);
    }
    protected void setDebug(boolean debug){ cfgProvider.setDebug(debug);}
    protected void setCfgFileExtension(String ext){
        this.cfgFileExtension = ext;
        cfgProvider.setCfgFileExtension(ext);
    }
    protected Map<String, Map> getAllCfgMaps(){
        return cfgProvider.getAllCfgMaps();

    }

    protected String getFullPath() {
        return cfgProvider.getGameFullPath();
    }
}
