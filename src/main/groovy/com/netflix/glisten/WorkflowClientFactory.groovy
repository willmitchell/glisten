/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.glisten

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.flow.DataConverter
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClient
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactory
import com.amazonaws.services.simpleworkflow.flow.ManualActivityCompletionClientFactoryImpl
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternal
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientExternalBase
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientFactoryExternalBase
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClientExternal
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import groovy.transform.Canonical

@Canonical class WorkflowClientFactory {

    final AmazonSimpleWorkflow simpleWorkflow

    /** The AWS SWF domain that will be used in this service for polling and scheduling workflows */
    final String domain

    /** The AWS SWF domain that will be used in this service for polling and scheduling workflows */
    final String taskList

    /**
     * Creates a workflow client that allows you to schedule a workflow by calling methods on the workflow interface.
     *
     * @param workflow the interface for this workflow
     * @param workflowDescriptionTemplate provides a description for your workflow
     * @param tags optional custom workflow tags
     * @return the workflow client
     */
    public <T> InterfaceBasedWorkflowClient<T> getNewWorkflowClient(Class<T> workflow,
            WorkflowDescriptionTemplate workflowDescriptionTemplate, WorkflowTags tags = null) {
        WorkflowType workflowType = new WorkflowMetaAttributes(workflow).workflowType
        def factory = new WorkflowClientFactoryExternalBase<InterfaceBasedWorkflowClient>(simpleWorkflow, domain) {
            @Override
            protected InterfaceBasedWorkflowClient createClientInstance(WorkflowExecution workflowExecution,
                    StartWorkflowOptions options, DataConverter dataConverter,
                    GenericWorkflowClientExternal genericClient) {
                new InterfaceBasedWorkflowClient(workflow, workflowDescriptionTemplate, workflowExecution, workflowType,
                        options, dataConverter, genericClient, tags)
            }
        }
        StartWorkflowOptions startWorkflowOptions = new StartWorkflowOptions(taskList: taskList)
        if (tags) {
            startWorkflowOptions.tagList = tags.constructTags()
        }
        factory.startWorkflowOptions = startWorkflowOptions
        factory.client
    }

    /**
     * Gets an existing workflow. Useful for canceling, terminating or just looking at attributes of the workflow.
     *
     * @param workflowIdentification ids for a specific existing workflow execution
     * @return the workflow client
     */
    public WorkflowClientExternal getWorkflowClient(WorkflowExecution workflowIdentification) {
        def factory = new WorkflowClientFactoryExternalBase<WorkflowClientExternal>(simpleWorkflow, domain) {
            @Override
            protected WorkflowClientExternal createClientInstance(WorkflowExecution workflowExecution,
                    StartWorkflowOptions options, DataConverter dataConverter,
                    GenericWorkflowClientExternal genericClient) {
                new WorkflowClientExternalBase(workflowExecution, null, options, dataConverter, genericClient) {}
            }
        }
        factory.getClient(workflowIdentification)
    }

    /**
     * Returns a client for the manual activity. Manual activities do not complete automatically. With a
     * ManualActivityCompletionClient you can revisit an executing manual activity and complete it as you see fit.
     *
     * @param taskToken a token generated by the manual activity
     * @return the workflow client
     */
    ManualActivityCompletionClient getManualActivityCompletionClient(String taskToken) {
        ManualActivityCompletionClientFactory manualCompletionClientFactory =
            new ManualActivityCompletionClientFactoryImpl(simpleWorkflow)
        manualCompletionClientFactory.getClient(taskToken)
    }
}