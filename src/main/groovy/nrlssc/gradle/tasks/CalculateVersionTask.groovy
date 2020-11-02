package nrlssc.gradle.tasks

import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.extensions.HGitExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CalculateVersionTask extends DefaultTask {
    private static Logger logger = LoggerFactory.getLogger(CalculateVersionTask.class)

    static CalculateVersionTask createFor(Project project)
    {
        CalculateVersionTask task = project.tasks.create("calculateVersion", CalculateVersionTask.class)
        task.group = HGitPlugin.TASK_GROUP
        task.description = 'Displays the project name and calculated version'
        return task
    }

    @TaskAction
    void run()
    {
        Project project = getProject()
        HGitExtension ext = project.extensions.getByType(HGitExtension.class)
        logger.lifecycle(project.name + " @ " + ext.getProjectVersion())
    }
}
