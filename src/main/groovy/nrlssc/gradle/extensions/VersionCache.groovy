package nrlssc.gradle.extensions

enum VersionCache {
    Instance;


    Map<String, String> versionMap = new HashMap<>();
    VersionCache(){

    }

    void put(String vcsRoot, String version){
        versionMap.put(vcsRoot, version)
    }

    String get(String vcsRoot){
        return versionMap.get(vcsRoot)
    }

    boolean contains(String vcsRoot)
    {
        return versionMap.containsKey(vcsRoot)
    }
}
