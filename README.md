# cloud-itonami-isco-5322

Open Occupation Blueprint for **ISCO-08 5322**: Home-based Personal Care Workers.

This repository designs a forkable OSS business for an independent home-based personal care worker: a mobility/lifting-assist robot supports daily-living tasks for elderly or disabled clients under a governor-gated actor.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a mobility and lifting-assist robot supports daily-living tasks during home visits under an actor that proposes
actions and an independent **Home Care Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
lifting/transfer assist, bathing assist, or medication reminders) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client consent + care plan + daily-living support protocol
        |
        v
Care Advisor -> Home Care Governor -> assist, hold for family/clinician review, or escalate
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `5322`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger
- :telemetry

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
