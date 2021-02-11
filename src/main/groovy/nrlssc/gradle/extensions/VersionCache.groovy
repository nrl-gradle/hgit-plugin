package nrlssc.gradle.extensions

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

abstract class VersionCache implements BuildService<Params>, AutoCloseable {
    interface Params extends BuildServiceParameters {

    }
    private static Logger logger =  LoggerFactory.getLogger(VersionCache.class)

    Map<String, String> versionMap = new ConcurrentHashMap<>();
    VersionCache(){

    }

    void put(String vcsRoot, String version){
        versionMap.put(vcsRoot, version)
    }

    String get(String vcsRoot){
        logger.lifecycle("Getting $vcsRoot from version cache")
        return versionMap.get(vcsRoot)
    }

    boolean contains(String vcsRoot)
    {
        return versionMap.containsKey(vcsRoot)
    }
}
