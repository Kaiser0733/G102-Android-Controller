## What changed and why

-

## Checklist

- [ ] Tests pass (`./gradlew testDebugUnitTest`)
- [ ] No protocol packets changed unexpectedly (if changed intentionally:
      evidence + updated vectors in the PR description)
- [ ] Existing regression vectors preserved (RGB OFF pin intact)
- [ ] No automatic USB activity added (startup/rotation/UI events must
      stay USB-silent; `scripts/check_hotfix.py` stays green)
- [ ] Documentation updated if behavior changed
- [ ] No secrets or personal information
- [ ] Hardware-tested (state device + mouse, or "not tested")
