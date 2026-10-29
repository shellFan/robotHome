package com.robot.home;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildProvenanceTest {

    @Test
    void packagedResourcesExposeGitAndArtifactBuildIdentity() throws Exception {
        Properties git = load("git.properties");
        Properties build = load("META-INF/build-info.properties");

        String commit = git.getProperty("git.commit.id.full");
        String branch = git.getProperty("git.branch");
        assertNotNull(commit, "git.properties must include the complete commit SHA");
        assertTrue(commit.matches("[0-9a-f]{40}") || "UNKNOWN".equals(commit),
                "the commit SHA must be complete or explicitly unknown");
        assertNotNull(branch, "git.properties must include the branch name");
        assertTrue(!branch.trim().isEmpty(), "the branch name must not be blank");
        assertNotNull(git.getProperty("git.commit.time"), "git.properties must include commit time or UNKNOWN");
        assertNotNull(build.getProperty("build.time"), "build-info.properties must include build time");
        assertEquals("1.0.0", build.getProperty("build.version"), "artifact version must be embedded");
    }

    private Properties load(String resource) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "missing build metadata resource: " + resource);
            properties.load(input);
        }
        return properties;
    }
}
