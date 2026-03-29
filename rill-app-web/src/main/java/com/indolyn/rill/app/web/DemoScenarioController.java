package com.indolyn.rill.app.web;

import com.indolyn.rill.app.dto.DemoScenarioRequest;
import com.indolyn.rill.app.dto.DemoScenarioResponse;
import com.indolyn.rill.app.dto.DemoScenarioRunResponse;
import com.indolyn.rill.app.service.DemoScenarioService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace/scenarios")
public class DemoScenarioController {

    private final DemoScenarioService demoScenarioService;

    public DemoScenarioController(DemoScenarioService demoScenarioService) {
        this.demoScenarioService = demoScenarioService;
    }

    @GetMapping
    public List<DemoScenarioResponse> listScenarios() {
        return demoScenarioService.listScenarios();
    }

    @GetMapping("/{id}")
    public DemoScenarioResponse getScenario(@PathVariable long id) {
        return demoScenarioService.getScenario(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemoScenarioResponse createScenario(@RequestBody DemoScenarioRequest request) {
        return demoScenarioService.createScenario(request);
    }

    @PutMapping("/{id}")
    public DemoScenarioResponse updateScenario(@PathVariable long id, @RequestBody DemoScenarioRequest request) {
        return demoScenarioService.updateScenario(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScenario(@PathVariable long id) {
        demoScenarioService.deleteScenario(id);
    }

    @PostMapping("/{id}/run/{sessionId}")
    public DemoScenarioRunResponse runScenario(@PathVariable long id, @PathVariable String sessionId) {
        return demoScenarioService.runScenario(sessionId, id);
    }
}
