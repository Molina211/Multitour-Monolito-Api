package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto;

public record RegisterExecutionRequest(boolean served, Integer executed, String causal, String actorId) {
}
