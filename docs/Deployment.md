# Deployment & Hosting

## Environments

**Development/CI Environment:**
- **Purpose:** Automated testing and validation
- **Trigger:** Every push to non-tag branches
- **Location:** GitLab CI/CD runners
- **Artifacts:** Test reports, code coverage reports, log files

**Release Environment:**
- **Purpose:** Publishing Maven artifacts
- **Trigger:** Pushing a semantic version tag (e.g., `1.2.3`)
- **Location:** GitLab Maven Package Registry
- **Artifacts:** 
  - Main JAR files (compiled classes)
  - Sources JAR files (source code)
  - POM files (project object model)

**No Production Runtime Environment:**
This is a library project, not a deployed service. It is distributed as Maven artifacts consumed by other projects.

## Deployment Flow

**Automated Deployment (via GitLab CI/CD):**

**Release Creation:**
   - Use `release-patch` and `release-minor` tag pushes for releases
   - Do not push tags manually, let CI handle tagging
   - After merging a PR into `main`, trigger a release by manually triggering the pipeline with either:
     - `release-patch` variable (for patch version bump)
     - `release-minor` variable (for minor version bump)
   - `gitlab-release` job creates GitLab release

## CI/CD Pipelines

**Pipeline Definition:** `.gitlab-ci.yml`

**Pipeline Stages:**
1. **build:** Compiles code, runs tests, generates coverage reports
2. **code-coverage:** Aggregates and visualizes test coverage
3. **deploy:** Publishes Maven artifacts (triggered by tags)
4. **dependency-check:** Scans for vulnerable dependencies (optional, triggered by `RUN_DEPENDENCY_CHECK=true`)
5. **release:** Creates GitLab release (triggered by tags)

**Key Jobs:**

**build:**
- Runs on all branches except tags
- Activates `ci` and `coverage` Maven profiles
- Executes: `mvn clean verify --activate-profiles ci,coverage`
- Generates:
  - Test reports (JUnit XML)
  - Code coverage reports (JaCoCo)
  - Code quality reports (Detekt/Code Climate JSON)
- Artifacts: Log files, test reports, coverage reports (expire in 1 week)

**deploy:**
- Runs only on semantic version tags
- Activates `ci` profile
- Sets Maven revision to tag value
- Executes: `mvn clean deploy --activate-profiles ci -Drevision=$CI_COMMIT_TAG`
- Publishes to: GitLab Maven Package Registry

**gitlab-release:**
- Runs after successful deploy
- Creates GitLab release with tag name

**Pipeline Configuration Location:**
- Primary: `.gitlab-ci.yml` in repository root
- Templates: Includes templates from `chromaway/core-infra/gitlab-automation` project (ref 1.3.3)
  - `templates/release.yml`
  - `templates/report-code-coverage.yml`
  - `templates/maven-dependency-scanning.yml`

**Docker Image:**
- Base image: `registry.gitlab.com/chromaway/core-tools/chromia-images/maven-docker-java21:1.0.11`
- Includes: Maven, Java 21, SSH client

## Known Deployment Issues

**Binary Compatibility Checks:**
   - **Risk:** API changes may break binary compatibility without proper baseline updates
   - **Impact:** Downstream projects may fail to compile
   - **Mitigation:** Run `kotlin-bcv:dump` after intentional API changes and commit updated baseline
