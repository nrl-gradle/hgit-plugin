package nrlssc.gradle.tasks

import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.helpers.PropertyName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PushBuildTagTask extends DefaultTask {
    private static Logger logger = LoggerFactory.getLogger(PushBuildTagTask.class)



    static PushBuildTagTask createFor(Project project)
    {
        PushBuildTagTask task = project.tasks.create("pushBuildTag", PushBuildTagTask.class)
        task.group = HGitPlugin.TASK_GROUP
        task.description = 'Pushes build version tag to git origin repository.'
        return task
    }

   

    @TaskAction
    void run()
    {
        Project project = getProject()
        
        HGitExtension hgit = project.extensions.getByType(HGitExtension.class)

        if(!hgit.isReleaseBranch(hgit.fetchBranch()) || !hgit.isCI())
        {
            logger.lifecycle("Skipping PushBuildTag task, as this is not a release build.")
            return
        }
        
        
        String url = hgit.getOriginUrl()

        String userParam = "userSCM"
        String passParam = "passSCM"
         
        String creds = null
        
        if(PropertyName.exists(project, userParam) && PropertyName.exists(project, passParam))
        {
            String un = URLEncoder.encode(PropertyName.getAsString(project, userParam), "UTF-8")
            String pw = URLEncoder.encode(PropertyName.getAsString(project, passParam), "UTF-8")
            creds = "$un:$pw"
        }
        
        if(creds != null && url.startsWith("https://")){
            url = url.replace("https://", "https://$creds@")
        }
        
        try {
            hgit.gitPushVersionTag(url)
        }
        catch (Exception ex)
        {
            logger.error("Failed PushBuildTag.  This is frequently caused because of a failure to specify the 'userSCM' and 'passSCM' properties that are needed to push to https Git repos.")
        }
    }
    
}
