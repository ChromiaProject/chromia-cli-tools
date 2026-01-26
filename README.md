# chromia-cli-tools

**Project Name:** chromia-cli-tools
**Repository URL:** https://gitlab.com/chromaway/core-tools/chromia-cli-tools
**chromia-cli-tools** is a Maven multi-module library project that provides APIs and utilities for building, compiling, deploying, and managing Chromia blockchain applications. This library serves as the foundational layer for Chromia CLI tools, enabling developers to interact with the Chromia blockchain ecosystem programmatically.

## Overview

This project is structured as a **library** rather than a standalone CLI application. It provides APIs that are consumed by CLI implementations:
- **`chr`** (chromia-cli) - Main command-line interface for Chromia development
- **`pmc`** (management-console) - CLI for interacting with the directory chain (management chain) on Chromia networks

The library sits in the middle of the Chromia development toolchain, transforming Rell source code into deployable blockchain configurations and managing the entire lifecycle from compilation to deployment.

## Documentation

### General Documentation

- **[Introduction](docs/Introduction.md)** - Project overview, purpose, value creation, upstream/downstream projects, and ecosystem context
- **[Setup](docs/Setup.md)** - Prerequisites, environment setup, local development, IDE configuration, and common pitfalls
- **[Deployment](docs/Deployment.md)** - Deployment flow, CI/CD pipelines, environments, and deployment risks
- **[External Services](docs/External-Services.md)** - CI/CD services, hosting, monitoring, third-party APIs, and Maven dependencies

### Module Documentation

- **[chromia-build-tools](docs/chromia-build-tools.md)** - Core compilation, deployment, and library management APIs
- **[chromia-cli-tools](docs/chromia-cli-tools.md)** - CLI-specific utilities and launcher infrastructure
- **[chromia-cli-base](docs/chromia-cli-base.md)** - Base utilities and shared code

## Key Features

### 🔨 Blockchain Compilation
- Compiles Rell (Chromia's blockchain programming language) source code into blockchain configurations (GTV format)
   - Validates Rell code syntax and semantics
   - Computes blockchain RIDs (Resource Identifiers)
   - Supports multiple blockchain definitions in a single project

### 📦 Library Management
- Installs Rell libraries from Git repositories or Chromia Library-chain registry
   - Verifies library compatibility and versions
- Manages library dependencies automatically
   - Calculates library RIDs for dependency tracking

### 🚀 Deployment Operations
   - Creates new blockchain deployments on Chromia networks
   - Updates existing blockchain deployments
   - Performs blockchain actions (start, stop, etc.) via directory chain proposals
- Handles deployment compression and error recovery

### 🔧 Code Generation
- Generates type-safe client stubs from Rell code for multiple languages:
  - TypeScript
  - JavaScript
  - Python

### ⚙️ Configuration Management
- Loads and validates `chromia.yml` configuration files
- JSON schema validation for configuration models
- Manages database connection settings
- Handles compile-time and runtime configuration options

### 🎨 CLI Infrastructure
- Base launcher class with exception handling
- Configuration option builders for CLI commands
- Terminal output formatting and theming
- Environment adapters between CLI frameworks

## Architecture

The project is organized as a Maven multi-module structure with three main modules:

```
chromia-cli-tools/
├── chromia-cli-base/          # Base utilities and shared code
├── chromia-cli-tools/         # CLI-specific utilities and launcher infrastructure
└── chromia-build-tools/       # Core compilation, deployment, and library management APIs
```

### Module Responsibilities

1. **chromia-build-tools** - Core functionality module
   - Public APIs for compilation, deployment, and library management
   - Configuration loading and validation
   - Rell compilation orchestration
   - Code generation utilities

2. **chromia-cli-tools** - CLI infrastructure module
   - Base CLI launcher with exception handling
   - Configuration option builders
   - Terminal output formatting
   - Environment adapters

3. **chromia-cli-base** - Base utilities module
   - Shared utilities and common code
   - Foundation for other modules

## Technology Stack

**Core Technologies:**
- **Kotlin** - Primary implementation language
- **Java** 21 or later - JVM target and runtime requirement
- **Maven** - Build and dependency management

**Chromia/Postchain Ecosystem:**
- **Rell** - Blockchain programming language runtime
- **Postchain** - Core blockchain infrastructure
- **Postchain Client** - Client libraries for network interactions
- **Rell Codegen** - Code generation tools

**CLI Framework:**
- **Clikt** - Command-line interface framework
- **Mordant** - Terminal output formatting

**Key Utilities:**
- **JGit** - Git operations for library management
- **Jackson** - JSON processing

## Quick Start

### Prerequisites

- **Java Development Kit (JDK):** Version 21 or higher
- **Maven:** Version 3.x

### Building the Project

   ```
   mvn clean install
   ```
   
### Using as a Library

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.chromia.cli.tools</groupId>
    <artifactId>chromia-build-tools</artifactId>
    <version>dev</version>
</dependency>
```

## Ecosystem Context

This project sits in the middle of the Chromia development toolchain:

- **Input:** Rell source code, `chromia.yml` configuration files, library dependencies
- **Processing:** Compiles Rell to blockchain configurations, validates models, manages libraries
- **Output:** Blockchain configurations (GTV format), client stubs, deployment results

The project integrates with the Chromia blockchain ecosystem through:
- **Postchain runtime** - For blockchain execution
- **Directory chain** - For blockchain registry and discovery
- **Library chain** - For shared library management
- **Git repositories** - For library distribution

### Upstream Dependencies

The project depends on several Chromia/Postchain projects:
- **Postchain** - Core blockchain infrastructure
- **Rell** - Rell programming language
- **Rell Codegen** - Code generation tools
- **Postchain Client** - Client libraries for network interactions
- **Directory Chain** - Directory chain functionality
- **Library Chain** - Library chain functionality

### Downstream Consumers

The library is consumed by:
- **Chromia CLI** (`chr`) - Main command-line interface
- **Management Console** (`pmc`) - CLI for directory chain interactions

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn clean verify -Pcoverage
```

### Code Quality

The project uses:
- **Detekt** - Static code analysis for Kotlin
- **Kotlin BCV Plugin** - Binary compatibility verification

See [Setup documentation](docs/Setup.md#ide-setup) for IDE configuration.