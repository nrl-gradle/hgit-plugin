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
    private int majorVersion = -1
    private int minorVersion = -1
    
    private List<String> relBranches = ['release', 'origin/release']
    private List<String> rcBranches = ['release-candidate', 'origin/release-candidate']
    private List<String> intBranches = ['default', 'origin/default', 'develop', 'origin/develop']
    String rcQualifier = 'rc'
    String intQualifier = 'dev'

    private boolean fastBuild = false

    private Project project
    
    HGitExtension(Project project)
    {
        this.project = project
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
        relBranches = [branchName]
    }

    List<String> getIntBranches() {
        return intBranches
    }

    void setIntBranches(List<String> intBranches) {
        this.intBranches = intBranches
    }

    int getMajorVersion() {
        return majorVersion
    }

    private boolean forceManual = false
    void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion
        forceManual = true
    }

    void setMajorVersion(String majorVersion) {
        setMajorVersion(Integer.parseInt(majorVersion))
    }

    void setFastBuild(boolean fb){
        fastBuild = fb
    }

    void setFastBuild(String fb){
        fastBuild = fb.toBoolean()
    }

    int getMinorVersion() {
        return minorVersion
    }

    void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion
        versionScheme = VersionScheme.Manual
        forceManual = true
    }

    void setMinorVersion(String minorVersion) {
        setMinorVersion(Integer.parseInt(minorVersion))
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
        for(String thisBranch in relBranches){
            if(thisBranch == branch) return true
        }
        return false
    }

    boolean isRCBranch(String branch){
        for(String thisBranch in rcBranches){
            if(thisBranch == branch) return true
        }
        return false
    }
    
    boolean isIntBranch(String branch){
        for(String thisBranch in intBranches){
            if(thisBranch == branch) return true
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

//   String getHGBranchesFilter(List<String> branches) {
//       boolean first = true
//       def branchesString = "("
//       for(String branchNam : relBranches){
//           if(!first) branchesString += " or "
//
//           branchesString += "branch('$branchNam')"
//
//           if(first) first = false
//       }
//       branchesString += ")"
//       return branchesString
//   }

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
            String retVal = PluginUtils.execute([getGit(), 'describe', '--tags'], project.projectDir)
            
            Pattern p = Pattern.compile('[Vv](\\d+\\.\\d+).*')
            Matcher m = p.matcher(retVal)
            if(m.matches())
            {
                retVal = m.group(1)
            }
            else{
                retVal = '0.0'
            }
            return retVal
        }catch(Exception ex)
        {
            return '0.0'
        }
    }

    private static String getBetaQualifier(String branch){
        //TODO grab feature/bugfix/hotfix data from branch name
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
            qualifier = "b"
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
    
    String getProjectVersion(){
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
        }catch(Exception ex)
        {
            version = "unknown"
        }

        return version
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
