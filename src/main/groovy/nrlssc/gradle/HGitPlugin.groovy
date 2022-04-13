package nrlssc.gradle

import nrlssc.gradle.extensions.HGitExtension
import nrlssc.gradle.extensions.UselessVersionCache
import nrlssc.gradle.extensions.VersionCache
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
import org.gradle.api.provider.Provider
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

        VersionCache versionCache = null
        try{
            Provider<VersionCache> versionCacheProvider = project.getGradle().getSharedServices().registerIfAbsent("versioncache", VersionCache.class, {})
            versionCache = versionCacheProvider.get()
        }catch(Throwable) {
            versionCache = new UselessVersionCache()
        }

        HGitExtension hgit = project.extensions.create("hgit", HGitExtension, project, versionCache)
        hgit.checkManualVersion()

        logger.debug("$project.name has root of $project.rootProject.name")

        project.gradle.projectsEvaluated {
            if (!hgit.fastBuildEnabled() || PluginUtils.containsArtifactTask(project)) {
                project.version = hgit.getProjectVersion()
                logger.lifecycle(project.name + " @ " + project.version)
            } else {
                project.version = getDate()
                if (project.name == project.rootProject.name) {//only run once
                    if (hgit.fastBuildEnabled()) logger.lifecycle("Version Numbers suppressed due to the 'fastBuild' property.")
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
        PushBuildTagTask.createFor(project)


        project.pluginManager.withPlugin('nrlssc.pub'){
            Task t = project.tasks.getByName('publish')
            if (t != null) {
                t.doFirst {
                    HGitExtension ext = project.extensions.getByName('hgit')
                    project.version = ext.getProjectVersion() //SERIOUSLY, do not calc proj version before projects evaluated (it'll cache)
                    project.pub.publishType = ext.isReleaseBranch(ext.fetchBranch()) ? 'release' : 'snapshot'
                }
            }
        }
        //handler for forcing pub publishType when/if configured together
        
        
        //region Update/Lookup Dependencies Configurations
        Project proj = project

        Configuration upConf
        Configuration upConfRel

        try{
            upConf = proj.configurations.getByName(UP_CONFIG)
        }catch(Exception ex){
            upConf = proj.configurations.create(UP_CONFIG)
        }
        try{
            upConfRel = proj.configurations.getByName(UP_REL_CONFIG)
        }catch(Exception ex){
            upConfRel = proj.configurations.create(UP_REL_CONFIG)
        }

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
        upConf.resolutionStrategy {
            cacheDynamicVersionsFor 0, 'seconds'
            cacheChangingModulesFor 0, 'seconds'

            eachDependency { DependencyResolveDetails details ->
                if (details.requested.group == project.group) {
                    details.useVersion '+'
                }
            }
        }

        upConfRel.resolutionStrategy {
            cacheDynamicVersionsFor 0, 'seconds'
            cacheChangingModulesFor 0, 'seconds'

            eachDependency { DependencyResolveDetails details ->
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
