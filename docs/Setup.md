# Setup & Development

## Prerequisites

**Required Tools:**
- **Java Development Kit (JDK):** Version 21 or higher
- **Maven:** Version 3.x (exact minimum version not specified)

**Optional but Recommended:**
- **IntelliJ IDEA:** With Detekt plugin for code quality checks (see IDE Setup section below)

## IDE Setup

### Configuring Detekt Linter in IntelliJ IDEA

**Setup Instructions:**
- Install Detekt plugin from Jetbrains marketplace.
- Go to Preferences > Tools > detekt
  - Check "Enable background analysis" checkbox
  - Check "Enable formatting" checkbox
- Set linter configuration file 
  - In the Detekt settings panel, set the `Configuration File(s) path` to: `detekt.yml` (in the repository root)

All the detekt violations will be prompted in IDE as editing code.

Optionally, you can use `AutoCorrect by Detekt rules` action to fix auto-correctable violations.

## Environment Variables

**Runtime Variables (for CLI usage):**
- `CHR_LOG_FOLDER`: Directory for log files (defaults to "logs" if not set)

**CI/CD Variables:**
- `RUN_DEPENDENCY_CHECK`: Set to "true" to enable OWASP dependency scanning
- `MAVEN_CLI_OPTS`: Maven command-line options (set by CI/CD)

## Setup Steps

5. **Build the project:**
   ```bash
   mvn clean install
   ```
   
   **Note:** This will automatically check out `directory-chain` and `library-chain` repositories to `target/` directory during the build process

## Running Locally

**As a Library:**
This project is a library and cannot be "run" directly. It must be consumed by another project (e.g., `chr` or `pmc`).

**Running Tests:**
```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn clean verify -Pcoverage
```

**Building Specific Modules:**
```bash
# Build only chromia-build-tools
cd chromia-build-tools
mvn clean install

# Build only chromia-cli-tools
cd chromia-cli-tools
mvn clean install
```

**Code Quality Checks:**
```bash
# Run Detekt (static analysis)
mvn verify  # Detekt runs during verify phase

# Check binary compatibility
mvn compile kotlin-bcv:dump  # After making API changes
```

## Common Pitfalls

5. **Binary Compatibility Failures:**
   - **Problem:** `kotlin-bcv:check` fails after API changes
   - **Solution:** Run `mvn compile kotlin-bcv:dump` to update baseline after intentional API changes
