/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.gradle.transportversion;

import org.elasticsearch.gradle.LoggedExec;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@CacheableTask
public class TransportVersionTask extends DefaultTask {
    private static final String NAME = "generate-transport-version";

    @TaskAction
    public void run() throws IOException {
            System.out.println("TransportVersionTask run");
            try(var output = new PrintWriter(new FileWriter("server/src/main/java/org/elasticsearch/TransportVersion.java", true))){
                output.println("heee");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }
}
