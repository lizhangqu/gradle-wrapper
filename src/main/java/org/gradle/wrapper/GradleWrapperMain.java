/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.wrapper;

import org.gradle.cli.CommandLineParser;
import org.gradle.cli.ParsedCommandLine;
import org.gradle.cli.SystemPropertiesCommandLineConverter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

import static org.gradle.wrapper.Download.UNKNOWN_VERSION;

public class GradleWrapperMain {
    public static final String GRADLE_USER_HOME_OPTION = "g";
    public static final String GRADLE_USER_HOME_DETAILED_OPTION = "gradle-user-home";
    public static final String GRADLE_QUIET_OPTION = "q";
    public static final String GRADLE_QUIET_DETAILED_OPTION = "quiet";

    public static void main(String[] args) throws Exception {
        File wrapperJar = wrapperJar();
        File propertiesFile = wrapperProperties(wrapperJar);
        File localPropertiesFile = wrapperLocalProperties(wrapperJar);
        File rootDir = rootDir(wrapperJar);

        CommandLineParser parser = new CommandLineParser();
        parser.allowUnknownOptions();
        parser.option(GRADLE_USER_HOME_OPTION, GRADLE_USER_HOME_DETAILED_OPTION).hasArgument();
        parser.option(GRADLE_QUIET_OPTION, GRADLE_QUIET_DETAILED_OPTION);

        SystemPropertiesCommandLineConverter converter = new SystemPropertiesCommandLineConverter();
        converter.configure(parser);

        ParsedCommandLine options = parser.parse(args);
        String gradleVersionFromArgs = removeGradleVersion(args, options);

        Properties systemProperties = System.getProperties();
        systemProperties.putAll(converter.convert(options, new HashMap<String, String>()));

        File gradleUserHome = gradleUserHome(options);

        addSystemProperties(gradleUserHome, rootDir);

        Logger logger = logger(options);

        WrapperExecutor wrapperExecutor = WrapperExecutor.forWrapperPropertiesFile(propertiesFile, localPropertiesFile, gradleVersionFromArgs);
        wrapperExecutor.execute(
                args,
                new Install(logger, new Download(logger, "gradlew", UNKNOWN_VERSION), new PathAssembler(gradleUserHome)),
                new BootstrapMainStarter());
    }

    private static String removeGradleVersion(String[] args, ParsedCommandLine options) {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg != null) {
                    arg = arg.trim();
                    if (arg.startsWith("--gradle-version")) {
                        args[i] = "";
                    }
                }
            }
        }

        List<String> extraArguments = options.getExtraArguments();
        if (extraArguments != null && extraArguments.size() > 0) {
            Iterator<String> iterator = extraArguments.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                if (next != null) {
                    next = next.trim();
                    if (next.startsWith("--gradle-version")) {
                        iterator.remove();
                        return next.replace("--gradle-version=", "");
                    }
                }
            }
        }

        return null;
    }


    private static void addSystemProperties(File gradleHome, File rootDir) {
        System.getProperties().putAll(SystemPropertiesHandler.getSystemProperties(new File(gradleHome, "gradle.properties")));
        System.getProperties().putAll(SystemPropertiesHandler.getSystemProperties(new File(rootDir, "gradle.properties")));
    }

    private static File rootDir(File wrapperJar) {
        return wrapperJar.getParentFile().getParentFile().getParentFile();
    }

    private static File wrapperProperties(File wrapperJar) {
        return new File(wrapperJar.getParent(), wrapperJar.getName().replaceFirst("\\.jar$", ".properties"));
    }

    private static File wrapperLocalProperties(File wrapperJar) {
        return new File(wrapperJar.getParent(), wrapperJar.getName().replaceFirst("\\.jar$", "-local.properties"));
    }

    private static File wrapperJar() {
        URI location;
        try {
            location = GradleWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if (!location.getScheme().equals("file")) {
            throw new RuntimeException(String.format("Cannot determine classpath for wrapper Jar from codebase '%s'.", location));
        }
        try {
            return Paths.get(location).toFile();
        } catch (NoClassDefFoundError e) {
            return new File(location.getPath());
        }
    }

    private static File gradleUserHome(ParsedCommandLine options) {
        if (options.hasOption(GRADLE_USER_HOME_OPTION)) {
            return new File(options.option(GRADLE_USER_HOME_OPTION).getValue());
        }
        return GradleUserHomeLookup.gradleUserHome();
    }

    private static Logger logger(ParsedCommandLine options) {
        return new Logger(options.hasOption(GRADLE_QUIET_OPTION));
    }
}
