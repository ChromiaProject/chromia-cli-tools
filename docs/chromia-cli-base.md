# chromia-cli-base Module

## Overview

`chromia-cli-base` is the minimal base module providing shared utilities for CLI output formatting. It contains foundational code used by other modules in the chromia-cli-tools project.

## Purpose

This module provides:
- JSON table formatting utilities
- Terminal output utilities (Mordant-based)

## Architecture

The module is structured as a thin utility layer with minimal dependencies:

```
chromia-cli-base/
└── src/main/kotlin/com/chromia/cli/base/
    ├── formatter/
    │   └── json.kt                    # JSON table formatting
    └── mordant/
        └── mordant.kt                 # Terminal output utilities
```

## Major Components

### Package: `com.chromia.cli.base.formatter`

**json.kt:**
- `jsonTable()`: Converts table structures to JSON format
- `json()`: Pretty-prints JSON data using Gson
- `fixKey()`: Normalizes keys for JSON output (replaces spaces with underscores, removes colons)

**Purpose:** Provides utilities for formatting CLI output as JSON tables, enabling structured data output for programmatic consumption.

### Package: `com.chromia.cli.base.mordant`

**mordant.kt:**
- Internal utilities copied from the Mordant library (terminal output library)
- `TableBuilderInstance`: Internal table builder implementation
- `CellStyleBuilderMixin`: Cell styling utilities
- `ColumnBuilderInstance`: Column configuration utilities

## Dependencies

**Core Dependencies:**
- `net.postchain:postchain-common` - Postchain common utilities
- `net.postchain.client:postchain-client` - Postchain client library
- `com.github.ajalt.clikt:clikt-jvm` - CLI framework
- `com.github.ajalt.mordant:mordant-jvm-jna-jvm` - Terminal output formatting
- `io.github.microutils:kotlin-logging-jvm` - Logging utilities
- `com.google.code.gson:gson` - JSON processing

## Usage

This module is primarily consumed by `chromia-cli-tools` module for CLI output formatting. It provides low-level utilities that other modules build upon.

**Example Usage (from chromia-cli-tools):**
```kotlin
import com.chromia.cli.base.formatter.json
import com.chromia.cli.base.formatter.jsonTable

// Format data as JSON
val jsonOutput = json(data)

// Format table as JSON
val tableJson = jsonTable {
    header { /* ... */ }
    body { /* ... */ }
}
```

## Binary Compatibility

The build ensures that the binary compatibility of this library is maintained. If you make any changes to the exported API, run:

```bash
mvn compile kotlin-bcv:dump
```

This updates the binary compatibility baseline.

## Known Issues

2. **Minimal Functionality:** This module is intentionally minimal. Most functionality is in `chromia-cli-tools` and `chromia-build-tools` modules.
