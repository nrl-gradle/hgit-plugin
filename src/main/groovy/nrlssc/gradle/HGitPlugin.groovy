package nrlssc.gradle

import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.helpers.PluginUtils
import nrlssc.gradle.helpers.VersionScheme
import nrlssc.gradle.tasks.CalculateVersionTask
import nrlssc.gradle.tasks.LookupDependenciesTask
import nrlssc.gradle.tasks.PullDependenciesTask
import nrlssc.gradle.tasks.PushBuildTagTask
import nrlssc.gradle.tasks.RootVersionFileTask
import nrlssc.gradle.tasks.UpdateDependenciesTask
import nrlssc.gradle.tasks.cmd.BranchTask
import nrlssc.gradle.tasks.cmd.FetchTask
import nrlssc.gradle.tasks.cmd.StatusTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.DependencySubstitution
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.plugins.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Pattern

class HGitPlugin implements Plugin<Project> {
    private static Logger logger = LoggerFactory.getLogger(HGitPlugin.class)

    public final static String TASK_GROUP = 'hgit'
    public static String UP_CONFIG = 'updateResolver'
    public static String UP_REL_CONFIG = 'updateResolverRelease'
    
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
        
//        project.afterEvaluate {
//            HGitExtension ext = project.extensions.getByName('hgit')
//            if(ext.isReleaseBranch(ext.fetchBranch())) {
//                project.configurations.each {
//                    it.resolutionStrategy.dependencySubstitution {
//                        all { DependencySubstitution dependency ->
//                            if (dependency.requested instanceof ModuleComponentSelector) {
//                                dependency.useTarget details.requested.group + ':' + details.requested.module + ':' + details.requested.version
//                            }
//                        }
//                    }
//                }
//            }
//        }
        
        
        RootVersionFileTask.createFor(project)
        CalculateVersionTask.createFor(project)
        BranchTask.createFor(project)
        FetchTask.createFor(project)
        StatusTask.createFor(project)
        PullDependenciesTask.createFor(project)
        PushBuildTagTask.createFor(project)

        
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
        
        
        //region Update/Lookup Dependencies Configurations
        Project proj = project
        Configuration upConf = proj.configurations.create(UP_CONFIG)
        Configuration upConfRel = proj.configurations.create(UP_REL_CONFIG)

        Configuration defConf = proj.configurations.getByName('default')

        upConf.extendsFrom(defConf)
        upConf.transitive = false

        upConfRel.extendsFrom(defConf)
        upConfRel.transitive = false

        proj.configurations.add(upConfRel)
        proj.configurations.add(upConf)

        proj.configurations {
            upConf
            upConfRel
        }
        //upConf.extendsFrom(proj.configurations.getByName('default'))
        upConf.resolutionStrategy {
            cacheDynamicVersionsFor 0, 'seconds'
            cacheChangingModulesFor 0, 'seconds'

            eachDependency { DependencyResolveDetails details ->
                //specifying a fixed version for all libraries with 'org.gradle' group
                if (details.requested.group == project.group) {
                    details.useVersion '+'
                }
            }
        }

        upConfRel.resolutionStrategy {
            cacheDynamicVersionsFor 0, 'seconds'
            cacheChangingModulesFor 0, 'seconds'

            eachDependency { DependencyResolveDetails details ->
                //specifying a fixed version for all libraries with 'org.gradle' group
                if (details.requested.group == project.group) {
                    details.useVersion '+'
                }
            }

            componentSelection {
                all { ComponentSelection selection ->
                    if(selection.candidate.group.equalsIgnoreCase(proj.group.toString())) {
                        if (!selection.candidate.version.matches(Pattern.compile(/(\d+\.?)+/))) {
                            selection.reject("rejecting non-release version ${selection.candidate.version} for ${selection.candidate.module}")
                        }
                    }
                }

            }
        }

        UpdateDependenciesTask.createFor(project)
        UpdateDependenciesTask.createRCFor(project)
        LookupDependenciesTask.createFor(project)
        LookupDependenciesTask.createRCFor(project)
        
        //endregion
        
        
    }
}
