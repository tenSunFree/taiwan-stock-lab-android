# taiwan-stock-lab-android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84?logo=android&logoColor=white)](https://developer.android.com/build/releases/gradle-plugin)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Multi--Module-4CAF50)](#architecture)
[![Async](https://img.shields.io/badge/Async-Coroutines%20%2B%20Flow-1565C0)](#tech-stack)
[![Data](https://img.shields.io/badge/Data-Offline--First%20%2B%20Room%203-009688)](#offline-first-architecture)
[![DI](https://img.shields.io/badge/DI-Hilt-49A84A)](#dependency-injection)
[![UI](https://img.shields.io/badge/UI-XML%20%2B%20Compose%20Interop-3DDC84?logo=android&logoColor=white)](#ui)
[![Testing](https://img.shields.io/badge/Testing-JUnit5%20%2B%20MockK-FF9800)](#testing)
[![Code Quality](https://img.shields.io/badge/Code%20Quality-ktlint%20%2B%20detekt-blueviolet)](#tech-stack)
[![Observability](https://img.shields.io/badge/Observability-Crashlytics%20%2B%20LeakCanary-FFCA28?logo=firebase&logoColor=black)](#tech-stack)
[![Android CI](https://github.com/tenSunFree/taiwan-stock-lab-android/actions/workflows/ci.yml/badge.svg)](https://github.com/tenSunFree/taiwan-stock-lab-android/actions/workflows/ci.yml)
[![Build](https://img.shields.io/badge/Build-Gradle%20Version%20Catalog-02303A?logo=gradle&logoColor=white)](#tech-stack)

---

## Introduction

A Taiwan stock market Android app built on top of the [TWSE OpenAPI](https://openapi.twse.com.tw/),
aggregating valuation, daily price, average-price, and trading data for listed stocks.

The current implementation is built with Kotlin, Clean Architecture, a multi-module Gradle setup,
structured concurrency, an offline-first Room 3 persistence layer, a Hilt-based dependency injection
composition root, a Hilt-injected presentation state layer using MVI-style Unidirectional Data Flow,
an XML-based stock list screen with a Compose-based market summary component, and a GitHub Actions
CI pipeline.

This repository is intended for learning, technical assessment, and engineering demonstration
purposes.

---

## Preview

<p align="left">
  <img src="https://i.postimg.cc/FsX9CVGG/Screenshot-20260824-033530.png" width="160" alt="Stock list screen showing sorted stocks with price coloring and the market summary bar"/>
  <img src="https://i.postimg.cc/CLpFrsJv/Screenshot-20260824-033540.png" width="160" alt="Sort direction bottom sheet with ascending and descending options"/>
  <img src="https://i.postimg.cc/3ws6rnk1/Screenshot-20260824-034002.png" width="160" alt="Stock detail dialog showing P/E ratio, dividend yield, and P/B ratio"/>
</p>

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
- Local Room access encapsulated behind `StockLocalDataSource`, mirroring `TwseRemoteDataSource` on
  the remote side
- Reactive local observation through `Flow<List<Stock>>`
- Network refresh writes into Room instead of returning remote data directly to consumers
- Failed refreshes and empty/invalid remote snapshots preserve the existing cache
- Coroutine cancellation is propagated instead of being converted into `Result.failure`
- Last successful refresh timestamp persisted transactionally alongside the stock cache, surfaced in
  the UI as "最後更新：MM/dd HH:mm"
- UI state distinguishes "local cache not yet loaded" from "loaded but empty", preventing the
  empty-state text from flashing before the initial Room query completes
- Exported Room schema stored in version control
- Explicit Room schema migration (v1 → v2) preserving existing cached data, with a
  `MigrationTestHelper` test verifying it
- Application-level dependency injection composition root using Hilt, organized by responsibility
  (network, database, repository) rather than by layer, with core and data/domain layers kept
  independent of the DI framework
- Screen-level presentation state managed with a Hilt-injected ViewModel, using `StateFlow` for
  persistent UI state and `SharedFlow` for one-off UI effects (MVI-style Unidirectional Data Flow)
- XML-based stock list screen with `RecyclerView`, `ListAdapter`/`DiffUtil`, and
  `SwipeRefreshLayout`
- Presentation-layer price coloring: closing price above/below the monthly average, and
  positive/negative daily change, each mapped to red/green following Taiwan stock-market convention
- Stock-code sorting (ascending/descending) via a Material bottom sheet, default descending, with a
  single-selection `RadioGroup` showing the current direction
- Sort-direction changes update the ViewModel state atomically and reset the list to the top once
  the newly sorted data is committed
- Stock valuation details (P/E ratio, dividend yield, P/B ratio) via a Material alert dialog
- Initial-loading and empty-state UI handling
- Compose-based market summary bar (`MarketSummaryBar`) embedded into the XML screen via
  `ComposeView`, showing advancing/declining/unchanged stock counts computed by a pure,
  independently-testable function
- GitHub Actions CI running ktlint, detekt, JVM unit tests, Android Lint, and a debug build on every
  push to `main` and every pull request, split into parallel `static-analysis` and `test-build` jobs
- JVM unit tests and Android instrumentation tests
- ktlint (14.2.0) and detekt (2.0.0-alpha.5) configured across all modules from the root Gradle
  build, with formatting applied and all enabled rules passing
- Compose naming conventions handled through annotation-scoped exceptions rather than disabling
  function-naming checks project-wide
- Static analysis passes without a detekt baseline
- Firebase Crashlytics for configured builds, with crash collection controlled per build type via
  an `AndroidManifest.xml` meta-data placeholder rather than a runtime code path — Firebase is
  omitted entirely (not just disabled) when `google-services.json` isn't present, so a clone
  without Firebase configured still builds and runs normally
- LeakCanary 2.14 included through `debugImplementation` only, for automatic memory-leak detection
  with no `Application`-level code required
- Sort selector and stock-detail dialog implemented as `BottomSheetDialogFragment` /
  `DialogFragment` managed by `FragmentManager`, so both survive configuration changes (e.g.
  rotation) instead of being dismissed when the host Activity is recreated; the stock-detail
  dialog stores the values it needs as Fragment arguments rather than re-resolving them from the
  ViewModel, so it also survives process death correctly
- Custom `StockItemAnimator` keeps ordinary add/remove/change item animations while skipping only
  move animations, replacing a blanket `itemAnimator = null`
- Dark mode verified across XML and Compose UI, with no hardcoded colors bypassing the day/night
  system

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

- **`:app`** — Android application entry point and dependency injection composition root. Owns
  application-level Hilt modules and the screen host (`MainActivity`).
- **`:core:common`** — Pure Kotlin/JVM module. Contains shared coroutine abstractions such as
  `DispatchersProvider`. Does not depend on Android or feature modules.
- **`:core:network`** — Reusable networking infrastructure (Retrofit, OkHttp, Moshi,
  `NetworkClientFactory`). Contains no TWSE-specific feature logic.
- **`:core:ui`** — Shared color/theme resources for both XML (`stock_price_up`, `stock_price_down`,
  `stock_card_background`, with light/dark variants) and Compose (`StockLabTheme`,
  `StockLabColors`).
- **`:feature:stocklist`** — Owns all stock-list-specific logic: TWSE API contracts and DTOs, remote
  data source, Room persistence, domain model, repository implementation, presentation state
  (ViewModel), and screen-specific UI rendering (`RecyclerView` adapter, item layout, UI models,
  Compose components).

Core modules never depend on feature modules. The domain layer has no knowledge of Retrofit, Room,
or Android framework classes.

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

`TwseRemoteDataSource` launches the three independent endpoint requests concurrently inside the same
structured-concurrency scope. If one request fails, the snapshot fetch fails as a whole rather than
producing a partially inconsistent snapshot.

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

A failed network request or an empty/invalid remote snapshot does not overwrite the existing local
cache. `observeLastRefreshedAt()` is backed by a single-row `refresh_metadata` table written in the
same Room transaction as the stock cache replacement — the cache and its "last updated" timestamp
can never drift out of sync.

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

`core:*` and `feature:stocklist`'s `data`/`domain` classes contain no Hilt annotations or `@Inject`
constructors. `:app` assembles the complete object graph through explicit `@Provides` modules,
organized by responsibility rather than by layer: `DatabaseModule` only provides Room-specific
types (`StockDatabase`, `StockDao`), while `StockRepositoryModule` provides both data sources
(`TwseRemoteDataSource`, `StockLocalDataSource`) and the `StockRepository` that coordinates them. The
presentation layer (`StockListViewModel`) is the one exception to the "no Hilt in feature classes"
rule — see below.

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

`StockListViewModel` is intentionally Hilt-aware (`@HiltViewModel`) because it belongs to the
Android presentation layer, which is already framework-coupled. The `data` and `domain` layers
remain independent of Hilt.

Initial remote refresh is triggered through an explicit `OnStart` event rather than the ViewModel
constructor, so UI effect collectors are guaranteed to be active before a possible refresh error is
emitted — `MutableSharedFlow` defaults to `replay = 0`, so an effect emitted before any collector
subscribes would otherwise be silently dropped.

ViewModel coroutines call repository suspend functions directly on `viewModelScope` without forcing
a specific dispatcher — the underlying Retrofit and Room APIs already expose main-safe suspend
functions.

`StockListUiState.hasLoadedCache` distinguishes "the local Room query hasn't emitted yet" from "it
emitted an empty result" — without this, the UI briefly showed the empty-state text before the
initial cache arrived, since a default/empty state was indistinguishable from a confirmed-empty
cache.

Selecting a sort direction updates `sortDirection` and the resorted `stocks` list in a single atomic
`StateFlow` update (`applySort(direction)`), avoiding a transient state where the new direction is
paired with the previous ordering. Re-selecting the currently active direction is a no-op — no
re-sort, no state emission.

### UI

The stock list screen is `RecyclerView`-based, hosted by `MainActivity` (`@AndroidEntryPoint`,
`by viewModels()`):

```text
MainActivity
 ├── MaterialToolbar (sort action)
 ├── ComposeView (MarketSummaryBar — advancing/declining/unchanged counts)
 ├── "最後更新：..." timestamp label
 ├── SwipeRefreshLayout
 │      └── RecyclerView (StockListAdapter / ListAdapter + DiffUtil)
 ├── SortBottomSheetFragment (single-selection RadioGroup for sort direction)
 └── StockDetailDialogFragment (stock detail)
```

`StockUiModel`, `PricePosition` (`ABOVE_AVERAGE`/`BELOW_AVERAGE`/`EQUAL`/`UNKNOWN`),
`ChangeDirection` (`POSITIVE`/`NEGATIVE`/`ZERO`/`UNKNOWN`), and `StockUiModelMapper` all live in
`:feature:stocklist/presentation` — these encode stock-specific business rules (closing price vs.
monthly average, change sign) and formatting, not generic app-level view logic. `PricePosition` and
`ChangeDirection` are kept as separate enums rather than a single ambiguous `PriceTrend.UP/DOWN`,
since "closing price above the monthly average" and "the stock rose today" are different facts.

`StockListAdapter` uses `ViewBinding` and `DiffUtil.ItemCallback` (item identity by stock code,
content equality by full `StockUiModel`) for efficient list updates.

`RecyclerView.itemAnimator` uses a custom `StockItemAnimator` rather than the default
`ItemAnimator` or a blanket `itemAnimator = null`. A full stock-code sort reversal reorders nearly
the entire dataset (1,000+ rows), which `DiffUtil` interprets as a large batch of item-move
operations; animating every one of them was visually indistinguishable from the list scrolling on
its own. `StockItemAnimator` overrides only `animateMove` to skip that specific category, while
ordinary add/remove/change animations (e.g. new stocks appearing on refresh, individual price
updates) are kept — a narrower trade-off than disabling item animations entirely. On a
sort-direction change, the list is additionally reset to the top (`scrollToPosition(0)`) once the
newly sorted data is committed via `submitList`'s callback.

Both the sort selector and the stock-detail dialog are `FragmentManager`-hosted
(`BottomSheetDialogFragment` / `DialogFragment`) rather than plain `BottomSheetDialog` /
`MaterialAlertDialogBuilder` instances attached directly to the Activity. A plain `Dialog` is
dismissed automatically when its hosting Activity is destroyed, so rotating the screen while either
was open used to close it with no way back; `FragmentManager` saves and restores both Fragments
automatically across configuration changes:

- `SortBottomSheetFragment` reads the current sort direction from the shared, activity-scoped
  `StockListViewModel` (`by activityViewModels()`) every time its view is (re)created, so a
  restored instance after rotation always reflects the latest state rather than a stale snapshot.
  Selecting an option uses an `OnClickListener` on each `MaterialRadioButton` rather than
  `RadioGroup.OnCheckedChangeListener` — the latter only fires when the checked state actually
  changes, so tapping the already-selected option would otherwise leave the sheet open with no
  feedback.
- `StockDetailDialogFragment` stores the five values it needs to render (`code`, `name`, `peRatio`,
  `dividendYield`, `pbRatio`) as Fragment arguments instead of re-resolving them from the ViewModel
  on (re)creation. Fragment arguments are saved and restored by `FragmentManager` across both
  configuration changes and process death, so the dialog renders correctly even if it's restored
  before the stock list has finished reloading from Room — without requiring `StockUiModel` itself
  to be made `Parcelable`.
- Both `MainActivity.showSortBottomSheet()` and `showStockDetail()` guard against a
  `findFragmentByTag()` hit before calling `showNow()` rather than `show()` — `show()`'s underlying
  `commit()` is queued asynchronously on the main thread, so a rapid double-tap could pass the
  guard before the first transaction has actually executed; `showNow()` commits synchronously,
  making the guard reliable.

### Compose Interoperability

A `MarketSummaryBar` composable is embedded into the existing XML screen (`activity_main.xml`) via
`ComposeView`, demonstrating XML/Compose interop within the same screen rather than a full-screen
rewrite.

```text
MainActivity.setupComposeMarketSummary()
   │
   ▼
ComposeView.setContent { StockLabTheme { MarketSummaryBar(summary) } }
   │
   ├── uiState.stocks (collectAsStateWithLifecycle)
   │
   ▼
computeMarketSummary(stocks)  ← pure function, feature:stocklist/presentation/mapper
   │
   ▼
MarketSummary(advancingCount, decliningCount, unchangedCount)
```

`computeMarketSummary()` is a plain Kotlin function with no Compose dependency, so its counting
logic (classifying each stock by `ChangeDirection`) is covered by ordinary JUnit 5 tests rather than
requiring Compose UI testing infrastructure. `MarketSummaryBar` itself is a stateless composable —
it receives `MarketSummary` as a parameter instead of observing the ViewModel directly, keeping the
Compose component decoupled from business logic.

`StockLabTheme` (in `:core:ui`) defines an explicit `ColorScheme` and `Typography` rather than
relying on Material 3's unconfigured defaults, and `StockLabColors.priceUp`/`priceDown` mirror the
existing XML color resources (`stock_price_up`/`stock_price_down`) so Compose and XML share the same
stock-market color convention instead of diverging into two separate palettes.

### Feature-Internal Layering

`:feature:stocklist` uses package-level Clean Architecture layering instead of splitting every layer
into a separate Gradle module.

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
    │   ├── StockListAdapter.kt
    │   └── StockItemAnimator.kt
    ├── compose/
    │   └── MarketSummaryBar.kt
    ├── contract/
    │   ├── SortDirection.kt
    │   ├── StockListUiState.kt
    │   ├── StockListUiEvent.kt
    │   └── StockListUiEffect.kt
    ├── dialog/
    │   ├── SortBottomSheetFragment.kt
    │   └── StockDetailDialogFragment.kt
    ├── model/
    │   ├── StockUiModel.kt
    │   ├── MarketSummary.kt
    │   ├── PricePosition.kt
    │   └── ChangeDirection.kt
    └── mapper/
        ├── StockUiModelMapper.kt
        └── MarketSummaryMapper.kt
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
- Application-level composition root, modules organized by responsibility (network, database,
  repository)
- Data and domain layers remain Hilt-independent

**Presentation**

- MVVM
- MVI-style Unidirectional Data Flow
- `StateFlow` for persistent UI state
- `SharedFlow` for one-off UI effects
- Hilt-injected screen-level ViewModel (`@HiltViewModel`)

**UI**

- XML layouts + ViewBinding
- `RecyclerView` + `ListAdapter` / `DiffUtil` + custom `StockItemAnimator`
- `SwipeRefreshLayout`
- Material Components (`MaterialCardView`, `MaterialToolbar`, `BottomSheetDialogFragment`,
  `RadioGroup`/`MaterialRadioButton`)
- Fragment KTX (`androidx.fragment:fragment-ktx`), for `BottomSheetDialogFragment` /
  `DialogFragment` (`activityViewModels()`, `Fragment` arguments) managed by `FragmentManager` so
  transient UI survives configuration changes and process death
- `repeatOnLifecycle` for lifecycle-aware `Flow` collection

**Compose Interoperability**

- Jetpack Compose (Compose BOM 2026.06.01)
- Compose Material 3
- `ComposeView` embedded within an XML screen
- Explicit `StockLabTheme` / `StockLabColors` shared with XML color resources

**Code Quality**

- ktlint 14.2.0 (`org.jlleitschuh.gradle.ktlint`), applied across all modules from the root build
- detekt 2.0.0-alpha.5 (`dev.detekt`), applied across all modules from the root build,
  `buildUponDefaultConfig = true` with a minimal project override (`config/detekt/detekt.yml`)
- `.editorconfig` for repository-wide Kotlin/Kotlin-DSL formatting rules
- `FunctionNaming` remains enabled; Jetpack Compose `@Composable` functions use an
  annotation-scoped naming exception rather than disabling the rule project-wide
- The existing underscore-based base package retains an explicit `PackageNaming` exception
- All enabled ktlint and detekt rules pass with zero findings and no detekt baseline
- `ktlintCheck` and `detekt` run in a dedicated `static-analysis` CI job, in parallel with the
  `test-build` job, on every push and pull request; a violation fails the `static-analysis` job
  independently of the test/build result

### Static Analysis Policy

ktlint owns Kotlin formatting and style enforcement, while detekt focuses on code smells,
complexity, naming, and maintainability concerns.

Project-wide rule disables are avoided where a narrower exception is available. In particular,
`FunctionNaming` remains enabled for ordinary Kotlin functions, while `@Composable` functions are
excluded through annotation-aware configuration because PascalCase composable names follow the
Jetpack Compose convention.

`PackageNaming` is intentionally disabled because the existing project namespace contains
underscores; renaming the namespace would be a repository-wide migration unrelated to the current
quality-tooling scope.

No detekt baseline is used. Existing findings are either resolved, narrowly suppressed with an
explicit rationale, or documented as deliberate project-level rule exceptions.

**Observability**

- Firebase Crashlytics for production crash, non-fatal error, and ANR reporting
- Firebase Analytics included for Crashlytics breadcrumb context
- Firebase configuration is optional for this public repository:
    - `app/google-services.json` present → the `google-services` and `firebase-crashlytics`
      Gradle plugins are applied and the Firebase SDKs are included
    - file absent → Firebase is omitted entirely; the project still builds and runs normally,
      just without crash reporting
- Crash collection is controlled per build type through the `firebase_crashlytics_collection_enabled`
  manifest meta-data, set via `manifestPlaceholders` in `app/build.gradle.kts` — disabled in debug
  builds so local development crashes never pollute production crash-free-user metrics, enabled in
  release builds
- `TaiwanStockApplication` has no Firebase-specific code; collection state is entirely a build-time
  concern (plugin application + manifest placeholder), not a runtime branch
- LeakCanary (2.14), `debugImplementation` only — automatically detects leaked `Activity`,
  `Fragment`, `View`, `ViewModel`, and `Service` instances with no code changes required

**Testing**

- JUnit 5
- MockK
- kotlinx-coroutines-test
- Turbine
- AndroidJUnit4 / AndroidX Test
- Room in-memory database tests
- Room `MigrationTestHelper`
- Hilt instrumentation testing (`hilt-android-testing`, custom `HiltTestRunner`)

**CI/CD**

- GitHub Actions, two parallel jobs on every push/PR to `main`:
    - `static-analysis` — ktlint + detekt
    - `test-build` — JVM unit tests, Android Lint, debug build
- ktlint reports (plain text + Checkstyle XML), detekt reports (HTML + SARIF), and test/lint
  reports are uploaded as workflow artifacts (7-day retention) when produced

**Planned**

- Espresso UI tests

---

## Testing

The project separates fast JVM unit tests from Android instrumentation tests:

```text
src/test/          JVM unit tests
src/androidTest/   Android runtime / integration tests
```

**Numeric Parsing** — `TwseNumericParserTest` covers null values, empty values, TWSE missing-value
sentinels, invalid numeric strings, thousands separators, decimal parsing, and integer parsing.

**Dataset Aggregation** — `StockMapperTest` covers joining the three TWSE datasets by stock code,
preserving stocks when optional valuation data is missing, and rejecting invalid rows without a
required code or name.

**Structured Concurrency** — `TwseRemoteDataSourceTest` verifies that three mocked endpoint calls,
each delayed by one virtual second, complete in approximately one virtual second rather than three.

**Offline-First Repository** — `OfflineFirstStockRepositoryTest` covers reading cached stocks via
`StockLocalDataSource`, reading/writing the last-refreshed timestamp, successful remote refresh,
preserving cache after network failure/empty snapshot, and propagating `CancellationException`.

**Room DAO** — `StockDaoTest` is an Android instrumentation test using a real in-memory Room
database with `BundledSQLiteDriver`, covering atomic stock+metadata replacement.

**Room Migration** — `StockDatabaseMigrationTest` uses Room's `MigrationTestHelper` to verify
`MIGRATION_1_2` preserves existing stock rows and correctly adds the `refresh_metadata` table.

**Dependency Injection** — `StockRepositoryInjectionTest` is an Android instrumentation test
verifying the production Hilt dependency graph resolves and injects `StockRepository`.

**Presentation State** — `StockListViewModelTest` covers cached-stock observation, `hasLoadedCache`
becoming true even for an empty cache, `OnStart` triggering the initial refresh exactly once,
explicit `OnRefresh`, atomic sort-direction + stock-order updates, no-op behavior when re-selecting
the current sort direction, refresh failure state/effect, last-refreshed timestamp exposure, and
stock-detail effects for known/unknown stock codes — using JUnit 5, MockK,
`kotlinx-coroutines-test` (`StandardTestDispatcher`), and Turbine.

**UI Formatting Rules** — `StockUiModelMapperTest` covers price-position classification (above/below
monthly average), change-direction classification (positive/negative), null-value placeholders,
thousands-separator formatting, and the `+`/`-` sign on the change value.

**Market Summary Calculation** — `MarketSummaryMapperTest` covers counting stocks by change
direction and returning an all-zero summary for an empty list, independent of any Compose runtime.

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
selectingAscendingSort_updatesDirectionAndStocksAtomically
selectingCurrentSortDirection_isANoOp
computeMarketSummary_countsStocksByChangeDirection
computeMarketSummary_returnsAllZeroForEmptyList
```

---

## Continuous Integration

Every push to `main` and every pull request triggers a GitHub Actions workflow
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) with two parallel jobs:

```text
static-analysis:  checkout → setup JDK 17 → setup Gradle → ktlintCheck → detekt
test-build:       checkout → setup JDK 17 → setup Gradle → test → lint → assembleDebug
```

`static-analysis` and `test-build` run as independent, parallel jobs — a `static-analysis` failure
does not block or gate `test-build`. `ktlintCheck` and `detekt` run across all modules and fail the
`static-analysis` job on any enabled rule violation — no detekt baseline is used, so every finding
must be resolved or narrowly suppressed with an explicit rationale. Compose `@Composable` functions
use annotation-scoped naming exceptions, while the existing underscore-based base package retains an
explicit package-naming exemption.

ktlint reports (plain text + Checkstyle XML), detekt reports (HTML + SARIF), test reports, and
Android Lint reports are uploaded as workflow artifacts (7-day retention) whenever the corresponding
task produces them, regardless of whether the job passes or fails, so a failure can usually be
diagnosed directly from the Actions run without reproducing it locally.

Instrumented tests (`connectedDebugAndroidTest`) are not run in this workflow, since Android
emulators in CI add meaningful setup and boot-time complexity and are planned as a separate workflow
rather than blocking every push.

---

## Git Workflow

The repository is developed through small, independently reviewable Pull Requests. Each
architectural concern is introduced in its own focused change, such as: project foundation, network
infrastructure, TWSE aggregation, Room persistence, dependency injection, presentation state, UI,
and quality tooling.

Dependencies are introduced when first needed rather than being added up front.

The repository also contains a [Pull Request template](.github/pull_request_template.md) covering
Summary, Changes, Architecture, Verification, Screenshots, Notes, and Related work.
A [GitHub Actions workflow](.github/workflows/ci.yml) runs tests, lint, and a debug build on every
push and pull request against `main`.

---

## Roadmap

| Stage                    | Scope                                                                                                 | Status    |
|--------------------------|-------------------------------------------------------------------------------------------------------|-----------|
| Project foundation       | Multi-module setup, Version Catalog, `core:common`                                                    | ✅ Done    |
| Network infrastructure   | Retrofit, OkHttp, Moshi, `core:network`                                                               | ✅ Done    |
| TWSE remote layer        | API service and DTOs                                                                                  | ✅ Done    |
| Domain + aggregation     | `Stock`, numeric parsing, concurrent fetch, merge by code                                             | ✅ Done    |
| Persistence              | Room 3, DAO, schema export                                                                            | ✅ Done    |
| Offline-first repository | Room source of truth, refresh/cache policy                                                            | ✅ Done    |
| Dependency injection     | Hilt composition root                                                                                 | ✅ Done    |
| Presentation state       | ViewModel + MVI-style UDF                                                                             | ✅ Done    |
| XML UI                   | Stock list, sorting, detail dialog                                                                    | ✅ Done    |
| Cache metadata           | Local data source abstraction, loading-state fix, last-updated timestamp, schema migration            | ✅ Done    |
| Sort UX polish           | Single-selection sort UI, atomic sort state, stable scroll-to-top                                     | ✅ Done    |
| Quality tooling          | GitHub Actions CI (test, lint, build)                                                                 | ✅ Done    |
| Compose interoperability | `ComposeView` custom components (`MarketSummaryBar`)                                                  | ✅ Done    |
| Quality tooling          | ktlint, detekt — plugin wiring and minimal config across all modules                                  | ✅ Done    |
| Quality tooling          | ktlint, detekt — formatting pass and enabled-rule findings resolved (zero baseline)                   | ✅ Done    |
| Quality tooling          | ktlint, detekt — CI integration                                                                       | ✅ Done    |
| Observability            | Firebase Crashlytics (optional, config-gated), LeakCanary (debug-only)                                | ✅ Done    |
| Polish                   | Dark mode verification, rotation-safe dialogs (`FragmentManager`), narrower item animations           | ✅ Done    |
| Scaling                  | Paging 3 for the stock list, if dataset size grows significantly                                      | ⏳ Future  |

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
git clone https://github.com/kw012345678/taiwan-stock-lab-android.git
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

Run Room instrumentation tests, including the schema migration test (requires a running emulator or
physical device):

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

Run ktlint checks across all modules:

```bash
./gradlew ktlintCheck
```

Auto-format Kotlin sources with ktlint:

```bash
./gradlew ktlintFormat
```

Run detekt static analysis across all modules:

```bash
./gradlew detekt
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
│       │   │   │   └── activity_main.xml
│       │   │   ├── menu/
│       │   │   │   └── menu_stock_list.xml
│       │   │   ├── drawable/
│       │   │   │   └── ic_sort.xml
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
│       └── src/main/
│           ├── kotlin/.../core/ui/theme/
│           │   └── StockLabTheme.kt
│           └── res/
│               ├── values/colors.xml
│               └── values-night/colors.xml
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
│           │   │       ├── compose/
│           │   │       ├── contract/
│           │   │       ├── dialog/
│           │   │       │   ├── SortBottomSheetFragment.kt
│           │   │       │   └── StockDetailDialogFragment.kt
│           │   │       ├── model/
│           │   │       └── mapper/
│           │   └── res/
│           │       ├── layout/
│           │       │   ├── item_stock_card.xml
│           │       │   └── bottom_sheet_sort.xml
│           │       ├── drawable/bg_bottom_sheet_handle.xml
│           │       └── values/strings.xml
│           │
│           ├── test/kotlin/
│           └── androidTest/
│               └── kotlin/.../data/local/
│                   ├── StockDaoTest.kt
│                   └── StockDatabaseMigrationTest.kt
│
├── config/
│   └── detekt/
│       └── detekt.yml
│
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│
├── .github/
│   ├── pull_request_template.md
│   └── workflows/
│       └── ci.yml
│
├── .editorconfig
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Data Source

Stock market data is provided by the
public [Taiwan Stock Exchange OpenAPI](https://openapi.twse.com.tw/).

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
- XML/Jetpack Compose interoperability
- repository-wide code-style enforcement
- static-analysis rule governance
- zero-baseline static analysis
- continuous integration
- automated testing
- incremental delivery through reviewable Pull Requests

---

## License

This repository is currently intended for learning, technical assessment, and demonstration
purposes.
