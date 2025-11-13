/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.njord.publisher.nx3.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for invoker.properties files used by maven-invoker-plugin.
 * <p>
 * Supports parsing:
 * - invoker.goals.N - Maven goals to execute
 * - invoker.environmentVariables.* - Environment variables
 * - Property interpolation (${property})
 * - Log file extraction from -l flag
 * </p>
 */
public class InvokerPropertiesParser {

    private static final Logger log = LoggerFactory.getLogger(InvokerPropertiesParser.class);

    private final Properties properties;
    private final Map<String, String> interpolationValues;

    /**
     * Represents a single goal invocation.
     */
    public static class GoalInvocation {
        private final List<String> goals;
        private final File logFile;

        public GoalInvocation(List<String> goals, File logFile) {
            this.goals = goals;
            this.logFile = logFile;
        }

        public List<String> getGoals() {
            return goals;
        }

        public File getLogFile() {
            return logFile;
        }
    }

    /**
     * Creates a parser for the given invoker.properties file.
     *
     * @param propertiesFile    the invoker.properties file
     * @param interpolationValues values for property interpolation (e.g., project.version)
     * @throws IOException if the file cannot be read
     */
    public InvokerPropertiesParser(File propertiesFile, Map<String, String> interpolationValues) throws IOException {
        this.properties = new Properties();
        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
            properties.load(fis);
        }
        this.interpolationValues = interpolationValues;
    }

    /**
     * Gets all goal invocations defined in the properties file.
     * Goals are sorted by their numeric suffix (invoker.goals.1, invoker.goals.2, etc.)
     *
     * @param projectDir the project directory (used for resolving relative log file paths)
     * @return list of goal invocations
     */
    public List<GoalInvocation> getGoalInvocations(File projectDir) {
        List<GoalInvocation> invocations = new ArrayList<>();

        // Find all invoker.goals.N properties
        SortedMap<Integer, String> goalsByNumber = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("invoker.goals.")) {
                String numberPart = key.substring("invoker.goals.".length());
                try {
                    int number = Integer.parseInt(numberPart);
                    String goalsString = properties.getProperty(key);
                    goalsByNumber.put(number, goalsString);
                } catch (NumberFormatException e) {
                    log.warn("Invalid goal number in property: {}", key);
                }
            }
        }

        // Parse each goal string
        for (String goalsString : goalsByNumber.values()) {
            GoalInvocation invocation = parseGoalString(goalsString, projectDir);
            invocations.add(invocation);
        }

        return invocations;
    }

    /**
     * Gets environment variables defined in the properties file.
     *
     * @return map of environment variable names to values
     */
    public Map<String, String> getEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("invoker.environmentVariables.")) {
                String varName = key.substring("invoker.environmentVariables.".length());
                String varValue = properties.getProperty(key);
                envVars.put(varName, interpolate(varValue));
            }
        }

        return envVars;
    }

    /**
     * Parses a goal string into goals and log file.
     * Handles -l flag extraction and property interpolation.
     *
     * @param goalsString the goal string (e.g., "-V -e clean deploy -l build.log")
     * @param projectDir  the project directory
     * @return parsed goal invocation
     */
    private GoalInvocation parseGoalString(String goalsString, File projectDir) {
        // Interpolate properties
        goalsString = interpolate(goalsString);

        // Split into tokens
        List<String> tokens = tokenize(goalsString);

        // Extract log file from -l flag
        File logFile = null;
        List<String> goals = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if ("-l".equals(token) && i + 1 < tokens.size()) {
                // Next token is the log file
                logFile = new File(projectDir, tokens.get(i + 1));
                i++; // Skip the log file name token
            } else {
                goals.add(token);
            }
        }

        return new GoalInvocation(goals, logFile);
    }

    /**
     * Tokenizes a goal string, respecting quoted strings.
     */
    private List<String> tokenize(String str) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    /**
     * Interpolates ${property} placeholders in a string.
     */
    private String interpolate(String value) {
        if (value == null) {
            return null;
        }

        String result = value;
        for (Map.Entry<String, String> entry : interpolationValues.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }

        return result;
    }
}
