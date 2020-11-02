package nrlssc.gradle

import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.helpers.PluginUtils
import nrlssc.gradle.helpers.VersionScheme
import nrlssc.gradle.tasks.CalculateVersionTask
import nrlssc.gradle.tasks.PullDependenciesTask
import nrlssc.gradle.tasks.RootVersionFileTask
import nrlssc.gradle.tasks.cmd.BranchTask
import nrlssc.gradle.tasks.cmd.FetchTask
import nrlssc.gradle.tasks.cmd.StatusTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HGitPlugin implements Plugin<Project> {
    private static Logger logger = LoggerFactory.getLogger(HGitPlugin.class)

    public final static String TASK_GROUP = 'hgit'
    
    private Project project

    def getDate() {
        def date = new Date()
        def formattedDate = date.format('yyyyMMddHHmmss')
        return formattedDate
    }
    
    @Override
    void apply(Project target) {
        this.project = target

        project.extensions.create("hgit", HGitExtension, project)


        project.gradle.projectsEvaluated {
            HGitExtension ext = project.extensions.getByName('hgit')
            String version = ext.getProjectVersion()
            logger.debug("$project.name has root of $project.rootProject.name")

            if (!ext.fastBuildEnabled() || PluginUtils.containsArtifactTask(project)) {
                project.version = version
                logger.lifecycle(project.name + " @ " + project.version)
            } else {
                project.version = getDate()
                if (project.name == project.rootProject.name) {//only run once
                    if (ext.fastBuildEnabled()) logger.lifecycle("Version Numbers suppressed due to the 'fastBuild' property.")
                    else logger.lifecycle("Version Numbers suppressed during during non-artifact generating tasks.")
                }
            }
        }
        
        RootVersionFileTask.createFor(project)
        CalculateVersionTask.createFor(project)
        BranchTask.createFor(project)
        FetchTask.createFor(project)
        StatusTask.createFor(project)
        PullDependenciesTask.createFor(project)

        
        project.pluginManager.withPlugin('nrlssc.pub'){
            Task t = project.tasks.getByName('publish')
            if (t != null) {
                t.doFirst {
                    HGitExtension ext = project.extensions.getByName('hgit')
                    project.version = ext.getProjectVersion()
                }
            }
            
            HGitExtension ext = project.extensions.getByType(HGitExtension)
            project.pub.publishType = ext.isReleaseBranch(ext.fetchBranch()) ? 'release' : 'snapshot'
        }
        //handler for forcing pub publishType when/if configured together
    }

    
}
