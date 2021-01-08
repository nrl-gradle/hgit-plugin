package nrlssc.gradle.tasks

import nrlssc.gradle.ProjectsSettingsPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LookupDependenciesTask extends DefaultTask {
    private static Logger logger = LoggerFactory.getLogger(LookupDependenciesTask.class)

    boolean checkRC = false
    static String NRL_GROUP = 'nrl'

    static LookupDependenciesTask createFor(Project project)
    {
        LookupDependenciesTask task = project.tasks.create("lookupDependencies", LookupDependenciesTask.class)
        task.group = NRL_GROUP
        task.description = 'Lists latest release version of all (non-transitive) dependencies in your group.'
        return task
    }

    static LookupDependenciesTask createRCFor(Project project)
    {
        LookupDependenciesTask task = project.tasks.create("lookupDependenciesNightly", LookupDependenciesTask.class)
        task.group = NRL_GROUP
        task.description = 'List latest version of all (non-transitive) dependencies in your group.'
        task.checkRC = true
        return task
    }

    @TaskAction
    void run()
    {
        Project project = getProject()
        
        Configuration upConf = checkRC ? 
                project.configurations.getByName(ProjectsSettingsPlugin.UP_CONFIG) :
                project.configurations.getByName(ProjectsSettingsPlugin.UP_REL_CONFIG)
        
        println "\nLatest Dependency Versions:"
        upConf.resolvedConfiguration.resolvedArtifacts.each {
            if(it.moduleVersion.toString().split(":")[0].equalsIgnoreCase(project.group.toString())) {
                println "${it.moduleVersion}"
            }
        }

    }
}
