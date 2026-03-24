package com.indolyn.rill.app.web;

import com.indolyn.rill.app.service.RillQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  private final RillQueryService rillQueryService;

  public HealthController(RillQueryService rillQueryService) {
    this.rillQueryService = rillQueryService;
  }

  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", "ok");
    payload.put("app", "rill");
    payload.put("loadedDatabases", rillQueryService.getLoadedDatabases());
    return payload;
  }
}
