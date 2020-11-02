package nrlssc.gradle.helpers

import org.slf4j.Logger
import org.slf4j.LoggerFactory


enum VersionScheme {
    Manual,   //Major/Minor versions manually set via hgit{majorVersion,minorVersion}
    CommitMessage,  //Major/Minor versions determined from major++ and minor++ in commit messages 
    Standard  //Major version determined by major++ in commit message on release branch.  Minor version determined by number of commits since last major++

    private static Logger logger = LoggerFactory.getLogger(VersionScheme.class)
    static VersionScheme fromString(String s){
        for(VersionScheme vs : values()){
            if(vs.name().equalsIgnoreCase(s)){
                return vs
            }
        }
        logger.error("Could not parse VersionScheme from string '" + s + "', valid values are '" + values().join(',') + "'.  Defaulting to Standard.")
        return Standard
    }
}