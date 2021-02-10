package nrlssc.gradle.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum VersionCache {
    Instance;

    private static Logger logger =  LoggerFactory.getLogger(VersionCache.class)

    Map<String, String> versionMap = new HashMap<>();
    VersionCache(){

    }

    void put(String vcsRoot, String version){
        versionMap.put(vcsRoot, version)
    }

    String get(String vcsRoot){
        String version = versionMap.get(vcsRoot)
        logger.lifecycle("Retrived $version from VersionCache using $vcsRoot")
        return version
    }

    boolean contains(String vcsRoot)
    {
        return versionMap.containsKey(vcsRoot)
    }
}
