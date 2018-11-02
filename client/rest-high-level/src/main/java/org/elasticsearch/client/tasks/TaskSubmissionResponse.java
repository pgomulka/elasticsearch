/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.client.tasks;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.tasks.TaskId;

import java.io.IOException;

/**
 * Response object that contains the task id
 */
public class TaskSubmissionResponse extends ActionResponse implements ToXContentObject {

    protected static final ParseField TASK = new ParseField("task");

    protected static final ConstructingObjectParser<TaskId, Void> PARSER = new ConstructingObjectParser<>(
        "task_submission_response", true, a -> (TaskId) a[0]);

    static {
        PARSER.declareField(ConstructingObjectParser.constructorArg(), TaskId.parser(), TASK, ObjectParser.ValueType.STRING);
    }

    public static TaskSubmissionResponse fromXContent(XContentParser parser) throws IOException {
        TaskId taskId = PARSER.parse(parser, null);
        return new TaskSubmissionResponse(taskId);
    }

    private final TaskId task;

    protected TaskSubmissionResponse(TaskId task) {
        this.task = task;
    }

    /**
     * Get the task id of the submitted task
     */
    public TaskId getTask() {
        return task;
    }

    @Override
    public int hashCode() {
        return task.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return task.equals(other);

    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (task != null) {
            builder.field(TASK.getPreferredName(), task.toString());
        }
        builder.endObject();
        return builder;
    }
}
