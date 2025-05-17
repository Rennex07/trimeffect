# Trim Effect

Add configurable potion effects to armor trim. They are unstackable.
You can customize the effect, amplifer, ambient, and particles.


Used **PaperMC** 1.21.4 API

Trims aren't hardcoded so if you want to disable effect on some trim just remove them from config.
Config example:
```
trims:
  sentry:
    effects:
      - effect: SPEED
        amplifier: 1
        ambient: true
        particles: true
```
