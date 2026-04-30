# Asthipathra ER Diagram (Schema Form)

```mermaid
erDiagram
    ROLES ||--o{ USERS : has
    USERS ||--o{ TWO_FACTOR_AUTH : verifies
    USERS ||--o{ USER_DEVICES : registers
    USERS ||--o{ ASSETS : owns
    USERS ||--o{ NOMINEES : defines
    USERS ||--o{ LOGIN_HISTORY : logs
    USERS ||--o{ USER_SESSIONS : opens
    USERS ||--o{ AUDIT_LOG : performs
    USERS ||--o{ SECURITY_ALERTS : receives
    USERS ||--o{ NOTIFICATIONS : gets
    USERS ||--o{ ASSET_ACCESS_LOG : accesses

    ASSETS ||--o{ ASSET_SHARING : shared_as
    NOMINEES ||--o{ ASSET_SHARING : receives_share
    ASSETS ||--o{ RELEASE_CONDITIONS : release_rule
    ASSETS ||--o{ RELEASE_LOG : release_event
    NOMINEES ||--o{ RELEASE_LOG : released_to
    ASSETS ||--o{ ASSET_ACCESS_LOG : accessed
```

All tables are in 3NF:
- Master entities (`USERS`, `ASSETS`, `NOMINEES`) keep only direct attributes.
- Relationship tables (`ASSET_SHARING`, `RELEASE_LOG`) separate many-to-many or event data.
- Logging/security tables are isolated for auditability and low coupling.
