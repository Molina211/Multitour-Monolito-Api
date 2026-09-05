package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.CollaboratorNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CollaboratorQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CollaboratorQueryService implements CollaboratorQueryUseCase {

    private final MembershipRepositoryPort membershipRepositoryPort;

    public CollaboratorQueryService(MembershipRepositoryPort membershipRepositoryPort) {
        this.membershipRepositoryPort = membershipRepositoryPort;
    }

    @Override
    public List<Membership> listByTenant(String tenantId) {
        return membershipRepositoryPort.findAllByTenantIdAndRole(tenantId, MembershipRole.OPERATIONAL_COLLABORATOR);
    }

    @Override
    public Membership getById(String tenantId, UUID membershipId) {
        Membership membership = membershipRepositoryPort.findByTenantIdAndMembershipId(tenantId, membershipId)
                .orElseThrow(() -> new CollaboratorNotFoundException(tenantId, membershipId.toString()));

        if (membership.role() != MembershipRole.OPERATIONAL_COLLABORATOR) {
            throw new CollaboratorNotFoundException(tenantId, membershipId.toString());
        }

        return membership;
    }
}
