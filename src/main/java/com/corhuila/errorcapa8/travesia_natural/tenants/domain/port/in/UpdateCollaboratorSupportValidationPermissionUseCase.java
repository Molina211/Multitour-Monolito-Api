package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;

public interface UpdateCollaboratorSupportValidationPermissionUseCase {

    Tenant updateCollaboratorSupportValidationPermission(UpdateCollaboratorSupportValidationPermissionCommand command);
}
