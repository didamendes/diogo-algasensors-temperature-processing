package br.com.diogomendes.algasensors.temperature.processing.api.controller;

import br.com.diogomendes.algasensors.temperature.processing.api.model.TemperatureLogOutput;
import br.com.diogomendes.algasensors.temperature.processing.common.IdGenerator;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static br.com.diogomendes.algasensors.temperature.processing.infraestructure.rabbitmq.RabbitMQConfig.FANOUT_EXCHANGE_NAME;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
@RequestMapping("/api/sensors/{sensorId}/temperatures/data")
@Slf4j
@RequiredArgsConstructor
public class TemperatureProcessingController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping(consumes = TEXT_PLAIN_VALUE)
    public void data(@PathVariable TSID sensorId, @RequestBody String input) {
        if (input == null || input.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        Double temperature;

        try {
            temperature = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST);
        }

        TemperatureLogOutput temperatureLogOutput = TemperatureLogOutput.builder()
                .id(IdGenerator.generateTimeBasedUUID())
                .sensorId(sensorId)
                .value(temperature)
                .registeredAt(OffsetDateTime.now())
                .build();

        MessagePostProcessor messagePostProcessor = message -> {
            message.getMessageProperties().setHeader("sensorId", temperatureLogOutput.getSensorId().toString());
            return message;
        };

        rabbitTemplate.convertAndSend(FANOUT_EXCHANGE_NAME, "", temperatureLogOutput, messagePostProcessor);
    }

}
