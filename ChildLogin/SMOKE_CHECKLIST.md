# Smoke Checklist (Phase 0)

Critical flows to validate per build:
- Login: email/pass validation, sign-in success/failure paths
- Register: validations, sign-up, repeat pass
- Firebase: auth (sign-in/up), realtime DB reads (value events), single reads, writes
- Services: BaseService start/stop, long-lived tasks
- Accessibility: event processing, limits, data posting
- Media: video/audio start/stop where applicable
- Notifications: message fetch/display, intents
- Permissions: runtime prompts & edge cases

Run order:
1. App launch
2. Login flow
3. Register flow
4. Notifications retrieval
5. Accessibility data sync
6. Service start/stop
7. Firebase value events & single query
8. Permissions prompts

Logging/metrics: Keep previous tags/keys unchanged.
