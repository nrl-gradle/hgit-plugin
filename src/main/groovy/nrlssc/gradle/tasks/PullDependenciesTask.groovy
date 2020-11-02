package nrlssc.gradle.tasks

import groovy.json.JsonSlurper
import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.helpers.PluginUtils
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
            logger.debug("Skipping PullDependencies on $project due to missing $DEP_FILE file")
            return
        }

        JsonSlurper slurper = new JsonSlurper()
        Object deps = slurper.parse(project.file(DEP_FILE))
        HGitExtension hgit = project.extensions.getByType(HGitExtension.class)
        

        deps.each {
            String url = it.url
            
            String command = ""
            String extra = ""
            if(url.endsWith(".git") || url.startsWith("git://"))
            {
                command += hgit.getGit()
                extra = " --recursive"
            }
            else
            {
                command += hgit.getHG()
            }
            
            String name = getRepoName(url)
            command += " clone$extra $url ../$name"
            
            if(!project.file("../$name").exists())
            {
                logger.lifecycle("Cloning '$name'")
                logger.lifecycle(PluginUtils.execute(command, project.projectDir))
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
