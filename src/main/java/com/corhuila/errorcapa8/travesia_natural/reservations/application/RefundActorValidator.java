package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.RefundActionNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;

import java.util.UUID;

/**
 * RN-RES-008: solo un actor con rol ADMINISTRATOR puede autorizar, rechazar o registrar
 * como saldo a favor una solicitud de devolución. Compartido por los tres application
 * services de decisión de devolución (spec 019).
 */
final class RefundActorValidator {

    private RefundActorValidator() {
    }

    static void requireAdministratorActor(MembershipRepositoryPort membershipRepositoryPort, String tenantId,
                                           String actorId) {
        UUID actorMembershipId;
        try {
            actorMembershipId = UUID.fromString(actorId);
        } catch (IllegalArgumentException e) {
            throw new RefundActionNotAllowedException(
                    "actorId must be a valid membershipId for a refund decision: " + actorId);
        }
        Membership actor = membershipRepositoryPort.findByTenantIdAndMembershipId(tenantId, actorMembershipId)
                .orElseThrow(() -> new RefundActionNotAllowedException(
                        "membership not found for actorId: " + actorId + " in tenant " + tenantId));
        if (actor.role() != MembershipRole.ADMINISTRATOR) {
            throw new RefundActionNotAllowedException(
                    "only an ADMINISTRATOR can decide on a refund request, actor role: " + actor.role());
        }
    }
}
