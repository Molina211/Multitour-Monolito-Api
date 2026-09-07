package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.CollaboratorNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;

    private CollaboratorQueryService collaboratorQueryService;

    @BeforeEach
    void setUp() {
        collaboratorQueryService = new CollaboratorQueryService(membershipRepositoryPort);
    }

    private Membership aCollaborator(UUID membershipId, MembershipRole role) {
        return Membership.reconstitute(membershipId, TENANT_ID, "Juan", null, "juan@correo.com", null,
                "hashed-password", role, MembershipStatus.ACTIVA, Instant.now());
    }

    @Test
    void listsCollaboratorsOfATenant() {
        Membership collaborator = aCollaborator(UUID.randomUUID(), MembershipRole.OPERATIONAL_COLLABORATOR);
        when(membershipRepositoryPort.findAllByTenantIdAndRole(TENANT_ID, MembershipRole.OPERATIONAL_COLLABORATOR))
                .thenReturn(List.of(collaborator));

        List<Membership> result = collaboratorQueryService.listByTenant(TENANT_ID);

        assertThat(result).containsExactly(collaborator);
    }

    @Test
    void getsACollaboratorById() {
        UUID membershipId = UUID.randomUUID();
        Membership collaborator = aCollaborator(membershipId, MembershipRole.OPERATIONAL_COLLABORATOR);
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, membershipId))
                .thenReturn(Optional.of(collaborator));

        Membership result = collaboratorQueryService.getById(TENANT_ID, membershipId);

        assertThat(result).isEqualTo(collaborator);
    }

    @Test
    void rejectsWhenMembershipDoesNotExist() {
        UUID membershipId = UUID.randomUUID();
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, membershipId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> collaboratorQueryService.getById(TENANT_ID, membershipId))
                .isInstanceOf(CollaboratorNotFoundException.class);
    }

    @Test
    void rejectsWhenMembershipIsNotACollaborator() {
        UUID membershipId = UUID.randomUUID();
        Membership administrator = aCollaborator(membershipId, MembershipRole.ADMINISTRATOR);
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, membershipId))
                .thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> collaboratorQueryService.getById(TENANT_ID, membershipId))
                .isInstanceOf(CollaboratorNotFoundException.class);
    }
}
