# chromia-cli-tools Module

## Overview

`chromia-cli-tools` provides CLI-specific utilities and launcher infrastructure for building command-line interfaces that consume the chromia-build-tools APIs. This module bridges the gap between the programmatic APIs and user-facing CLI commands.

## Purpose

This module provides:
- CLI launcher base class with exception handling
- Configuration option builders for Clikt commands
- Terminal output formatting and theming
- Environment adapters between CLI frameworks

## Architecture

The module provides infrastructure for CLI applications:

```
chromia-cli-tools/
└── src/main/kotlin/com/chromia/cli/tools/
    ├── launcher/
    │   └── CliLauncher.kt              # Base CLI launcher class
    ├── config/
    │   └── configOption.kt             # Configuration option builders
    ├── formatter/
    │   ├── PanelHelpFormatter.kt       # Custom help formatter
    │   └── theme.kt                     # Terminal color theme
    ├── env/
    │   └── CliktCliEnv.kt              # Clikt to RellCliEnv adapter
    ├── ft/
    │   ├── evmAuth.kt                  # EVM authentication utilities
    │   └── ftAuth.kt                   # FT4 authentication utilities
    ├── multisignature/
    │   └── multisignature.kt           # Multi-signature utilities
    └── util/
        ├── date.kt                     # Date utilities
        └── Options.kt                  # CLI option builders
```

## Major Components

### Package: `com.chromia.cli.tools.launcher`

**CliLauncher.kt:**
- Base class for CLI applications using Clikt framework
- Handles exception translation and error reporting
- Configures terminal output and help formatting
- Provides `catchingAllExceptionsMain()` for centralized exception handling

**Key Features:**
- Translates exceptions to user-friendly error messages
- Configures terminal theme based on ANSI support
- Sets up custom help formatter (PanelHelpFormatter)
- Provides consistent error exit codes (1, 2, or 3) based on exception type
- Logs errors to files (configurable via `CHR_LOG_FOLDER`)

**Exception Handling:**
- `RellCliExitException` → Exit code 1
- `ClientError` → Exit code 1
- `RellCliException` → Exit code 2
- `ValidationException` → Exit code 2
- `UserMistake` → Exit code 2 or 3 (depending on cause)
- `SQLException` → Exit code 2
- Generic `Exception` → Exit code 3

### Package: `com.chromia.cli.tools.config`

**configOption.kt:**
- Configuration option builders for Clikt commands
- `chromiaConfigOption()`: Loads client configuration (optional, reads from filesystem)
- `chromiaModelOption()`: Loads and validates `chromia.yml` (required)
- `optionalChromiaModelOption()`: Loads model without throwing if missing
- `safeOptionalChromiaModelOption()`: Loads model without throwing on errors
- `chromiaModelConfigOption()`: Loads both model and config

**Purpose:** Provides reusable configuration loading patterns for CLI commands, ensuring consistent configuration handling across commands.

### Package: `com.chromia.cli.tools.formatter`

**PanelHelpFormatter.kt:**
- Custom help formatter for CLI commands
- Provides panel-based help output instead of standard Clikt formatting

**theme.kt:**
- `chromiaTheme`: Terminal color theme configuration
- Defines color scheme for Chromia CLI output

### Package: `com.chromia.cli.tools.env`

**CliktCliEnv.kt:**
- `CliktCliEnv`: Adapter between Clikt commands and `RellCliEnv` interface
- Bridges Clikt's echo functionality to Rell's CLI environment interface
- Tracks error state for command execution

### Package: `com.chromia.cli.tools.ft`

**evmAuth.kt / ftAuth.kt:**
- Authentication utilities for EVM and FT4 (Chromia's authentication system)
- Used for signing transactions and authenticating with Chromia networks

### Package: `com.chromia.cli.tools.multisignature`

**multisignature.kt:**
- Utilities for multi-signature transaction handling
- Supports threshold-based signing

### Package: `com.chromia.cli.tools.util`

**Options.kt:**
- Reusable CLI option builders:
  - `thresholdOption()`: Multi-signature threshold configuration
  - `timebOptions()`: Time-based transaction options
  - `slowDBStatementLogMsOption()`: Database logging threshold
  - `signersOption()`: Signer configuration

**date.kt:**
- Date/time parsing and formatting utilities for CLI

## Dependencies

**Core Dependencies:**
- `com.chromia.cli.tools:chromia-build-tools` - Core APIs
- `com.chromia.cli.tools:chromia-cli-base` - Base utilities
- `net.postchain.client:postchain-client` - Postchain client
- `net.postchain.rell:rell-api-base` - Rell API
- `net.postchain.client:ft4-client` - FT4 authentication
- `com.github.ajalt.clikt:clikt-jvm` - CLI framework
- `com.github.ajalt.mordant:mordant-jvm-jna-jvm` - Terminal output
- `io.github.microutils:kotlin-logging-jvm` - Logging

**Utilities:**
- `org.webjars.npm:ethers` - Ethereum/EVM utilities
- `org.http4k:http4k-server-netty` - HTTP server (for some CLI features)

## Usage Pattern

This module is consumed by CLI implementations (like the `chr` command). A typical CLI implementation would:

1. Extend `CliLauncher` for the main command
2. Use configuration option builders for command options
3. Use `CliktCliEnv` to bridge to Rell APIs
4. Use utility functions for common CLI patterns

**Example Structure:**
```kotlin
class MyCommand : CliLauncher("my-command") {
    // Use config option builders
    private val model by chromiaModelOption()
    
    override fun run() {
        val cliEnv = cliEnv()
        // Use chromia-build-tools APIs
        val configs = ChromiaCompileApi.build(cliEnv, model)
        // ...
    }
}

fun main(args: Array<String>) {
    MyCommand().catchingAllExceptionsMain(args)
}
```

## Integration with chromia-build-tools

This module provides the CLI layer on top of `chromia-build-tools`:
- `CliLauncher` handles user-facing concerns (errors, output)
- Configuration options load models and configs for build-tools APIs
- `CliktCliEnv` adapts Clikt to Rell's CLI environment interface
- Utilities provide common CLI patterns (authentication, signing, etc.)

## Logging

3. **Logging Location:** Log file location is configurable via `CHR_LOG_FOLDER` but defaults to "logs" directory in current working directory.

## Testing

The module includes unit tests in `src/test/kotlin/`. Run tests with:

```bash
cd chromia-cli-tools
mvn test
```

## Binary Compatibility

The build ensures that the binary compatibility of this library is maintained. If you make any changes to the exported API, run:

```bash
mvn compile kotlin-bcv:dump
```
