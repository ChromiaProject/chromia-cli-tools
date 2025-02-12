# chromia-cli-tools

## Configuring Detekt Linter in Intellij IDEA

### Setup Instructions
- Install Detekt plugin from Jetbrains marketplace.
- Go to Preferences > Tools > detekt
  - Check "Enable background analysis" checkbox
  - Check "Enable formatting" checkbox
- Set linter configuration file 
  - In the Detekt settings panel, set the `Configuration File(s) path` to: [detekt.yml](detekt.yml)

All the detekt violations will be prompted in IDE as editing code.

Optionally, you can use `AutoCorrect by Detekt rules` action to fix auto-correctable violations.
