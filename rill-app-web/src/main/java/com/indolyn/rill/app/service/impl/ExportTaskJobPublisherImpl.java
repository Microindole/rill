package com.indolyn.rill.app.service.impl;

import com.indolyn.rill.app.messaging.ExportTaskMessage;
import com.indolyn.rill.app.service.ExportTaskJobPublisher;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExportTaskJobPublisherImpl implements ExportTaskJobPublisher {

    private final String transport;
    private final String topic;
    private final RocketMQTemplate rocketMQTemplate;
    private final ExportTaskExecutionProcessor exportTaskExecutionProcessor;

    public ExportTaskJobPublisherImpl(
        @Value("${app.messaging.export-task.transport:local}") String transport,
        @Value("${app.messaging.export-task.topic:rill-export-task}") String topic,
        ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
        ExportTaskExecutionProcessor exportTaskExecutionProcessor) {
        this.transport = transport == null ? "local" : transport.trim().toLowerCase();
        this.topic = topic;
        this.rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        this.exportTaskExecutionProcessor = exportTaskExecutionProcessor;
    }

    @Override
    public void publish(long taskId) {
        if ("rocketmq".equals(transport)) {
            if (rocketMQTemplate == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "RocketMQ publisher is not available");
            }
            rocketMQTemplate.convertAndSend(topic, new ExportTaskMessage(taskId));
            return;
        }
        exportTaskExecutionProcessor.process(taskId);
    }
}
