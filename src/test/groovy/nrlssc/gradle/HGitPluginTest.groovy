package nrlssc.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;


public class HGitPluginTest {

    @Rule
    public TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void apply() {


        File buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'java'
                id 'nrlssc.hgit'
            }
        """

        File settingsFile = testProjectDir.newFile("settings.gradle")
        settingsFile << "rootProject.name = 'sample-project'"

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('calculateVersion')
                .withPluginClasspath()
                .build()

        System.out.println(result.output)
    }

}