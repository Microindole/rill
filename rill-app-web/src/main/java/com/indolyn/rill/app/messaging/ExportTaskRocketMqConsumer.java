package com.indolyn.rill.app.messaging;

import com.indolyn.rill.app.service.impl.ExportTaskExecutionProcessor;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.export-task.transport", havingValue = "rocketmq")
@RocketMQMessageListener(
    topic = "${app.messaging.export-task.topic:rill-export-task}",
    consumerGroup = "${rocketmq.consumer.group:rill-app-web-consumer}",
    messageModel = MessageModel.CLUSTERING)
public class ExportTaskRocketMqConsumer implements RocketMQListener<ExportTaskMessage> {

    private final ExportTaskExecutionProcessor exportTaskExecutionProcessor;

    public ExportTaskRocketMqConsumer(ExportTaskExecutionProcessor exportTaskExecutionProcessor) {
        this.exportTaskExecutionProcessor = exportTaskExecutionProcessor;
    }

    @Override
    public void onMessage(ExportTaskMessage message) {
        exportTaskExecutionProcessor.process(message.taskId());
    }
}
