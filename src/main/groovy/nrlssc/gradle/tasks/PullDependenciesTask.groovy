package nrlssc.gradle.tasks

import groovy.json.JsonSlurper
import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.helpers.PluginUtils
import nrlssc.gradle.helpers.PropertyName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PullDependenciesTask extends DefaultTask {
    private static Logger logger = LoggerFactory.getLogger(PullDependenciesTask.class)


    private static String DEP_FILE = 'dependencies.json'

    static PullDependenciesTask createFor(Project project)
    {
        PullDependenciesTask task = project.tasks.create("pullDependencies", PullDependenciesTask.class)
        task.group = HGitPlugin.TASK_GROUP
        task.description = 'Clones/updates sibling repos listed in dependencies.json'
        return task
    }

   

    @TaskAction
    void run()
    {
        //TODO make work with DI2E
        Project project = getProject()
        if(!project.file(DEP_FILE).exists()){
            logger.debug("Skipping $name on $project due to missing $DEP_FILE file")
            return
        }

        HGitExtension hgit = project.extensions.getByType(HGitExtension.class)

        if(hgit.isReleaseBranch(hgit.fetchBranch()) && hgit.isCI())
        {
           //TODO: Need to force static versions here
        }
        
        JsonSlurper slurper = new JsonSlurper()
        Object deps = slurper.parse(project.file(DEP_FILE))

        File cloneRoot = PropertyName.preferCoprojectSubdir.getAsBoolean(project) ? project.file("coprojects") : project.projectDir.parentFile

        if(!cloneRoot.exists()) cloneRoot.mkdirs()

        deps.each {
            String url = it.url
            String name = getRepoName(url)

            if(it.credentialsId && it.credentialsId != null && it.credentialsId.size() > 0){
                String credID = it.credentialsId
                String userParam = "user_$credID"
                String passParam = "pass_$credID"
                
                String creds = null
                
                if(PropertyName.exists(project, userParam) && PropertyName.exists(project, passParam))
                {
                    String un = URLEncoder.encode(PropertyName.getAsString(project, userParam), "UTF-8")
                    String pw = URLEncoder.encode(PropertyName.getAsString(project, passParam), "UTF-8")
                    creds = "$un:$pw"
                }
                else 
                {
                    logger.lifecycle("PullDependencies expected credentials $credID, but found none.  Failure likely.") 
                }
                
    
                if(creds != null && url.startsWith("https://")){
                    url = url.replace("https://", "https://$creds@")
                }
            }
            
            String command = ""
            String extra = ""
            boolean isGit = false
            if(url.endsWith(".git") || url.startsWith("git://"))
            {
                isGit = true
                
            }
            else
            {
                if(!PluginUtils.execute((hgit.getGit() + " ls-remote $url"), project.projectDir, true).contains("fatal"))
                {
                    isGit = true
                }
                else if(!PluginUtils.execute((hgit.getHG() + " identify $url"), project.projectDir, true).contains("abort"))
                {
                    isGit = false
                }
            }


            boolean isClone = true
            File repoDir = new File("$cloneRoot/$name")

            if(repoDir.exists())
            {
                isClone = false
            }
            
            if(isGit)
            {
                command += hgit.getGit()
                if(isClone) extra = " --recursive"
            }
            else 
            {
                command += hgit.getHG()
            }
            
            if(isClone)
            {
                command += " clone$extra $url $name"
            }
            else {
                command += " pull"
                if(!isGit) command += " -u"
            }


            if(isClone)
            {
                logger.lifecycle("Cloning '$name'")
                logger.lifecycle(PluginUtils.execute(command, cloneRoot, true))
            }
            else 
            {
                logger.lifecycle("Pulling '$name'")
                logger.lifecycle(PluginUtils.execute(command, repoDir, true))
                
            }
            
            //update to branch
            String branch = hgit.fetchBranch()
            if(it.branch && it.branch != null && it.branch.size() > 0) branch = it.branch

            if(isGit)
            {
                String checkoutBranch = "develop"

                String branchString = PluginUtils.execute(hgit.getGit() + " branch -a", repoDir, true)
                String[] branches = branchString.split("\n")
                for(String bran : branches)
                {
                    bran = bran.trim()
                    if(bran.startsWith("remotes/origin"))
                    {
                        String actualBran = bran.replace("remotes/origin/", "")
                        
                        if(actualBran.equalsIgnoreCase(branch))
                        {
                            checkoutBranch = branch
                        }
                    }
                }
                logger.lifecycle("Updating to $checkoutBranch")
                logger.lifecycle(PluginUtils.execute(hgit.getGit() + " checkout $checkoutBranch", repoDir, true))
            }
            else {
                String checkoutBranch = "default"
                
                String branchString = PluginUtils.execute(hgit.getHG() + " branches", repoDir, true)
                String[] branches = branchString.split("\n")
                for(String bran : branches)
                {
                    if(bran.split(" ")[0].trim().equalsIgnoreCase(branch))
                    {
                        checkoutBranch = branch
                    }
                }
                
                logger.lifecycle("Updating to $checkoutBranch")
                logger.lifecycle(PluginUtils.execute(hgit.getHG() + " update $checkoutBranch", repoDir, true))
            }
            
        }
        

    }
    
    static String getRepoName(String url)
    {
        String retVal = url.split("/")[-1]
        if(retVal.endsWith(".git"))
        {
            retVal = retVal.replaceAll(/.git$/, "");
        }
        return retVal
    }
    
}
