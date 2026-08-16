# taiwan-stock-lab-android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84?logo=android&logoColor=white)](https://developer.android.com/build/releases/gradle-plugin)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Multi--Module-4CAF50)](#architecture)
[![Async](https://img.shields.io/badge/Async-Coroutines%20%2B%20Flow-1565C0)](#tech-stack)
[![Data](https://img.shields.io/badge/Data-Offline--First%20%2B%20Room%203-009688)](#offline-first-architecture)
[![DI](https://img.shields.io/badge/DI-Hilt-49A84A)](#dependency-injection)
[![Testing](https://img.shields.io/badge/Testing-JUnit5%20%2B%20MockK-FF9800)](#testing)
[![Build](https://img.shields.io/badge/Build-Gradle%20Version%20Catalog-02303A?logo=gradle&logoColor=white)](#tech-stack)

---

## Introduction

A Taiwan stock market Android app built on top of the [TWSE OpenAPI](https://openapi.twse.com.tw/), aggregating valuation, daily price, average-price, and trading data for listed stocks.

The current implementation focuses on a production-oriented data foundation built with Kotlin, Clean Architecture, a multi-module Gradle setup, structured concurrency, an offline-first Room 3 persistence layer, and a Hilt-based dependency injection composition root.

The presentation layer is planned around MVVM with MVI-style Unidirectional Data Flow, using XML as the primary UI with selective Jetpack Compose interoperability.

This repository is intended for learning, technical assessment, and engineering demonstration purposes.

---

## Features

### Implemented

- Concurrent aggregation of three TWSE endpoints:
  - `BWIBBU_ALL`
  - `STOCK_DAY_AVG_ALL`
  - `STOCK_DAY_ALL`
- Structured concurrency with `coroutineScope` and `async` / `await`
- Dataset merge by stock code with O(n+m+k) map-based joins
- `STOCK_DAY_ALL` used as the primary stock universe
- Missing valuation or average-price data does not remove an otherwise valid stock
- Defensive parsing of TWSE string-based numeric fields
- Missing-value normalization for `"-"`, `"--"`, `"---"`, empty strings, and invalid numeric input
- Domain numeric representation using nullable `BigDecimal` and `Long`
- Room 3 offline-first persistence
- Local Room database as the single source of truth
- Reactive local observation through `Flow<List<Stock>>`
- Network refresh writes into Room instead of returning remote data directly to consumers
- Failed refreshes preserve the existing cache
- Empty or invalid remote snapshots preserve the existing cache
- Coroutine cancellation is propagated instead of being converted into `Result.failure`
- Exported Room schema stored in version control
- Application-level dependency injection composition root using Hilt, with core and feature modules kept independent of the DI framework
- JVM unit tests and Android instrumentation tests

### Planned

- ViewModel with MVI-style `UiState`, `UiEvent`, and `UiEffect`
- XML-based stock list screen
- Sorting and filtering
- Stock detail dialog
- Jetpack Compose custom components embedded through `ComposeView`
- Dark mode
- Configuration-change support
- ktlint
- detekt
- GitHub Actions CI
- Crash reporting and observability

---

## Architecture

### Module Graph

```text
                       :app
                         │
                         ▼
                 :feature:stocklist
                  │       │       │
                  ▼       ▼       ▼
           :core:common :core:ui :core:network
```

**Modules**

- **`:app`** — Android application entry point and dependency injection composition root. Owns application-level Hilt modules and navigation. Currently contains the initial Activity shell.
- **`:core:common`** — Pure Kotlin/JVM module. Contains shared coroutine abstractions such as `DispatchersProvider`. Does not depend on Android or feature modules.
- **`:core:network`** — Reusable networking infrastructure (Retrofit, OkHttp, Moshi, `NetworkClientFactory`). Contains no TWSE-specific feature logic.
- **`:core:ui`** — Shared UI module scaffold. Reusable themes and UI components are planned as the presentation layer is implemented.
- **`:feature:stocklist`** — Owns stock-list-specific data and domain logic: TWSE API contracts and DTOs, remote data source, Room persistence, domain model, repository implementation, and the future presentation layer.

Core modules never depend on feature modules. The domain layer has no knowledge of Retrofit, Room, or Android framework classes.

### Remote Data Flow

```text
TWSE APIs
   ├── BWIBBU_ALL
   ├── STOCK_DAY_AVG_ALL
   └── STOCK_DAY_ALL
          │
          ▼
TwseRemoteDataSource
          │
          │ structured concurrency
          ▼
TwseRawSnapshot
          │
          ▼
StockMapper
   │
   ├── merge by stock code
   └── TwseNumericParser
          │
          ▼
List<Stock>
```

`TwseRemoteDataSource` launches the three independent endpoint requests concurrently inside the same structured-concurrency scope. If one request fails, the snapshot fetch fails as a whole rather than producing a partially inconsistent snapshot.

### Offline-First Architecture

```text
                         refresh
                           │
                           ▼
TWSE APIs
   │
   ▼
TwseRemoteDataSource
   │
   ▼
StockMapper
   │
   ▼
OfflineFirstStockRepository
   │
   ▼
StockDao
   │
   ▼
Room 3
   │
   │ Flow<List<StockEntity>>
   ▼
StockEntityMapper
   │
   ▼
Flow<List<Stock>>
   │
   ▼
presentation layer
```

Room is the single source of truth. The repository exposes two distinct operations:

```kotlin
fun observeStocks(): Flow<List<Stock>>

suspend fun refreshStocks(): Result<Unit>
```

`observeStocks()` observes local persisted state. `refreshStocks()` retrieves a new TWSE snapshot and synchronizes it into Room — it does not return remote stock data directly to the caller. This keeps the read path consistent regardless of network availability.

A failed network request or an empty/invalid remote snapshot does not overwrite the existing local cache.

### Dependency Injection

Dependency construction is centralized in `:app` using Hilt.

```text
TaiwanStockApplication
        │
        ▼
SingletonComponent
        │
        ├── TwseNetworkModule     → Retrofit, TwseApiService
        ├── DatabaseModule        → StockDatabase, StockDao
        └── StockRepositoryModule → TwseRemoteDataSource, StockRepository
```

`core:*` and `feature:stocklist` classes contain no Hilt annotations or `@Inject` constructors. `:app` assembles the complete object graph through explicit `@Provides` modules, keeping the domain and data layers independent of the dependency injection framework.

### Feature-Internal Layering

`:feature:stocklist` uses package-level Clean Architecture layering instead of splitting every layer into a separate Gradle module.

```text
feature/stocklist/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   ├── StockDatabase.kt
│   │   └── StockDatabaseFactory.kt
│   │
│   ├── remote/
│   │   ├── api/
│   │   ├── dto/
│   │   ├── model/
│   │   └── TwseRemoteDataSource.kt
│   │
│   ├── mapper/
│   │   ├── StockMapper.kt
│   │   ├── StockEntityMapper.kt
│   │   └── TwseNumericParser.kt
│   │
│   └── repository/
│       ├── OfflineFirstStockRepository.kt
│       └── EmptyStockSnapshotException.kt
│
└── domain/
    ├── model/
    │   └── Stock.kt
    └── repository/
        └── StockRepository.kt
```

---

## Tech Stack

**Language and Build**
- Kotlin 2.2.10
- Android Gradle Plugin 9.2.1
- Gradle 9.4.1
- Gradle Version Catalog
- Type-safe Project Accessors
- KSP

**Networking**
- Retrofit 3.0.0
- OkHttp 4.12.0
- Moshi 1.15.2 (KSP code generation)

**Concurrency**
- Kotlin Coroutines
- `coroutineScope` / `async` / `await`
- Flow

**Persistence**
- Room 3.0.1 (`androidx.room3`)
- Room KSP code generation
- Room Gradle Plugin
- `BundledSQLiteDriver`
- Exported and version-controlled Room schemas

**Dependency Injection**
- Hilt 2.60.1
- KSP code generation
- Application-level composition root
- Core and feature modules remain Hilt-independent

**Testing**
- JUnit 5
- MockK
- kotlinx-coroutines-test
- AndroidJUnit4 / AndroidX Test
- Room in-memory database tests
- Hilt instrumentation testing (`hilt-android-testing`, custom `HiltTestRunner`)

**Planned**
- StateFlow / SharedFlow
- XML UI
- Jetpack Compose interoperability
- Espresso UI tests
- ktlint / detekt
- GitHub Actions
- Firebase Crashlytics
- LeakCanary

---

## Testing

The project separates fast JVM unit tests from Android instrumentation tests:

```text
src/test/          JVM unit tests
src/androidTest/   Android runtime / integration tests
```

**Numeric Parsing** — `TwseNumericParserTest` covers null values, empty values, TWSE missing-value sentinels, invalid numeric strings, thousands separators, decimal parsing, and integer parsing.

**Dataset Aggregation** — `StockMapperTest` covers joining the three TWSE datasets by stock code, preserving stocks when optional valuation data is missing, and rejecting invalid rows without a required code or name.

**Structured Concurrency** — `TwseRemoteDataSourceTest` verifies that three mocked endpoint calls, each delayed by one virtual second, complete in approximately one virtual second rather than three — proving actual concurrent execution rather than merely checking that all three methods were called.

**Offline-First Repository** — `OfflineFirstStockRepositoryTest` covers reading cached stocks, successful remote refresh into Room, preserving cache after network failure, preserving cache after an empty remote snapshot, and propagating `CancellationException`.

**Room DAO** — `StockDaoTest` is an Android instrumentation test using a real in-memory Room database with `BundledSQLiteDriver`, verifying transactional replacement of cached data and deterministic ordering by stock code.

**Dependency Injection** — `StockRepositoryInjectionTest` is an Android instrumentation test that verifies the production Hilt dependency graph resolves and injects `StockRepository`, including its transitive Retrofit, remote data source, Room database, and DAO dependencies.

Notable test names:

```text
refreshStocks_whenNetworkFails_keepsExistingCache
refreshStocks_whenRemoteResultIsEmpty_keepsExistingCache
merge_keepsStockWhenValuationDataIsMissing
fetchSnapshot_fetchesAllEndpointsConcurrently
stockRepository_isInjectedSuccessfully
```

---

## Git Workflow

The repository is developed through small, independently reviewable Pull Requests. Each architectural concern is introduced in its own focused change, such as: project foundation, network infrastructure, TWSE aggregation, Room persistence, dependency injection, presentation state, UI, and quality tooling.

Dependencies are introduced when first needed rather than being added up front.

The repository also contains a [Pull Request template](.github/pull_request_template.md) covering Summary, Changes, Architecture, Verification, Screenshots, Notes, and Related work.

---

## Roadmap

| Stage | Scope | Status |
|---|---|---|
| Project foundation | Multi-module setup, Version Catalog, `core:common` | ✅ Done |
| Network infrastructure | Retrofit, OkHttp, Moshi, `core:network` | ✅ Done |
| TWSE remote layer | API service and DTOs | ✅ Done |
| Domain + aggregation | `Stock`, numeric parsing, concurrent fetch, merge by code | ✅ Done |
| Persistence | Room 3, DAO, schema export | ✅ Done |
| Offline-first repository | Room source of truth, refresh/cache policy | ✅ Done |
| Dependency injection | Hilt composition root | ✅ Done |
| Presentation state | ViewModel + MVI-style UDF | ⏳ Next |
| XML UI | Stock list, sorting/filtering, detail dialog | ⏳ Planned |
| Compose interoperability | `ComposeView` custom components | ⏳ Planned |
| Quality tooling | ktlint, detekt, CI | ⏳ Planned |
| Observability | Crashlytics, LeakCanary | ⏳ Planned |
| Polish | Dark mode, rotation, animations | ⏳ Planned |

---

## Environment

- Android Gradle Plugin: `9.2.1`
- Gradle: `9.4.1`
- Kotlin: `2.2.10`
- `compileSdk`: `37`
- `targetSdk`: `36`
- `minSdk`: `24`
- Java source / target compatibility: `17`
- Gradle daemon toolchain: `21`

---

## Local Development

Clone the repository:

```bash
git clone https://github.com/<your-account>/taiwan-stock-lab-android.git
cd taiwan-stock-lab-android
```

Build the app:

```bash
./gradlew assembleDebug
```

### Useful Commands

Run all JVM unit tests:
```bash
./gradlew test
```

Run stock-list unit tests:
```bash
./gradlew :feature:stocklist:testDebugUnitTest
```

Run Room instrumentation tests (requires a running emulator or physical device):
```bash
./gradlew :feature:stocklist:connectedDebugAndroidTest
```

Run Hilt dependency-graph instrumentation tests (requires a running emulator or physical device):
```bash
./gradlew :app:connectedDebugAndroidTest
```

Run Android Lint:
```bash
./gradlew lint
```

Clean the project:
```bash
./gradlew clean
```

---

## Project Structure

```text
taiwan-stock-lab-android/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/sun/taiwan_stock_lab_android/
│       │   │   ├── TaiwanStockApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   └── di/
│       │   │       ├── TwseNetworkModule.kt
│       │   │       ├── DatabaseModule.kt
│       │   │       └── StockRepositoryModule.kt
│       │   └── AndroidManifest.xml
│       ├── test/
│       └── androidTest/
│           └── java/com/sun/taiwan_stock_lab_android/
│               ├── HiltTestRunner.kt
│               └── StockRepositoryInjectionTest.kt
│
├── core/
│   ├── common/
│   ├── network/
│   └── ui/
│
├── feature/
│   └── stocklist/
│       ├── schemas/
│       │   └── .../1.json
│       │
│       └── src/
│           ├── main/kotlin/.../feature/stocklist/
│           │   ├── data/
│           │   │   ├── local/
│           │   │   ├── remote/
│           │   │   ├── mapper/
│           │   │   └── repository/
│           │   │
│           │   └── domain/
│           │       ├── model/
│           │       └── repository/
│           │
│           ├── test/kotlin/
│           └── androidTest/kotlin/
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── .github/
│   └── pull_request_template.md
│
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Data Source

Stock market data is provided by the public [Taiwan Stock Exchange OpenAPI](https://openapi.twse.com.tw/).

The current implementation consumes:
- `BWIBBU_ALL`
- `STOCK_DAY_AVG_ALL`
- `STOCK_DAY_ALL`

---

## Purpose

This project demonstrates Android engineering practices such as:

- modular architecture
- dependency boundaries
- structured concurrency
- defensive API parsing
- offline-first persistence
- dependency injection composition
- reactive data flow
- automated testing
- incremental delivery through reviewable Pull Requests

---

## License

This repository is currently intended for learning, technical assessment, and demonstration purposes.