package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * ============================================================================================
 * POR QUE EXISTE ESTE SEEDER (login de "Acceso de plataforma", punto 1b del bloque de
 * integracion Frontend-Backend, 2026-09-05)
 * ============================================================================================
 *
 * El rol PLATFORM_ADMINISTRATOR estaba declarado en MembershipRole pero no tenia ningun camino
 * de creacion: sin eso, la pantalla "Acceso de plataforma" del Frontend no tiene contra que
 * autenticarse. Spec 007 senalo a proposito que construir autorizacion por rol sin una HU real
 * seria "adivinar un requisito"; esta vez si hay una instruccion explicita del responsable
 * humano (2026-09-05) de resolverlo para poder cerrar el MVP, asi que se documenta la decision
 * en vez de tratarla como un hallazgo pendiente.
 *
 * Un Platform Administrator no pertenece a un tenant especifico (administra a todos), pero el
 * esquema actual exige tenant_id en toda fila de memberships (FK a tenants, ver
 * V2__create_tenants.sql, invariante INV-TEN-001). En vez de cambiar ese esquema —lo que
 * afectaria a todo el modulo tenants por un caso de uso minoritario—, se opto por la salida mas
 * simple (regla "preferir lo simple" de CLAUDE.md): un tenant reservado ("platform", nunca
 * ofrecido a un operador real) que aloja unicamente esta membership. El login sigue siendo
 * exactamente el mismo endpoint ya existente (POST /api/tenants/platform/login) porque
 * LoginService no filtra por rol (ver LoginService.java) — cero endpoints nuevos.
 *
 * Este seeder corre una vez al arrancar la aplicacion y es idempotente (no crea nada si el
 * tenant reservado ya existe). Las credenciales son un placeholder de desarrollo, pensadas para
 * la sustentacion del proyecto, no para produccion real: cambiar via variables de entorno antes
 * de cualquier despliegue publico.
 * ============================================================================================
 */
@Component
public class PlatformAdministratorSeeder implements ApplicationRunner {

    public static final String RESERVED_TENANT_ID = "platform";
    private static final String SEED_EMAIL = "admin@multitour.plataforma";
    private static final String SEED_PASSWORD = "Multitour#2026";

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdministratorSeeder(TenantRepositoryPort tenantRepositoryPort,
                                        MembershipRepositoryPort membershipRepositoryPort,
                                        PasswordEncoder passwordEncoder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tenantRepositoryPort.existsById(RESERVED_TENANT_ID)) {
            return;
        }

        Tenant reservedTenant = Tenant.create(RESERVED_TENANT_ID, "Multitour (plataforma)");
        tenantRepositoryPort.save(reservedTenant);

        String passwordHash = passwordEncoder.encode(SEED_PASSWORD);
        Membership platformAdministrator = Membership.createPlatformAdministrator(
                RESERVED_TENANT_ID, SEED_EMAIL, passwordHash);
        membershipRepositoryPort.save(platformAdministrator);
    }
}
