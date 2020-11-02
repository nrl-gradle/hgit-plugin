package nrlssc.gradle.tasks.cmd

import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.helpers.PluginUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by scraft on 3/10/2017.
 */
class StatusTask extends DefaultTask{
    private static Logger logger = LoggerFactory.getLogger(StatusTask.class)

    static StatusTask createFor(Project project)
    {
        StatusTask task = project.tasks.create("hgStatus", StatusTask.class)
        task.outputs.file("$project.rootProject.buildDir/tmp/hg") //this is literally just to enforce non-parallelization (and therefore non-deterministic order) of this task
        task.outputs.upToDateWhen {false}
        task.group = HGitPlugin.TASK_GROUP
        task.description = "Runs 'hg status' on all projects"
        return task
    }


    static String statusWithFeedback(Project project){
        HGitExtension ext = project.extensions.getByType(HGitExtension.class)
        //String initRev = nrl.fetchHgVersion()
        String command = ext.getHG() + ' status'
        return PluginUtils.execute(command, project.projectDir)
    }

    @TaskAction
    void status()
    {
        Project project = getProject()
        File[] fls = project.projectDir.listFiles()
        if(fls != null)
        {
            boolean foundHg = false
            for (File f : fls){
                if(f.isDirectory() && f.name == ".hg"){
                    foundHg = true
                    break
                }
            }

            if(foundHg)
            {
                logger.lifecycle("Status of $project.name")
                logger.lifecycle(statusWithFeedback(project))
            }
        }
    }
}
