package nrlssc.gradle.extensions

import org.gradle.api.services.BuildServiceParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UselessVersionCache extends VersionCache{
    private static final Logger logger = LoggerFactory.getLogger(UselessVersionCache.class)

    @Override
    boolean contains(String vcsRoot) {
        return false
    }

    @Override
    BuildServiceParameters.None getParameters() {
        return BuildServiceParameters.None
    }
}
