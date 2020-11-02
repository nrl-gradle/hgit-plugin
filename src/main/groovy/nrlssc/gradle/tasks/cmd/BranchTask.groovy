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
class BranchTask extends DefaultTask{
    private static Logger logger = LoggerFactory.getLogger(BranchTask.class)

    static BranchTask createFor(Project project)
    {
        BranchTask task = project.tasks.create("hgBranch", BranchTask.class)
        task.outputs.file("$project.rootProject.buildDir/tmp/hg") //this is literally just to enforce non-parallelization (and therefore non-deterministic order) of this task
        task.outputs.upToDateWhen {false}
        task.group = HGitPlugin.TASK_GROUP
        task.description = "Runs 'hg branch' on all projects"
        return task
    }


    static String branchWithFeedback(Project project){
        HGitExtension nrl = project.extensions.getByType(HGitExtension.class)
        //String initRev = nrl.fetchHgVersion()
        String command = nrl.getHG() + ' branch'
        return PluginUtils.execute(command, project.projectDir)
    }

    @TaskAction
    void branch()
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
                logger.lifecycle("Branch of $project.name")
                logger.lifecycle(branchWithFeedback(project))
            }
        }
    }
}
