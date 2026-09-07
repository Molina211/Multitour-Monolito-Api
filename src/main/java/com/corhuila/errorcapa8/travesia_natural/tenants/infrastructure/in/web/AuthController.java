package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidCredentialsException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginResult;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.LoginRequest;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * ============================================================================================
 * LEE ESTO ANTES DE CONECTAR EL FRONTEND A ESTE ENDPOINT (spec 004, HU-IAM-002 / FR-017)
 * ============================================================================================
 *
 * Este comentario es deliberadamente largo. Va dirigido a quien integre este endpoint desde
 * el repo Frontend (persona o IA) y no haya leído `specs/004-end-customer-login/spec.md` ni
 * `plan.md` de este repo — para que no tenga que rastrear nada más que este archivo para
 * entender por qué el endpoint está diseñado así y qué le falta al Frontend para poder usarlo.
 *
 * --------------------------------------------------------------------------------------------
 * 1. POR QUÉ EL `tenantId` VA EN LA URL Y NO SE PUEDE QUITAR
 * --------------------------------------------------------------------------------------------
 *
 * Este proyecto es multitenant y usa una única base de datos compartida entre todos los
 * tenants, con `tenant_id` como columna discriminadora obligatoria en cada tabla propia de un
 * tenant (`memberships` incluida). Esta estrategia está fijada formalmente en
 * `ADR-003-tenant-isolation-strategy.md` (repo Docs, `05-architecture/decisions/records/`),
 * y la invariante que la sostiene es `INV-TEN-001` (tenant isolation): ninguna consulta puede
 * cruzar datos entre tenants, y para eso siempre hace falta saber CONTRA QUÉ tenant se está
 * operando antes de tocar la base de datos.
 *
 * El motivo concreto por el que el login no puede resolver el tenant "solo" es que el `email`
 * de una `Membership` NO es único a nivel global — solo es único DENTRO de un mismo tenant.
 * Esta decisión se tomó explícitamente en spec 003 (registro de End Customer, HU-IAM-001
 * escenario 3) precisamente para permitir que dos tenants completamente independientes tengan
 * cada uno un cliente con el mismo correo, como dos cuentas sin ninguna relación entre sí (por
 * ejemplo, "laura.gomez@example.com" registrada tanto en el tenant "travesia-natural" como en
 * un tenant "otro-operador", sin que una sepa de la existencia de la otra). El índice único en
 * base de datos es `(tenant_id, email)`, NUNCA `email` solo — ver migración
 * `V3__add_end_customer_fields.sql`.
 *
 * Consecuencia directa: si este endpoint recibiera solo `email` + `password` sin ningún dato
 * de tenant, y existieran dos memberships con ese mismo email en tenants distintos, el Backend
 * NO TENDRÍA FORMA DE SABER contra cuál de las dos validar la contraseña. No es una limitación
 * de esta implementación que se pueda arreglar con más lógica: es una consecuencia directa e
 * ineludible de que el email no es una clave global en este modelo de datos. La única forma de
 * resolverlo es que quien llama al endpoint indique explícitamente el tenant.
 *
 * Se evaluaron 3 formas de indicar el tenant (todas discutidas y decididas junto al responsable
 * humano del proyecto, no una elección unilateral de la IA que escribió este código):
 *   a) Un campo de tenant en el body del login, junto a email/password.
 *   b) Un header custom (ej. `X-Tenant-Id`), como ya usan otros endpoints internos de este
 *      Backend que no pasan por una pantalla de usuario (ver `POST /api/reservations`, spec 001).
 *   c) El `tenantId` como parte de la URL: `POST /api/tenants/{tenantId}/login`.
 *
 * Se eligió (c) porque ya es exactamente el mismo patrón usado en
 * `POST /api/tenants/{tenantId}/customers` (spec 003, registro de este mismo tipo de cuenta),
 * y porque en general todos los endpoints de negocio de este módulo (`tenants`) que operan
 * "dentro de" un tenant específico usan la URL para eso, no el body ni un header — mantiene la
 * API consistente en vez de tener un mecanismo distinto solo para login.
 *
 * --------------------------------------------------------------------------------------------
 * 2. LO QUE ESTO SIGNIFICA PARA EL FRONTEND: FALTA UN CAMPO DE TENANT EN LA PANTALLA
 * --------------------------------------------------------------------------------------------
 *
 * Al momento de escribir esto (spec 004, 2026-09-03), `login.component.ts` / `.html` del repo
 * Frontend NO tienen ningún campo, selector, ni valor de configuración que identifique un
 * tenant — el formulario solo pide `email` y `password`, y el `onSubmit` actual es una
 * simulación que ni siquiera hace una llamada HTTP real. El mismo vacío existe en
 * `signup.component.html` (heredado de spec 003, que tampoco tocó el Frontend).
 *
 * Esto significa que, TAL COMO ESTÁ HOY el Frontend, es literalmente imposible invocar este
 * endpoint desde la pantalla de login real: no hay ningún dato en el formulario que se pueda
 * usar para construir la URL `/api/tenants/{tenantId}/login`. Este endpoint SÍ se puede probar
 * directamente contra el Backend (con `curl`/Postman/lo que sea — ver la sección "004" de
 * `PLAN-VERIFICACION.md` en este repo), pero no end-to-end desde el Frontend actual.
 *
 * Esto fue una decisión consciente, no un olvido: se implementó igual porque es la pieza que
 * hace falta para que el multitenant funcione de verdad en el login (sin esto, cualquier
 * implementación de login sería incompleta o insegura), y porque el equipo prefirió dejarlo
 * ya construido y documentado en vez de esperar a que el Frontend se pusiera al día primero.
 *
 * QUIEN INTEGRE FRONTEND + BACKEND (sea una persona o una IA, incluida la que mantiene el repo
 * Frontend) TIENE QUE AGREGAR AL FRONTEND una forma de obtener el `tenantId` antes de poder
 * llamar a este endpoint de verdad. Algunas opciones posibles (ninguna decidida todavía, es
 * trabajo pendiente y le corresponde a quien haga esa integración, no a este commit):
 *   - Un campo visible en el formulario de login/registro (ej. "código de operador" o
 *     "identificador de tenant"), igual de simple que agregar un input más.
 *   - Resolver el tenant a partir del dominio/subdominio desde el que se sirve el Frontend
 *     (ej. `travesia-natural.multitour.app` → tenantId `travesia-natural`), si en algún momento
 *     el despliegue usa subdominios por tenant (hoy no existe esa infraestructura).
 *   - Un valor de configuración fijo por build/deploy del Frontend, si cada instancia del
 *     Frontend sirve a un único tenant conocido de antemano.
 *
 * Sin uno de estos cambios en el Frontend, el login (y el registro de spec 003) no se pueden
 * conectar de verdad, sin importar cuánto se mejore este Backend. Por favor validar esta
 * decisión y, si se elige una alternativa distinta a las de arriba, está bien — lo único que no
 * se puede evitar es que EL FRONTEND NECESITA SABER DE ALGUNA FORMA A QUÉ TENANT PERTENECE
 * ANTES de llamar a este endpoint.
 *
 * --------------------------------------------------------------------------------------------
 * 3. POR QUÉ EL ERROR ES SIEMPRE 401 GENÉRICO (INCLUSO PARA UN TENANT QUE NO EXISTE)
 * --------------------------------------------------------------------------------------------
 *
 * A diferencia de los demás endpoints de `tenants` (que sí devuelven 404 si el `tenantId` no
 * existe), este login devuelve el MISMO 401 genérico para absolutamente todos los casos de
 * rechazo: password incorrecto, email sin membership en ese tenant, `tenantId` inexistente,
 * tenant `Inactivo`, o membership `INACTIVA`. Esto es intencional (HU-IAM-002 escenarios 2 y
 * 3): si el login devolviera 404 para un tenant inexistente, cualquiera podría "probar" IDs de
 * tenant contra este endpoint y usar la diferencia entre 401 y 404 para enumerar qué tenants
 * existen realmente en la plataforma — una fuga de información que este endpoint, a propósito,
 * no permite. Ver `LoginService` (capa de aplicación): todos los rechazos lanzan la misma
 * `InvalidCredentialsException`, sin distinción posible en el código para el controller.
 * ============================================================================================
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/login")
public class AuthController {

    private final LoginUseCase loginUseCase;

    public AuthController(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> login(@PathVariable String tenantId, @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(tenantId, request.email(), request.password());
        LoginResult result = loginUseCase.login(command);
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("invalid_credentials", ex.getMessage()));
    }
}
