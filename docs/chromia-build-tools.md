# chromia-build-tools Module

## Overview

`chromia-build-tools` is the core module providing APIs for compiling Rell code, deploying blockchains, managing libraries, and generating client stubs. This is the primary module that external projects consume.

## Purpose

This module provides:
- Blockchain compilation APIs (Rell → GTV configurations)
- Deployment APIs (create, update, manage blockchains)
- Library management APIs (install, verify libraries)
- Configuration management (load, validate `chromia.yml`)
- Code generation (TypeScript, JavaScript, Python client stubs)
- Project template generation

## Architecture

The module follows a layered API design:

```
chromia-build-tools/
├── src/main/kotlin/com/chromia/
│   ├── api/                    # Public APIs
│   │   ├── api.kt              # Main API objects
│   │   └── impl/               # Implementation details
│   │       ├── compile.kt      # Compilation logic
│   │       ├── deployment.kt    # Deployment logic
│   │       └── lib/            # Library management
│   └── build/tools/
│       ├── blockchain/         # Blockchain utilities
│       ├── compile/             # Compilation utilities
│       ├── config/              # Configuration management
│       ├── lib/                 # Library management
│       ├── model/               # Configuration models
│       ├── template/            # Project templates
│       └── ...                  # Various utilities
└── src/main/resources/
    ├── chromia-model-schema.json  # JSON schema
    └── com/chromia/build/tools/template/  # Templates
```

## Major Components

### Public APIs (`com.chromia.api`)

#### ChromiaCompileApi

**Purpose:** Compiles Rell source code to blockchain configurations

**Methods:**
- `build(cliEnv: RellCliEnv, model: ChromiaModel): List<BlockchainConfiguration>`
  - Compiles all blockchains defined in model
  - Returns list of compiled configurations in GTV format
  - Validates libraries before compilation

- `verify(cliEnv: RellCliEnv, model: ChromiaModel): Boolean`
  - Validates Rell code without full compilation
  - Returns true if code is valid
  - Useful for quick validation checks

#### ChromiaDeploymentApi

**Purpose:** Manages blockchain deployments on Chromia networks

**Methods:**
- `create(printer, model, chromiaConfig, configurations, compressConfigurations): List<BlockchainDeploymentResult>`
  - Creates new blockchain deployments
  - Compresses configurations if needed
  - Returns deployment results with blockchain RIDs

- `update(printer, model, chromiaConfig, configurations, compressConfigurations, height?): List<BlockchainDeploymentResult>`
  - Updates existing blockchain deployments
  - Optional height parameter for rollback scenarios
  - Returns updated deployment results

- `action(model, chromiaConfig, action: BlockchainAction, reason: String): Pair<Boolean, String?>`
  - Performs blockchain actions (start, stop, etc.)
  - Returns success status and optional message

#### ChromiaLibrariesApi

**Purpose:** Manages Rell library installation and verification

**Methods:**
- `install(cliEnv, model, repositoryCloner, forceInstall, progress, isExplicitInstall)`
  - Installs libraries from Git repositories
  - Clones repositories to `src/lib/{library-name}`
  - Verifies library structure
  - **Note:** Marked as `@ExperimentalApi` - API may change

### Implementation Details (`com.chromia.api.impl`)

#### compile.kt

**Core Functions:**
- `compileGtv(cliEnv, model)`: Orchestrates compilation of all blockchains
- `blockchainGtv(cliEnv, tika, compileModel, name, blockchainModel)`: Compiles single blockchain
- `libraryGtv(cliEnv, compileModel, name, library)`: Compiles library
- `verify(cliEnv, model)`: Code verification logic

**Key Features:**
- Maximum configuration size: 26 MiB (`BlockchainConfigurationMaxSize`)
- Supports web static assets inclusion
- Computes blockchain RIDs
- Validates library compatibility

#### deployment.kt

**Core Functions:**
- `createNew()`: Creates new deployments via directory chain
- `updateExisting()`: Updates existing deployments
- `action()`: Executes blockchain actions

**Deployment Flow:**
1. Load client configuration
2. Optionally compress configurations
3. Create deployment proposal via directory chain
4. Monitor deployment status
5. Return results

#### lib/ (Library Management)

**Key Classes:**
- `RepositoryCloner`: Interface for cloning Git repositories
- `GitRepositoryCloner`: JGit-based implementation
- `LibraryVerifyer`: Validates library structure and compatibility
- `DirectoryHashCalculator`: Computes RIDs for libraries

### Configuration Management (`com.chromia.build.tools.config`)

**ChromiaConfigLoader:**
- Loads and parses `chromia.yml` files
- Validates against JSON schema
- Returns `ChromiaModel` object

**ChromiaModel:**
- Root configuration model
- Contains blockchains, deployments, libraries, compile settings
- Validated against `chromia-model-schema.json`

**ChromiaClientConfig:**
- Client configuration for network connections
- Network endpoints, authentication settings

**BlockchainConfigurationCompressor:**
- Compresses large configurations before deployment
- Handles configurations approaching size limits

### Model Classes (`com.chromia.build.tools.model`)

**ChromiaModel:** Root model with:
- `blockchains`: Map of blockchain definitions
- `deployments`: Map of deployment configurations
- `libs`: Map of library definitions
- `compile`: Compilation settings
- `database`: Database configuration
- `test`: Test configuration

**BlockchainModel:** Individual blockchain configuration
- `type`: Blockchain or Library
- `module`: Rell module name
- `moduleArgs`: Module arguments
- `webStatic`: Web static assets directory

**CompileModel:** Compilation settings
- `rellVersion`: Rell language version
- `source`: Source directory path
- `target`: Target directory path
- `strictGtvConversion`: GTV conversion mode

**DeploymentModel:** Deployment configuration
- Network settings
- Deployment targets
- Authentication configuration

**RellLibraryModel:** Library definition
- Registry URL
- Version/tag
- Installation options

### Template Generation (`com.chromia.build.tools.template`)

**Templates Provided:**
- Asset management template (Rell + Next.js)
- Plain template
- Minimal template

**Template Structure:**
- Rell source code
- Frontend code (if applicable)
- Configuration files
- README documentation

## Key Features

### Blockchain Compilation

1. **Multi-Blockchain Support:** Compiles multiple blockchains from single model
2. **Library Compilation:** Handles libraries separately from application blockchains
3. **RID Computation:** Calculates blockchain Resource Identifiers
4. **Web Static Assets:** Includes static web assets in configurations
5. **Size Management:** Handles configuration compression for large deployments

### Library Management

1. **Multiple Sources:** 
   - Clones libraries from Git repositories (HTTPS/SSH)
   - Installs libraries from Chromia Library-chain registry
2. **Version Control:** Supports tags and branches (Git) or version numbers (Library-chain)
3. **Verification:** Validates library structure and compatibility
4. **RID Calculation:** Computes library RIDs for dependency tracking
5. **Force Reinstall:** Supports forced reinstallation of libraries

### Configuration Validation

1. **JSON Schema:** Validates `chromia.yml` against JSON schema
2. **Type Safety:** Strongly-typed model classes
3. **Error Reporting:** Detailed validation error messages
4. **Default Values:** Sensible defaults for optional settings

### Code Generation

1. **Multi-Language:** Generates TypeScript, JavaScript, Python stubs
2. **Type Safety:** Type-safe client code from Rell definitions
3. **Mermaid Diagrams:** Generates diagram visualizations (inferred from dependencies)

## Dependencies

**Core Chromia/Postchain:**
- `net.postchain.rell:rell` - Rell runtime and compiler
- `net.postchain:postchain` - Postchain infrastructure
- `net.postchain.client:postchain-client` - Postchain client
- `net.postchain.rell:codegen-*` - Code generation tools

**Utilities:**
- `org.eclipse.jgit:org.eclipse.jgit` - Git operations
- `com.fasterxml.jackson:jackson-*` - JSON processing
- `org.apache.tika:tika-core` - File type detection
- `org.http4k:http4k-*` - HTTP client

## Usage Examples

### Compiling Blockchains

```kotlin
val cliEnv = RellCliEnv()
val model = ChromiaConfigLoader.load(Path("chromia.yml"))
val configurations = ChromiaCompileApi.build(cliEnv, model)
// configurations contains compiled blockchain configs
```

### Installing Libraries

```kotlin
val cloner = GitRepositoryCloner()
ChromiaLibrariesApi.install(
    cliEnv = cliEnv,
    model = model,
    repositoryCloner = cloner,
    forceInstall = false
)
```

### Deploying Blockchains

```kotlin
val results = ChromiaDeploymentApi.create(
    printer = { isError, msg -> println(msg) },
    model = deploymentModel,
    chromiaConfig = clientConfig,
    configurations = configurations,
    compressConfigurations = true
)
```

## **Chromia Model Schema**

- Overview: The Chromia Model schema defines the configuration structure for Chromia model files (chromia.yml and chromia.yaml). It enables IDEs to validate and provide IntelliSense for Chromia model configuration.
- Schema Store entry: The schema is exposed via Schema Store `https://github.com/SchemaStore/schemastore/blob/master/src/api/json/catalog.json` with an entry named “Chromia Model,” matching files `chromia.yml` and `chromia.yaml`, and pointing to the schema URL:
  - URL: `https://gitlab.com/chromaway/core-tools/chromia-cli-tools/-/raw/dev/chromia-build-tools/src/main/resources/chromia-model-schema.json`
- Availability to IDEs: If you change the Chromia Model schema, it must be merged into the dev branch first. From there, it will be automatically available to IDEs (JetBrains and VS Code) via Schema Store; no manual action is required by IDE users.
- Location in repo: The schema file is at `chromia-build-tools/src/main/resources/chromia-model-schema.json` in the dev branch.

## Known Technical Debt

1. **TODO** remove `postchain-base` dependency (line 445)
2. **Experimental API:** `ChromiaLibrariesApi.install()` marked as `@ExperimentalApi` with note about removing `RepositoryCloner` parameter

## Known Limitations

1. **Single Rell Version:** A project can only use one Rell language version across all blockchains
2. **Library Name Constraints:** Library names must match folder names in `src/lib` exactly
3. **Configuration Size:** No automatic splitting of configurations exceeding 26 MiB limit

## Binary Compatibility

The build ensures binary compatibility. After API changes, run:

```bash
mvn compile kotlin-bcv:dump
```

This updates the binary compatibility baseline in `baseline.xml`.
