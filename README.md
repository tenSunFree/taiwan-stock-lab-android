# taiwan-stock-lab-android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84?logo=android&logoColor=white)](https://developer.android.com/build/releases/gradle-plugin)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Multi--Module-4CAF50)](#architecture)
[![Async](https://img.shields.io/badge/Async-Coroutines%20%2B%20Flow-1565C0)](#tech-stack)
[![Data](https://img.shields.io/badge/Data-Offline--First%20%2B%20Room%203-009688)](#offline-first-architecture)
[![DI](https://img.shields.io/badge/DI-Hilt-49A84A)](#dependency-injection)
[![UI](https://img.shields.io/badge/UI-XML%20%2B%20ViewBinding-3DDC84?logo=android&logoColor=white)](#ui)
[![Testing](https://img.shields.io/badge/Testing-JUnit5%20%2B%20MockK-FF9800)](#testing)
[![Build](https://img.shields.io/badge/Build-Gradle%20Version%20Catalog-02303A?logo=gradle&logoColor=white)](#tech-stack)

---

## Introduction

A Taiwan stock market Android app built on top of the [TWSE OpenAPI](https://openapi.twse.com.tw/), aggregating valuation, daily price, average-price, and trading data for listed stocks.

The current implementation is built with Kotlin, Clean Architecture, a multi-module Gradle setup, structured concurrency, an offline-first Room 3 persistence layer, a Hilt-based dependency injection composition root, a Hilt-injected presentation state layer using MVI-style Unidirectional Data Flow, and an XML-based stock list screen.

Jetpack Compose interoperability is planned as a follow-up.

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
- Room 3 offline-first persistence, with the local database as the single source of truth
- Local Room access encapsulated behind `StockLocalDataSource`, mirroring `TwseRemoteDataSource` on the remote side
- Reactive local observation through `Flow<List<Stock>>`
- Network refresh writes into Room instead of returning remote data directly to consumers
- Failed refreshes and empty/invalid remote snapshots preserve the existing cache
- Coroutine cancellation is propagated instead of being converted into `Result.failure`
- Last successful refresh timestamp persisted transactionally alongside the stock cache, surfaced in the UI as "最後更新：MM/dd HH:mm"
- UI state distinguishes "local cache not yet loaded" from "loaded but empty", preventing the empty-state text from flashing before the initial Room query completes
- Exported Room schema stored in version control
- Explicit Room schema migration (v1 → v2) preserving existing cached data, with a `MigrationTestHelper` test verifying it
- Application-level dependency injection composition root using Hilt, organized by responsibility (network, database, repository) rather than by layer, with core and data/domain layers kept independent of the DI framework
- Screen-level presentation state managed with a Hilt-injected ViewModel, using `StateFlow` for persistent UI state and `SharedFlow` for one-off UI effects (MVI-style Unidirectional Data Flow)
- XML-based stock list screen with `RecyclerView`, `ListAdapter`/`DiffUtil`, and `SwipeRefreshLayout`
- Presentation-layer price coloring: closing price above/below the monthly average, and positive/negative daily change, each mapped to red/green following Taiwan stock-market convention
- Stock-code sorting (ascending/descending) via a Material bottom sheet, default descending
- Stock valuation details (P/E ratio, dividend yield, P/B ratio) via a Material alert dialog
- Initial-loading and empty-state UI handling
- JVM unit tests and Android instrumentation tests

### Planned

- Jetpack Compose custom components embedded through `ComposeView`
- Dark mode verification pass
- Configuration-change support verification
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

- **`:app`** — Android application entry point and dependency injection composition root. Owns application-level Hilt modules and the screen host (`MainActivity`).
- **`:core:common`** — Pure Kotlin/JVM module. Contains shared coroutine abstractions such as `DispatchersProvider`. Does not depend on Android or feature modules.
- **`:core:network`** — Reusable networking infrastructure (Retrofit, OkHttp, Moshi, `NetworkClientFactory`). Contains no TWSE-specific feature logic.
- **`:core:ui`** — Shared color/theme resources (e.g. `stock_price_up`, `stock_price_down`, `stock_card_background`), with light/dark variants.
- **`:feature:stocklist`** — Owns all stock-list-specific logic: TWSE API contracts and DTOs, remote data source, Room persistence, domain model, repository implementation, presentation state (ViewModel), and screen-specific UI rendering (`RecyclerView` adapter, item layout, UI models).

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
StockLocalDataSource
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

Room is the single source of truth. The repository exposes three operations:

```kotlin
fun observeStocks(): Flow<List<Stock>>

fun observeLastRefreshedAt(): Flow<Long?>

suspend fun refreshStocks(): Result<Unit>
```

A failed network request or an empty/invalid remote snapshot does not overwrite the existing local cache. `observeLastRefreshedAt()` is backed by a single-row `refresh_metadata` table written in the same Room transaction as the stock cache replacement — the cache and its "last updated" timestamp can never drift out of sync.

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
        └── StockRepositoryModule → TwseRemoteDataSource, StockLocalDataSource, StockRepository
```

`core:*` and `feature:stocklist`'s `data`/`domain` classes contain no Hilt annotations or `@Inject` constructors. `:app` assembles the complete object graph through explicit `@Provides` modules, organized by responsibility rather than by layer: `DatabaseModule` only provides Room-specific types (`StockDatabase`, `StockDao`), while `StockRepositoryModule` provides both data sources (`TwseRemoteDataSource`, `StockLocalDataSource`) and the `StockRepository` that coordinates them. The presentation layer (`StockListViewModel`) is the one exception to the "no Hilt in feature classes" rule — see below.

### Presentation State

`StockListViewModel` follows an MVI-style Unidirectional Data Flow contract:

```text
UI
 │
 │ UiEvent
 ▼
StockListViewModel
 │
 ├── StateFlow<StockListUiState>
 │
 └── SharedFlow<StockListUiEffect>
 │
 ▼
StockRepository
```

`StockListViewModel` is intentionally Hilt-aware (`@HiltViewModel`) because it belongs to the Android presentation layer, which is already framework-coupled. The `data` and `domain` layers remain independent of Hilt.

Initial remote refresh is triggered through an explicit `OnStart` event rather than the ViewModel constructor, so UI effect collectors are guaranteed to be active before a possible refresh error is emitted — `MutableSharedFlow` defaults to `replay = 0`, so an effect emitted before any collector subscribes would otherwise be silently dropped.

ViewModel coroutines call repository suspend functions directly on `viewModelScope` without forcing a specific dispatcher — the underlying Retrofit and Room APIs already expose main-safe suspend functions.

`StockListUiState.hasLoadedCache` distinguishes "the local Room query hasn't emitted yet" from "it emitted an empty result" — without this, the UI briefly showed the empty-state text before the initial cache arrived, since a default/empty state was indistinguishable from a confirmed-empty cache.

### UI

The stock list screen is `RecyclerView`-based, hosted by `MainActivity` (`@AndroidEntryPoint`, `by viewModels()`):

```text
MainActivity
 ├── MaterialToolbar (sort action)
 ├── "最後更新：..." timestamp label
 ├── SwipeRefreshLayout
 │      └── RecyclerView (StockListAdapter / ListAdapter + DiffUtil)
 ├── BottomSheetDialog (sort direction)
 └── MaterialAlertDialogBuilder (stock detail)
```

`StockUiModel`, `PricePosition` (`ABOVE_AVERAGE`/`BELOW_AVERAGE`/`EQUAL`/`UNKNOWN`), `ChangeDirection` (`POSITIVE`/`NEGATIVE`/`ZERO`/`UNKNOWN`), and `StockUiModelMapper` all live in `:feature:stocklist/presentation` — these encode stock-specific business rules (closing price vs. monthly average, change sign) and formatting, not generic app-level view logic. `PricePosition` and `ChangeDirection` are kept as separate enums rather than a single ambiguous `PriceTrend.UP/DOWN`, since "closing price above the monthly average" and "the stock rose today" are different facts.

`StockListAdapter` uses `ViewBinding` and `DiffUtil.ItemCallback` (item identity by stock code, content equality by full `StockUiModel`) for efficient list updates.

### Feature-Internal Layering

`:feature:stocklist` uses package-level Clean Architecture layering instead of splitting every layer into a separate Gradle module.

```text
feature/stocklist/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   ├── StockDatabase.kt
│   │   ├── StockDatabaseFactory.kt
│   │   ├── StockDatabaseMigrations.kt
│   │   └── StockLocalDataSource.kt
│   ├── remote/
│   ├── mapper/
│   └── repository/
│
├── domain/
│   ├── model/
│   └── repository/
│
└── presentation/
    ├── StockListViewModel.kt
    ├── adapter/
    │   └── StockListAdapter.kt
    ├── contract/
    │   ├── SortDirection.kt
    │   ├── StockListUiState.kt
    │   ├── StockListUiEvent.kt
    │   └── StockListUiEffect.kt
    ├── model/
    │   ├── StockUiModel.kt
    │   ├── PricePosition.kt
    │   └── ChangeDirection.kt
    └── mapper/
        └── StockUiModelMapper.kt
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
- ViewBinding

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
- Explicit schema migrations (`Migration` + `MigrationTestHelper`)

**Dependency Injection**
- Hilt 2.60.1
- KSP code generation
- Application-level composition root, modules organized by responsibility (network, database, repository)
- Data and domain layers remain Hilt-independent

**Presentation**
- MVVM
- MVI-style Unidirectional Data Flow
- `StateFlow` for persistent UI state
- `SharedFlow` for one-off UI effects
- Hilt-injected screen-level ViewModel (`@HiltViewModel`)

**UI**
- XML layouts + ViewBinding
- `RecyclerView` + `ListAdapter` / `DiffUtil`
- `SwipeRefreshLayout`
- Material Components (`MaterialCardView`, `MaterialToolbar`, `BottomSheetDialog`, `MaterialAlertDialogBuilder`)
- `repeatOnLifecycle` for lifecycle-aware `Flow` collection

**Testing**
- JUnit 5
- MockK
- kotlinx-coroutines-test
- Turbine
- AndroidJUnit4 / AndroidX Test
- Room in-memory database tests
- Room `MigrationTestHelper`
- Hilt instrumentation testing (`hilt-android-testing`, custom `HiltTestRunner`)

**Planned**
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

**Structured Concurrency** — `TwseRemoteDataSourceTest` verifies that three mocked endpoint calls, each delayed by one virtual second, complete in approximately one virtual second rather than three.

**Offline-First Repository** — `OfflineFirstStockRepositoryTest` covers reading cached stocks via `StockLocalDataSource`, reading/writing the last-refreshed timestamp, successful remote refresh, preserving cache after network failure/empty snapshot, and propagating `CancellationException`.

**Room DAO** — `StockDaoTest` is an Android instrumentation test using a real in-memory Room database with `BundledSQLiteDriver`, covering atomic stock+metadata replacement.

**Room Migration** — `StockDatabaseMigrationTest` uses Room's `MigrationTestHelper` to verify `MIGRATION_1_2` preserves existing stock rows and correctly adds the `refresh_metadata` table.

**Dependency Injection** — `StockRepositoryInjectionTest` is an Android instrumentation test verifying the production Hilt dependency graph resolves and injects `StockRepository`.

**Presentation State** — `StockListViewModelTest` covers cached-stock observation, `hasLoadedCache` becoming true even for an empty cache, `OnStart` triggering the initial refresh exactly once, explicit `OnRefresh`, sort-direction changes, refresh failure state/effect, last-refreshed timestamp exposure, and stock-detail effects for known/unknown stock codes — using JUnit 5, MockK, `kotlinx-coroutines-test` (`StandardTestDispatcher`), and Turbine.

**UI Formatting Rules** — `StockUiModelMapperTest` covers price-position classification (above/below monthly average), change-direction classification (positive/negative), null-value placeholders, thousands-separator formatting, and the `+`/`-` sign on the change value.

Notable test names:

```text
refreshStocks_whenNetworkFails_keepsExistingCache
refreshStocks_whenRemoteResultIsEmpty_keepsExistingCache
merge_keepsStockWhenValuationDataIsMissing
fetchSnapshot_fetchesAllEndpointsConcurrently
stockRepository_isInjectedSuccessfully
OnStart_triggersRefreshOnlyOnce
knownStockClick_emitsShowStockDetail
closingAboveMonthlyAverage_isMarkedAboveAverage
positiveChange_includesPlusSign
replaceAll_writesRefreshMetadataAlongsideStocks
migrate1To2_preservesExistingStocksAndAddsMetadataTable
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
| Presentation state | ViewModel + MVI-style UDF | ✅ Done |
| XML UI | Stock list, sorting, detail dialog | ✅ Done |
| Cache metadata | Local data source abstraction, loading-state fix, last-updated timestamp, schema migration | ✅ Done |
| Compose interoperability | `ComposeView` custom components | ⏳ Next |
| Quality tooling | ktlint, detekt, CI | ⏳ Planned |
| Observability | Crashlytics, LeakCanary | ⏳ Planned |
| Polish | Dark mode, rotation verification, animations | ⏳ Planned |
| Scaling | Paging 3 for the stock list, if dataset size grows significantly | ⏳ Future |

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

Build and install the app:

```bash
./gradlew installDebug
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

Run Room instrumentation tests, including the schema migration test (requires a running emulator or physical device):
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
│       │   ├── res/
│       │   │   ├── layout/
│       │   │   │   ├── activity_main.xml
│       │   │   │   └── bottom_sheet_sort.xml
│       │   │   ├── menu/
│       │   │   │   └── menu_stock_list.xml
│       │   │   ├── drawable/
│       │   │   │   ├── ic_sort.xml
│       │   │   │   └── bg_bottom_sheet_handle.xml
│       │   │   └── values/
│       │   │       └── strings.xml
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
│       └── src/main/res/
│           ├── values/colors.xml
│           └── values-night/colors.xml
│
├── feature/
│   └── stocklist/
│       ├── schemas/
│       │   └── .../
│       │       ├── 1.json
│       │       └── 2.json
│       │
│       └── src/
│           ├── main/
│           │   ├── kotlin/.../feature/stocklist/
│           │   │   ├── data/
│           │   │   │   ├── local/
│           │   │   │   │   ├── dao/
│           │   │   │   │   ├── entity/
│           │   │   │   │   ├── StockDatabase.kt
│           │   │   │   │   ├── StockDatabaseFactory.kt
│           │   │   │   │   ├── StockDatabaseMigrations.kt
│           │   │   │   │   └── StockLocalDataSource.kt
│           │   │   │   ├── remote/
│           │   │   │   ├── mapper/
│           │   │   │   └── repository/
│           │   │   ├── domain/
│           │   │   └── presentation/
│           │   │       ├── StockListViewModel.kt
│           │   │       ├── adapter/
│           │   │       ├── contract/
│           │   │       ├── model/
│           │   │       └── mapper/
│           │   └── res/
│           │       ├── layout/item_stock_card.xml
│           │       └── values/strings.xml
│           │
│           ├── test/kotlin/
│           └── androidTest/
│               └── kotlin/.../data/local/
│                   ├── StockDaoTest.kt
│                   └── StockDatabaseMigrationTest.kt
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
- MVI-style unidirectional presentation state
- presentation-layer business-rule mapping (price coloring, formatting)
- reactive data flow
- database schema evolution with migration testing
- automated testing
- incremental delivery through reviewable Pull Requests

---

## License

This repository is currently intended for learning, technical assessment, and demonstration purposes.