package nrlssc.gradle.tasks

import nrlssc.gradle.HGitPlugin
import nrlssc.gradle.extensions.HGitExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Best way to use this is to have your archive task (ie 'jar') dependOn 'rootVersionFile'
 */
class RootVersionFileTask extends DefaultTask {
    
    static RootVersionFileTask createFor(Project proj)
    {
        RootVersionFileTask rv = proj.tasks.create("rootVersionFile", RootVersionFileTask.class)
        rv.group = HGitPlugin.TASK_GROUP
        rv.description = "Creates root version file for bundling in distributions"

        return rv
    }

    File outputFile = null

    void setOutputFile(File fl)
    {
        this.outputFile = fl
    }

    @OutputFile
    File getOutputFile()
    {
        if(outputFile == null)
        {
            Project project = getProject()
            outputFile = project.file("$project.buildDir/VERSION")
        }
        return outputFile
    }

    @TaskAction
    void createFile()
    {
        Project project = getProject()
        HGitExtension hgit = project.extensions.getByType(HGitExtension.class)
        String version = hgit.fetchRootVersion()

        File verFile = getOutputFile()
        if(verFile.exists())
        {
            verFile.delete()
        }
        if(!verFile.parentFile.exists())
        {
            verFile.parentFile.mkdirs()
        }
        verFile << "$version"
    }
}
