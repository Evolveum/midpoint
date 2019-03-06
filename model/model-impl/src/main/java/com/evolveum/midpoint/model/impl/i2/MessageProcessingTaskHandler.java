/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.model.impl.i2;

import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 *
 */
@Component
public class MessageProcessingTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(MessageProcessingTaskHandler.class);

	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/message-processing/handler-3";

	@Autowired private TaskManager taskManager;

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
		System.out.println("URI = " + HANDLER_URI);
	}

	@Override
	public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partitionDefinition) {

		ConnectionFactory connectionFactory = new ConnectionFactory();

//		connectionFactory.setU
//		connectionFactory.setHost("192.168.56.101");
//		factory.setUsername("guest");
//		factory.setPassword("guest");
//		factory.setVirtualHost("/");
//		String queueName = "sampleQueue";
//
//		try (Connection connection = factory.newConnection();
//				Channel channel = connection.createChannel()) {
//
//			DeliverCallback deliverCallback = (consumerTag, message) -> {
//				byte[] body = message.getBody();
//
//				System.out.println("Body: " + new String(body));
//				channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
//				task.incrementProgressAndStoreStatsIfNeeded();
//			};
//			channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});
//
//			while (task.canRun()) {
//				Thread.sleep(1000L);
//			}
//		} catch (Throwable t) {
//			LoggingUtils.logUnexpectedException(LOGGER, "Exception on RabbitMQ", t);
//		}

		TaskRunResult rv = new TaskRunResult();
		rv.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
		rv.setOperationResult(task.getResult());
		return rv;
	}

	@Override
	public String getCategoryName(Task task) {
		return TaskCategory.LIVE_SYNCHRONIZATION;
	}
}
