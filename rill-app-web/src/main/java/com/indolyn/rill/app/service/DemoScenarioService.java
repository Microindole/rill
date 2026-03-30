package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.DemoScenarioRequest;
import com.indolyn.rill.app.dto.DemoScenarioResponse;
import com.indolyn.rill.app.dto.DemoScenarioRunResponse;

import java.util.List;

public interface DemoScenarioService {

    List<DemoScenarioResponse> listScenarios();

    DemoScenarioResponse getScenario(long id);

    DemoScenarioResponse createScenario(DemoScenarioRequest request);

    DemoScenarioResponse updateScenario(long id, DemoScenarioRequest request);

    void deleteScenario(long id);

    DemoScenarioRunResponse runScenario(String sessionId, long scenarioId);
}
