# Introduction

## Project Summary

chromia-cli-tools is a Maven multi-module library project written in Kotlin that provides APIs and utilities for building, compiling, deploying, and managing Chromia blockchain applications. The project serves as the foundational library for Chromia CLI tools, enabling developers to:

- Compile Rell (Chromia's blockchain programming language) source code into blockchain configurations
- Deploy blockchain configurations to Chromia networks
- Manage blockchain libraries and dependencies
- Validate blockchain configurations and models

The project is structured as a library rather than a standalone CLI application. It provides programmatic APIs that are consumed by CLI implementations: `chr` (chromia-cli) and `pmc` (management-console).

## Value Creation and Users

**Primary Users:**
- Internal Chromia developers building CLI tools
- Developers creating Chromia blockchain applications who use the CLI

**Value Provided:**
1. **Abstraction Layer:** Provides a unified API for blockchain compilation, deployment, and management operations
2. **Configuration Management:** Handles complex blockchain configuration models and validation
3. **Library Management:** Automates installation and management of Rell libraries from Git repositories or Chromia Library-chain registry
4. **Deployment Automation:** Enables programmatic deployment and updates of blockchain configurations to Chromia networks

## Ecosystem Context

This project sits in the middle of the Chromia development toolchain:
- **Input:** Rell source code, `chromia.yml` configuration files, library dependencies
- **Processing:** Compiles Rell to blockchain configurations, validates models, manages libraries
- **Output:** Blockchain configurations (GTV format), client stubs, deployment results

The project integrates with the Chromia blockchain ecosystem through:
- Postchain Client for blockchain execution
- Directory chain for blockchain registry and discovery
- Library chain for shared library management
- Git repositories for library distribution  

### Upstream Dependencies (External Projects)

1. [**Postchain**](https://gitlab.com/chromaway/core/postchain)
   - Core blockchain infrastructure
   - Provides blockchain runtime and base APIs

2. [**Rell**](https://gitlab.com/chromaway/rell)
   - Rell programming language runtime
   - Required for compiling Rell source code

4. [**Postchain Client**](https://gitlab.com/chromaway/core/postchain-client)
   - Client libraries for interacting with Postchain/Chromia networks
   - Used for deployment operations

5. [**Directory Chain**](https://gitlab.com/chromaway/core/directory-chain)
   - Provides directory chain functionality and generated Kotlin client code

6. [**Library Chain**](https://bitbucket.org/chromawallet/library-chain)
   - Provides library chain functionality

### Downstream Projects

1. [**Chromia CLI**](https://gitlab.com/chromaway/core-tools/chromia-cli)
   - Consumes this library to provide command-line interface
   - Location not documented in this repository

2. [**Management Console**](https://gitlab.com/chromaway/core-tools/management-console) 
   - CLI for interacting with the directory chain (management chain) on a Chromia network
   - Consumes this library to provide command-line interface

## External References and Resources

**Documentation:**
- Chromia CLI Documentation: https://docs.chromia.com/build/cli/introduction 
- Rell language documentation: https://docs.chromia.com/rell/rell-intro 

**Key Technologies:**
- **Rell:** Chromia's blockchain programming language
- **Postchain:** Underlying blockchain infrastructure
- **GTV :** Binary format for blockchain configurations

## Module Overview

This project consists of three main modules:

1. **[chromia-build-tools](./chromia-build-tools.md)** - Core compilation, deployment, and library management APIs
2. **[chromia-cli-tools](./chromia-cli-tools.md)** - CLI-specific utilities and launcher infrastructure
3. **[chromia-cli-base](./chromia-cli-base.md)** - Base utilities and shared code

See the individual module documentation for detailed information about each component.
