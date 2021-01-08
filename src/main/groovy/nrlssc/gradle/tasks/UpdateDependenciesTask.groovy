package nrlssc.gradle.tasks

import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.ProjectsSettingsPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UpdateDependenciesTask extends DefaultTask {
    private static Logger logger = LoggerFactory.getLogger(UpdateDependenciesTask.class)

    boolean checkRC = false

    static UpdateDependenciesTask createFor(Project project)
    {
        UpdateDependenciesTask task = project.tasks.create("updateDependencies", UpdateDependenciesTask.class)
        task.group = HGitPlugin.TASK_GROUP
        task.description = 'Updates all dependencies to latest release version.'
        return task
    }

    static UpdateDependenciesTask createRCFor(Project project)
    {
        UpdateDependenciesTask task = project.tasks.create("updateDependenciesNightly", UpdateDependenciesTask.class)
        task.group = HGitPlugin.TASK_GROUP
        task.description = 'Updates all dependencies to latest version.'
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

        Map<String, String> depReplacer = new HashMap<>()
        upConf.resolvedConfiguration.resolvedArtifacts.each {
            if(it.moduleVersion.toString().split(":")[0].equalsIgnoreCase(project.group.toString())) {
                String[] splits = it.moduleVersion.toString().split(":")
                depReplacer.put("'" + splits[0] + ":" + splits[1] + ":(.*?)'" , "'" + it.moduleVersion.toString() + "'")
            }
        }

        File depFile = project.file("gradle/dependencies.gradle")
        if(!depFile.exists()) depFile = project.file("dependencies.gradle")
        
        if(depFile.exists()) {
            String origDepScript = depFile.text
            String depScript = origDepScript

            File origDep = new File(depFile.absolutePath + ".orig")
            origDep.createNewFile()
            origDep.write(origDepScript)
            
            for (String key : depReplacer.keySet()) {
                depScript = depScript.replaceAll(key, depReplacer.get(key))
            }
            depFile.write(depScript)
        }

        if(project.buildFile.exists()) {
            String oldBuildScript = project.buildFile.text
            String buildScript = oldBuildScript

            File orig = new File(project.buildFile.absolutePath + ".orig")
            orig.createNewFile()
            orig.write(oldBuildScript)

            for (String key : depReplacer.keySet()) {
                buildScript = buildScript.replaceAll(key, depReplacer.get(key))
            }
            project.buildFile.write(buildScript)
        }

    }
}
