package com.corhuila.errorcapa8.travesia_natural.common.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRecorder auditRecorder;

    public AuditController(AuditRecorder auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    @GetMapping
    public ResponseEntity<List<AuditRecord>> listAll() {
        return ResponseEntity.ok(auditRecorder.findAll());
    }
}
