# Testing Patterns

**Analysis Date:** 2026-05-18

## Test Framework

**Runner:**
- Backend: JUnit 5 through Spring Boot test dependencies in `backend/api/pom.xml`.
- Backend Spring tests: `@SpringBootTest`, `@WebMvcTest`, `@AutoConfigureMockMvc`, and Spring Security test helpers in `backend/api/src/test/java/com/yoloFarm/api/`.
- Frontend: Vitest 4.1.5 configured inside `frontend/vite.config.ts`.
- Frontend DOM tests: jsdom environment with React Testing Library and jest-dom setup in `frontend/src/setupTests.ts`.
- Simulator: no automated test runner detected; `simulator/digital-twin/tools/e2e_create_device_and_verify.py` is a manual E2E verification script.

**Assertion Library:**
- Backend: JUnit Jupiter assertions (`assertEquals`, `assertThrows`, `assertTrue`, `assertFalse`) and Spring MockMvc result matchers (`status`, `jsonPath`).
- Backend mocking/verification: Mockito (`when`, `verify`, `never`, `timeout`, `@Mock`, `@InjectMocks`, `@MockitoBean`).
- Frontend: Vitest `expect` plus `@testing-library/jest-dom` matchers.

**Run Commands:**
```bash
cd backend/api && ./mvnw test              # Run all backend tests on Unix-like shells
cd backend/api && .\mvnw.cmd test          # Run all backend tests on PowerShell
cd backend/api && .\mvnw.cmd test -Dtest=RuleEngineObserverTest  # Run one backend test class
cd frontend && npx vitest                  # Run frontend tests; package.json has no test script
cd frontend && npx vitest --watch          # Frontend watch mode
cd frontend && npx vitest --coverage       # Coverage command shape; coverage provider is not configured
```

## Test File Organization

**Location:**
- Backend tests live under `backend/api/src/test/java/com/yoloFarm/api/`, with some package-specific tests under `backend/api/src/test/java/com/yoloFarm/api/service/automation/` and `backend/api/src/test/java/com/yoloFarm/api/service/mqtt/observer/`.
- Backend test configuration lives in `backend/api/src/test/resources/application.yml`.
- Frontend tests are colocated under `__tests__` folders beside source areas: `frontend/src/stores/__tests__/authStore.test.ts`, `frontend/src/pages/__tests__/FarmDetailPage.test.tsx`.
- Frontend shared setup lives in `frontend/src/setupTests.ts`.
- Manual simulator verification lives in `simulator/digital-twin/tools/e2e_create_device_and_verify.py`.

**Naming:**
- Backend test classes end with `Test`: `FarmCrudIntegrationTest`, `ApiEndpointContractTest`, `MqttReceiverServiceTest`.
- Frontend test files end with `.test.ts` or `.test.tsx`.
- Backend test methods should use behavior names (`should...when...`) for unit tests and contract names (`...ShouldReturn...`) for endpoint tests.

**Structure:**
```text
backend/api/src/test/
├── java/com/yoloFarm/api/
│   ├── *Test.java
│   └── service/*/*Test.java
└── resources/application.yml

frontend/src/
├── pages/__tests__/*.test.tsx
├── stores/__tests__/*.test.ts
└── setupTests.ts
```

## Test Structure

**Suite Organization:**
```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest';

vi.mock('@/lib/axios', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
  },
}));

describe('notificationStore', () => {
  const initialStoreState = useNotificationStore.getState();

  beforeEach(() => {
    useNotificationStore.setState(initialStoreState, true);
    vi.clearAllMocks();
  });

  it('should fetch unread count', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: { unread_count: 5 } });
    await useNotificationStore.getState().fetchUnreadCount();
    expect(api.get).toHaveBeenCalledWith('/notifications/unread-count');
  });
});
```

**Patterns:**
- Backend pure unit tests use `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, and explicit fixture builders. See `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`.
- Backend Spring MVC contract tests use `@WebMvcTest`, `@AutoConfigureMockMvc`, `MockMvc`, `@MockitoBean`, `.with(authentication(...))`, and `.with(csrf())`. See `backend/api/src/test/java/com/yoloFarm/api/ApiEndpointContractTest.java`.
- Backend integration tests use `@SpringBootTest`, H2 config from `backend/api/src/test/resources/application.yml`, and `@Transactional` rollback. See `backend/api/src/test/java/com/yoloFarm/api/FarmCrudIntegrationTest.java`.
- Frontend store tests reset Zustand state with `useStore.setState(initialStoreState, true)` before each test. See `frontend/src/stores/__tests__/authStore.test.ts`.
- Frontend component tests render with the required router/providers inline. `frontend/src/pages/__tests__/FarmDetailPage.test.tsx` wraps `FarmDetailPage` in `MemoryRouter`, `Routes`, and `Route`.

## Mocking

**Framework:** Mockito for Java, Vitest `vi` for frontend.

**Patterns:**
```java
@ExtendWith(MockitoExtension.class)
class RuleServiceConditionPairingTest {
    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private RuleService ruleService;

    @Test
    void shouldThrowWhenActivatingConditionRule_withoutOppositeRule() {
        when(ruleRepository.findByIdAndFarmOwnerId(existing.getId(), ownerId))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> ruleService.toggleRule(existing.getId(), true, ownerId));
        verify(ruleRepository, never()).save(any(Rule.class));
    }
}
```

```typescript
vi.mock('@/lib/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
  getApiErrorMessage: vi.fn((err) => err?.message || 'Error'),
}));

vi.mocked(api.get).mockImplementation((url: string) => {
  if (url.includes('/devices')) return Promise.resolve({ data: mockDevices });
  return Promise.resolve({ data: mockFarm });
});
```

**What to Mock:**
- Mock external transports and infrastructure: MQTT client `IMqttClient` in `backend/api/src/test/java/com/yoloFarm/api/FarmCrudIntegrationTest.java`, axios in frontend tests, STOMP/WebSocket helpers in `frontend/src/pages/__tests__/FarmDetailPage.test.tsx`, Recharts components under jsdom.
- Mock repositories in service unit tests to keep domain branching deterministic.
- Mock Spring beans with `@MockitoBean` in slice/context tests when startup would otherwise create external clients.

**What NOT to Mock:**
- Do not mock the service under test in unit tests; construct it with mocked collaborators using `@InjectMocks` or direct constructors.
- Do not mock Jackson serialization for contract tests; `backend/api/src/test/java/com/yoloFarm/api/ApiContractSerializationTest.java` verifies real configured JSON behavior.
- Do not mock Zustand stores when testing store behavior; call `useAuthStore.getState()` and reset store state directly.

## Fixtures and Factories

**Test Data:**
```java
private Farm buildFarm() {
    User owner = User.builder()
            .id(ownerId)
            .username("owner")
            .password("pwd")
            .email("owner@yolo.test")
            .role(RoleEnum.FARMER)
            .build();

    Farm farm = new Farm();
    farm.setId(farmId);
    farm.setOwner(owner);
    return farm;
}
```

```typescript
const mockDevices = [
  { id: 'dev-1', name: 'Sensor A', model_id: 'model-1', status: 'ACTIVE', device_type: 'SENSOR' },
  { id: 'dev-2', name: 'Pump A', model_id: 'model-2', status: 'ACTIVE', device_type: 'ACTUATOR', operating_mode: 'MANUAL', connection_status: 'ONLINE' },
];
```

**Location:**
- Backend fixtures are private helper methods inside each test class, not shared factories. Examples: `buildFarm`, `buildSensorDevice`, and `buildConditionRuleWithShape` in `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`.
- Frontend fixtures are file-local constants such as `mockFarm`, `mockModels`, and `mockDevices` in `frontend/src/pages/__tests__/FarmDetailPage.test.tsx`.
- No shared fixture directory is detected.

## Coverage

**Requirements:** None enforced. No JaCoCo Maven plugin is configured in `backend/api/pom.xml`, and no Vitest coverage provider/config is configured in `frontend/vite.config.ts`.

**View Coverage:**
```bash
cd frontend && npx vitest --coverage       # Requires adding/configuring a Vitest coverage provider if missing
cd backend/api && .\mvnw.cmd test          # Runs tests only; no coverage report configured
```

## Test Types

**Unit Tests:**
- Backend service/domain unit tests use Mockito and isolated collaborators: `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`, `backend/api/src/test/java/com/yoloFarm/api/DeviceServiceRenameSyncTest.java`, `backend/api/src/test/java/com/yoloFarm/api/MqttReceiverServiceTest.java`.
- Frontend store unit tests exercise Zustand state transitions and API calls: `frontend/src/stores/__tests__/authStore.test.ts`, `frontend/src/stores/__tests__/notificationStore.test.ts`.

**Integration Tests:**
- Backend persistence/service integration uses `@SpringBootTest`, H2, and transaction rollback: `backend/api/src/test/java/com/yoloFarm/api/FarmCrudIntegrationTest.java`, `backend/api/src/test/java/com/yoloFarm/api/EntityMappingIntegrationTest.java`.
- Backend endpoint contracts use Spring MVC slices and MockMvc: `backend/api/src/test/java/com/yoloFarm/api/ApiEndpointContractTest.java`.
- Frontend integration-style component tests render routed pages and mock network/chart/WebSocket boundaries: `frontend/src/pages/__tests__/FarmDetailPage.test.tsx`.

**E2E Tests:**
- No Playwright/Cypress browser E2E suite is detected.
- Simulator/backend E2E verification is a manual script in `simulator/digital-twin/tools/e2e_create_device_and_verify.py`; it registers users, promotes an admin in the database, creates farm/model/device records through HTTP, waits for telemetry, and verifies the telemetry API.

## Common Patterns

**Async Testing:**
```typescript
await waitFor(() => {
  expect(screen.getAllByText('Farm Test')[0]).toBeInTheDocument();
});

fireEvent.click(screen.getByRole('button', { name: /Bat/i }));

await waitFor(() => {
  expect(api.post).toHaveBeenCalledWith('/devices/dev-2/command', { command: 'ON' });
});
```

```java
MqttMessage mqttMsg = new MqttMessage("37.5".getBytes());
mqttReceiverService.messageArrived("testuser/feeds/temp-feed", mqttMsg);

verify(mockObserver, timeout(2000).times(1)).update(any(SensorData.class));
```

**Error Testing:**
```java
assertThrows(IllegalStateException.class,
        () -> ruleService.toggleRule(existing.getId(), true, ownerId));
verify(ruleRepository, never()).save(any(Rule.class));
```

```typescript
vi.mocked(api.get).mockRejectedValueOnce(new Error('Unauthorized'));

await useAuthStore.getState().fetchProfile();

expect(useAuthStore.getState().accessToken).toBeNull();
expect(useAuthStore.getState().isAuthenticated).toBe(false);
```

---

*Testing analysis: 2026-05-18*
