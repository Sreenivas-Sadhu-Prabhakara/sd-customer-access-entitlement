package com.bank.bian.customeraccessentitlement.api;

import com.bank.bian.customeraccessentitlement.model.ControlRecord;
import com.bank.bian.customeraccessentitlement.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Customer Access Entitlement" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/access-entitlement-administrative-plan/initiate                    → Initiate a control record
 *   GET  /v1/access-entitlement-administrative-plan                             → Retrieve (list)
 *   GET  /v1/access-entitlement-administrative-plan/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/access-entitlement-administrative-plan/{crId}/update               → Update
 *   PUT  /v1/access-entitlement-administrative-plan/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Customer Access Entitlement",
                "businessArea", "Sales and Service",
                "businessDomain", "Customer Management",
                "functionalPattern", "Administer",
                "assetType", "Access Entitlement",
                "controlRecord", "Access Entitlement Administrative Plan",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/access-entitlement-administrative-plan/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/access-entitlement-administrative-plan")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/access-entitlement-administrative-plan/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/access-entitlement-administrative-plan/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/access-entitlement-administrative-plan/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
