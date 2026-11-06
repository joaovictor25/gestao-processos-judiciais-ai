package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping
    public DashboardService.Resumo consultar(@RequestParam(defaultValue = "1") int pagina) {
        return dashboard.consultar(pagina);
    }
}
