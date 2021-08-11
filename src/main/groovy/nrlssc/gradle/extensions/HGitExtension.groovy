package nrlssc.gradle.extensions

import nrlssc.gradle.helpers.PluginUtils
import nrlssc.gradle.helpers.PropertyName
import nrlssc.gradle.helpers.VersionScheme
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher
import java.util.regex.Pattern


class HGitExtension {
    private static Logger logger = LoggerFactory.getLogger(HGitExtension.class)
    
    boolean forceHg = false
    boolean forceGit = false
    
    VersionScheme versionScheme = VersionScheme.Standard
    String prereleaseString = 'SNAPSHOT'
    
    private List<String> relBranches = ['release', 'origin/release']
    private List<String> rcBranches = ['release-candidate', 'origin/release-candidate']
    private List<String> intBranches = ['default', 'origin/default', 'develop', 'origin/develop']
    private Pattern relBranchPattern = null
    private Pattern rcBranchPattern = null
    private Pattern intBranchPattern = null
    String rcQualifier = 'rc'
    String intQualifier = 'dev'

    private boolean fastBuild = false

    private Project project
    private VersionCache versionCache

    HGitExtension(Project project, VersionCache cache)
    {
        this.project = project
        this.versionCache = cache
    }
    

    //region getters/setters
    String getRelBranch() {
        return relBranches.join(",")
    }
    
    void setRelBranch(String branchName){
        relBranches = [branchName]
    }
    
    List<String> getRelBranches(){
        return relBranches
    }
    
    void setRelBranches(List<String> branches){
        relBranches = branches
    }

    String getRcBranch() {
        return rcBranches.join(",")
    }

    void setRcBranch(String branchName){
        rcBranches = [branchName]
    }

    List<String> getRcBranches() {
        return rcBranches
    }

    void setRcBranches(List<String> rcBranches) {
        this.rcBranches = rcBranches
    }

    String getIntBranch() {
        return intBranches.join(",")
    }

    void setIntBranch(String branchName){
        intBranches = [branchName]
    }

    List<String> getIntBranches() {
        return intBranches
    }

    void setIntBranches(List<String> intBranches) {
        this.intBranches = intBranches
    }

    Pattern getRelBranchPattern()
    {
        return relBranchPattern
    }

    void setRelBranchPattern(Pattern pattern)
    {
        this.relBranchPattern = pattern
    }

    Pattern getRCBranchPattern()
    {
        return rcBranchPattern
    }

    void setRCBranchPattern(Pattern pattern)
    {
        this.rcBranchPattern = pattern
    }

    Pattern getIntBranchPattern()
    {
        return intBranchPattern
    }

    void setIntBranchPattern(Pattern pattern)
    {
        this.intBranchPattern = pattern
    }


    private boolean forceManual = false
    private boolean versionCachedOrFailed = false
    private int major
    private int minor

    void checkManualVersion()
    {
        if(!versionCachedOrFailed)
        {
            boolean stopLooking = false
            Project p = project
            while(!stopLooking)
            {
                File f = project.file("version.gradle")
                if(f.exists())
                {
                    String ver = f.text.trim()
                    try{
                        String[] splits = ver.split("\\.");
                        major = Integer.parseInt(splits[0])
                        minor = Integer.parseInt(splits[1])
                        forceManual = true
                        stopLooking = true
                    }catch(ignored){
                        logger.error("Invalid version $ver in ${f.getPath()}");
                    }
                }
                if(p.getParent() != null)
                {
                    p = p.getParent()
                }
                else
                {
                    stopLooking = true
                }
            }
            versionCachedOrFailed = true
        }
    }

    int getMajorVersion() {
        checkManualVersion()
        return major
    }

    int getMinorVersion() {
        checkManualVersion()
        return minor
    }

    void setFastBuild(boolean fb){
        fastBuild = fb
    }

    void setFastBuild(String fb){
        fastBuild = fb.toBoolean()
    }

    VersionScheme getVersionScheme(){
        VersionScheme scheme = versionScheme

        if(forceManual){
            scheme = VersionScheme.Manual
        }

        return scheme
    }

    void setVersionScheme(VersionScheme scheme){
        this.versionScheme = scheme
    }

    void setVersionScheme(String scheme){
        this.versionScheme = VersionScheme.fromString(scheme)
    }

    String getRootVersion(){
        return fetchRootVersion()
    }
    
    //endregion getters/setters


    boolean isReleaseBranch(String branch){
        if(relBranchPattern != null && branch.matches(relBranchPattern)) return true;

        for(String thisBranch in relBranches){
            if(thisBranch.equalsIgnoreCase(branch)) return true
        }
        return false
    }

    boolean isRCBranch(String branch){
        if(rcBranchPattern != null && branch.matches(rcBranchPattern)) return true;

        for(String thisBranch in rcBranches){
            if(thisBranch.equalsIgnoreCase(branch)) return true
        }
        return false
    }
    
    boolean isIntBranch(String branch){
        if(intBranchPattern != null && branch.matches(intBranchPattern)) return true;

        for(String thisBranch in intBranches){
            if(thisBranch.equalsIgnoreCase(branch)) return true
        }
        return false
    }


    String getGit()
    {
        try {
            String gitCom = PropertyName.gitCommandPath.getAsString(project)
            if (gitCom == null || gitCom.length() == 0) gitCom = 'git'
            return gitCom
        }catch(Exception ex)
        {
            return 'git'
        }
    }
    
    String getHG()
    {
        try {
            String hgCom = PropertyName.hgCommandPath.getAsString(project)
            if (hgCom == null || hgCom.length() == 0) hgCom = 'hg'
            return hgCom
        }catch(Exception ex)
        {
            return 'hg'
        }
    }

    String getCVS()
    {
        if(forceHg)
        {
            return 'hg'
        }
        if(forceGit)
        {
            return 'git'
        }
        
        File f = project.rootDir
        while(f != null)
        {
            if(new File(f, ".hg").exists()) return 'hg'
            if(new File(f, ".git").exists()) return 'git'
            f = f.getParentFile()
        }
        return ''
    }
    
    String fetchRootVersion(){
        try {
            switch (getCVS())
            {
                case 'hg':
                    return PluginUtils.execute([getHG(), "id", "--num", "-r", "branch(.)"], project.rootProject.rootDir)
                case 'git':
                    return PluginUtils.execute([getGit(), 'rev-list', '--count', '--first-parent', 'HEAD'], project.rootProject.rootDir)
                default:
                    return '0'
            }
            
        }
        catch (Exception ex)
        {
            return '0'
        }
    }

    String fetchMajorVersionHG(){
        String version = '0'
        for(String branch : relBranches){
            try{
                return fetchMajorVersionHG(branch)
            } 
            catch (Exception ex) {
                
            }
        }
        return version
    }
    
    String fetchMajorVersionHG(String branchName){
        String version = '0'
        VersionScheme scheme = getVersionScheme()

        if(scheme == VersionScheme.Standard || scheme == VersionScheme.CommitMessage) {
            
            def command = [getHG(),
                           "log",
                           "-r",
                           "branch('$branchName') and desc('major++') and ancestors(.)",
                           "--template",
                           "{rev}|"]
            String retText = PluginUtils.execute(command, project.projectDir)

            int ver = 0
            if (retText.contains("abort")) {
                throw new Exception("invalid branch")
            } else {
                ver = retText.tokenize("|").size()
            }
            version = ver
        }
        return version
    }
    
    String fetchMinorVersionHG(){
        String version = '0'
        VersionScheme scheme = getVersionScheme()

        if(scheme == VersionScheme.CommitMessage) {
            for(String branch in relBranches){
                try {
                    return fetchCMMinorVersionHG(branch)
                }
                catch (Exception ex) {
                }
            }
        }
        else if(scheme == VersionScheme.Standard)
        {
            for(String branch in relBranches){
                try {
                    return fetchStandardMinorVersionHG(branch)
                }
                catch (Exception ex) {
                }
            }
        }
        
        return version

    }
    
    String fetchStandardMinorVersionHG(String branch){
        String version = '0'
        def command = [getHG(),
                       "log",
                       "-r",
                       "(max(branch('$branch') and desc('major++') and ancestors(.)):max(branch('$branch') and ancestors(.)) and branch('$branch'))",
                       "--template",
                       "{rev}|"]
        String retText = PluginUtils.execute(command, project.projectDir)

        int ver = 0
        if (retText.contains("abort")) {
            ver = 0
        } else {
            ver = retText.tokenize("|").size()
        }

        String tmp = null
        if((tmp = PluginUtils.execute([getHG(), 'log', '-r', "max(branch('$branch'))", '--template', '{rev}|'], project.projectDir)).contains("|") && ver == 0)
        {
            ver = tmp.tokenize("|").size()
        }

        version = ver
        return version
    }
    
    String fetchCMMinorVersionHG(String branch){
        String version = '0'
        def command = [getHG(),
                       "log",
                       "-r",
                       "(max(branch('$branch') and desc('major++') and ancestors(.)):max(branch('$branch') and ancestors(.))) and desc('minor++') and branch('$branch')",
                       "--template",
                       "{rev}|"]
        String retText = PluginUtils.execute(command, project.projectDir)

        int ver = 0
        if (retText.contains("abort")) {
            throw new Exception("invalid branch")
        } else {
            ver = retText.tokenize("|").size()
        }
        version = ver
        
        return version
    }

    String fetchBranch()
    {
        try {
            switch (getCVS())
            {
                case 'hg':
                    return PluginUtils.execute([getHG(), 'branch'], project.projectDir)
                case 'git':
                    return PluginUtils.execute([getGit(), 'rev-parse', '--abbrev-ref', 'HEAD'], project.projectDir)
            }
            
        }catch (Exception ex)
        {
            return ''
        }
    }

    String fetchPatchVersion()
    {
        try {
            switch (getCVS()) {
                case 'hg':
                    String retVal = PluginUtils.execute ( [ getHG ( ) , 'id', '--num' ], project.projectDir )
                    if ( retVal.contains ( "+" ) )
                    {
                    retVal = retVal.substring ( 0, retVal.indexOf ("+"))
                    }
                    return retVal
                case 'git':
                    return PluginUtils.execute([getGit(), 'rev-list', '--count', '--first-parent', 'HEAD'], project.rootProject.rootDir)
                default:
                    return '0'
            }
        }
        catch (Exception ex){
            return '0'
        }
    }
    
    String fetchGitPrimaryVersion()
    {
        try{
            Pattern p = Pattern.compile('[Vv](\\d+\\.\\d+).*')
            String tagStr = PluginUtils.execute([getGit(), 'tag', '--list', '--sort=-v:refname', '--merged'], project.projectDir)

            String[] splits = tagStr.split("\n")

            for(String tag : splits) {

                Matcher m = p.matcher(tag)
                if (m.matches()) {
                    return m.group(1)
                }
            }
        }catch(Exception ex) {}
        
        return '0.0'
    }

    private static boolean isSpecialBranch(String branch){
        return branch.startsWith("feature/") || branch.startsWith("bugfix/") || branch.startsWith("hotfix/") || branch.startsWith("PR-")
    }

    private static String getBetaQualifier(String branch){
        String qualifier = ""
        String namid = ""
        if (branch.startsWith("feature/")) {
            qualifier += "f"
            namid = branch.replace("feature/", "")
        } else if (branch.startsWith("bugfix/")) {
            qualifier += "bf"
            namid = branch.replace("bugfix/", "")

        } else if (branch.startsWith("hotfix/")) {
            qualifier += "hf"
            namid = branch.replace("hotfix/", "")
        } else if (branch.startsWith("PR-")) {
            qualifier += branch.toLowerCase().replace("-", ".")
        } else{
            def matcher = branch =~ /[a-zA-Z0-9_]+/

            for(def match in matcher){
                qualifier += match + '.'
            }

            if(qualifier.endsWith('.')) qualifier = qualifier.substring(0, qualifier.length() - 1)
        }
        
        
        if(namid.length() > 0)
        {
            String[] splits = namid.split("-")
            namid = ""
            for(int i = 0; i < splits.length; i++)
            {
                namid += "." + splits[i]
                if(namid == 1) break
            }
        }
        
        if(namid.length() > 0)
        {
            qualifier += namid
        }
        
        return qualifier
    }

    String getVCSRoot(){
        switch (getCVS()){
            case 'hg':
                return PluginUtils.execute([getHG(), 'root'], project.projectDir)
            case 'git':
                return PluginUtils.execute([getGit(), 'rev-parse', '--show-toplevel'], project.projectDir)
            default:
                return project.projectDir.getAbsolutePath()
        }
    }

    String getProjectVersion(){
        String vcsRoot = getVCSRoot()
        if(versionCache.contains(vcsRoot)) {
            return versionCache.get(vcsRoot)
        }


        String version = "0.0"
        try {
            String patch = fetchPatchVersion()

            String qualifier
            String branch = fetchBranch()



            if (isReleaseBranch(branch)) {
                qualifier = ''
            } else if (isRCBranch(branch)) {
                qualifier = rcQualifier
            } else if (isIntBranch(branch)) {
                qualifier = intQualifier
            } else {
                qualifier = getBetaQualifier(branch)
            }

            if(isSpecialBranch(branch))
            {
                if(qualifier != '') qualifier = qualifier + "-" + getBetaQualifier(branch)
                else qualifier = getBetaQualifier(branch)
            }
            
            if(!isCI())
            {
                String localQualifier = System.properties['user.name']
                if(localQualifier == null || localQualifier.length() < 1)
                {
                    localQualifier = "unstable"
                }
                qualifier = localQualifier + (qualifier.length() > 0 ? ".$qualifier" : "")
            }
            
            if(qualifier.length() > 0)
            {
                qualifier = "-" + qualifier
            }
            
            String cvsType = getCVS()
            logger.debug("CVS type for project $project.name is $cvsType")
    
            String snapString = ""
            if (!isReleaseBranch(branch)){
                 snapString = '-' + prereleaseString
            }
            
            if(getVersionScheme() == VersionScheme.Manual){
                version = majorVersion + '.' + minorVersion
            }
            else {
                switch (cvsType) {
                    case 'hg':
                        version = fetchMajorVersionHG() + '.' + fetchMinorVersionHG()
                        
                        break;
                    case 'git':
                        version = fetchGitPrimaryVersion()
                        
                        break;
                }
            }
            
            version += "." + patch + qualifier + snapString
            versionCache.put(vcsRoot, version)
        }catch(Exception ex)
        {
            version = "unknown"
        }


        return version
    }
    
    String getOriginUrl(){
        return PluginUtils.execute([getGit(), 'remote', 'get-url', 'origin'], project.projectDir)
    }
    
    void gitPushVersionTag(String url){
        String ver = getProjectVersion()
        String tag = "build_$ver"
        if(tag.endsWith("-$prereleaseString")){
            tag = tag.substring(0, tag.length() - "-$prereleaseString".length())
        }
        if(tag.length() >= 100)
        {
            String shortHash = PluginUtils.execute([getGit(), 'rev-parse',  '--short=10', 'HEAD'], project.projectDir)
            tag = tag.substring(0, 99 - shortHash.length()) + ".$shortHash"
        }
        logger.lifecycle("Pushing tag: $tag")
        PluginUtils.execute([getGit(), 'tag', tag], project.projectDir)
        PluginUtils.execute([getGit(), 'push', url, '--tags'], project.projectDir, true)
    }

    boolean isCI()
    {
        return PropertyName.isCI.getAsBoolean(project)
    }


    boolean fastBuildEnabled()
    {
        return !isCI() &&
            (
                    fastBuild || PropertyName.fastBuild.getAsBoolean(project)
            )
    }
}
