# taiwan-stock-lab-android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84?logo=android&logoColor=white)](https://developer.android.com/build/releases/gradle-plugin)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Multi--Module-4CAF50)](#architecture)
[![Async](https://img.shields.io/badge/Async-Coroutines%20%2B%20Flow-1565C0)](#tech-stack)
[![Data](https://img.shields.io/badge/Data-Offline--First%20%2B%20Room%203-009688)](#offline-first-architecture)
[![Paging](https://img.shields.io/badge/Paging-Room%20PagingSource%20%2B%20SQL%20Aggregates-4285F4)](#offline-first-architecture)
[![DI](https://img.shields.io/badge/DI-Hilt-49A84A)](#dependency-injection)
[![UI](https://img.shields.io/badge/UI-XML%20%2B%20Compose%20Interop-3DDC84?logo=android&logoColor=white)](#ui)
[![Testing](https://img.shields.io/badge/Testing-JUnit5%20%2B%20MockK%20%2B%20Espresso%2FCompose-FF9800)](#testing)
[![Code Quality](https://img.shields.io/badge/Code%20Quality-ktlint%20%2B%20detekt-blueviolet)](#tech-stack)
[![Observability](https://img.shields.io/badge/Observability-Crashlytics%20%2B%20LeakCanary-FFCA28?logo=firebase&logoColor=black)](#tech-stack)
[![Android CI](https://github.com/tenSunFree/taiwan-stock-lab-android/actions/workflows/ci.yml/badge.svg)](https://github.com/tenSunFree/taiwan-stock-lab-android/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/tenSunFree/taiwan-stock-lab-android/graph/badge.svg)](https://codecov.io/gh/tenSunFree/taiwan-stock-lab-android)
[![Build](https://img.shields.io/badge/Build-Gradle%20Version%20Catalog-02303A?logo=gradle&logoColor=white)](#tech-stack)

---

## Introduction

A Taiwan stock market Android app built on top of the [TWSE OpenAPI](https://openapi.twse.com.tw/),
aggregating valuation, daily price, average-price, and trading data for listed stocks.

The current implementation is built with Kotlin, Clean Architecture, a multi-module Gradle setup,
structured concurrency, an offline-first Room 3 persistence layer, a Hilt-based dependency injection
composition root, a Hilt-injected presentation state layer using MVI-style Unidirectional Data Flow,
an XML-based stock list screen with a Compose-based market summary component, Espresso and Compose
UI test coverage across both the XML and Compose surfaces, and a GitHub Actions CI pipeline.

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
- Reactive local observation split across three purpose-scoped reads — `Flow<PagingData<Stock>>`
  (list rendering), a single-row lookup, and a `Flow<MarketChangeSummary>` aggregate — see
  [Offline-First Architecture](#offline-first-architecture)
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
- XML-based stock list screen with `RecyclerView`, `PagingDataAdapter`/`DiffUtil`, and
  `SwipeRefreshLayout`
- Presentation-layer price coloring: closing price above/below the monthly average, and
  positive/negative daily change, each mapped to red/green following Taiwan stock-market convention
- Stock-code sorting (ascending/descending) via a Material bottom sheet, default descending, with a
  single-selection `RadioGroup` showing the current direction
- Stock valuation details (P/E ratio, dividend yield, P/B ratio) via a Material alert dialog
- Initial-loading and empty-state UI handling
- Compose-based market summary bar (`MarketSummaryBar`) embedded into the XML screen via
  `ComposeView`, showing advancing/declining/unchanged stock counts computed by a pure,
  independently-testable function
- Espresso UI tests for the XML stock list screen (RecyclerView item click → stock detail dialog),
  a pure Compose UI test for `MarketSummaryBar` using the semantics tree, and a mixed
  Espresso + Compose UI test (`createAndroidComposeRule<MainActivity>`) covering sort-direction
  changes that reorder the XML `RecyclerView` while asserting the embedded Compose content stays
  correct — backed by a `FakeStockRepository` installed via Hilt `@TestInstallIn`, so these tests
  never hit the real TWSE API
- GitHub Actions CI running ktlint, detekt, JVM unit tests, JaCoCo unit-test coverage (uploaded to
  Codecov), Android Lint, and a debug build on every push to `main` and every pull request, split
  into parallel `static-analysis` and `test-build` jobs
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
- Room-backed Paging 3 for the stock list: `StockDao` exposes ascending/descending
  `PagingSource<Int, StockEntity>` queries, wired through a `Pager` and collected by a
  `PagingDataAdapter`, replacing full in-memory list materialization for RecyclerView rendering
- Market summary (advancing/declining/unchanged counts) computed as a single SQL aggregate query
  (`SUM`/`CASE` over the `change` column) rather than classifying a fully-materialized stock list
  in Kotlin
- Stock-detail lookup resolves a clicked stock code via a single-row Room query
  (`SELECT ... WHERE code = :code`) instead of scanning an in-memory list
- Sort-direction changes are a query-level concern (`ORDER BY code ASC`/`DESC`, re-subscribed via
  `flatMapLatest`) rather than an in-memory re-sort, so the paged stream never needs the full
  dataset in memory to reorder it

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
```

Room is the single source of truth, but the repository exposes **three separate read paths**
rather than one, because "the RecyclerView's visible rows" and "counts/lookups over the whole
dataset" are genuinely different concerns:

```kotlin
// Paged — feeds the RecyclerView. Sort direction is a query-level ORDER BY, not an in-memory sort.
fun observeStocksPaged(direction: SortDirection): Flow<PagingData<Stock>>

// Single-row — resolves a clicked stock code without scanning a full in-memory list.
suspend fun getStock(code: String): Stock?

// DB-side aggregate — advancing/declining/unchanged counts via SUM/CASE, not a Kotlin classify-
// and-count pass over every loaded row.
fun observeMarketSummary(): Flow<MarketChangeSummary>

fun observeLastRefreshedAt(): Flow<Long?>

suspend fun refreshStocks(): Result<Unit>
```

```text
observeStocksPaged(direction)
   │
   ▼
StockDao.observeAllAscendingPaged() / observeAllDescendingPaged()
   │
   │ PagingSource<Int, StockEntity>, via Pager
   ▼
StockLocalDataSource: Flow<PagingData<StockEntity>>
   │
   ▼
OfflineFirstStockRepository: Flow<PagingData<Stock>>  (StockEntityMapper.toDomain())
   │
   ▼
StockListViewModel.stocksPagingData: Flow<PagingData<StockUiModel>>
   (StockUiModelMapper.toUiModel(), .cachedIn(viewModelScope))
   │
   ▼
MainActivity: adapter.submitData(pagingData)
   │
   ▼
StockListAdapter (PagingDataAdapter) → RecyclerView
```

```text
observeMarketSummary()
   │
   ▼
StockDao.observeMarketSummary()  ← SUM/CASE over `change`, in SQL
   │
   ▼
StockLocalDataSource: Flow<MarketSummaryRow>
   │
   ▼
OfflineFirstStockRepository: Flow<MarketChangeSummary>
   │
   ▼
StockListViewModel: collected in observeMarketSummary(),
   mapped via MarketChangeSummary.toUiModel() → MarketSummary
   │
   ▼
StockListUiState.marketSummary  (StateFlow)
   │
   ▼
MainActivity: uiState.marketSummary (collectAsStateWithLifecycle)
   │
   ▼
MarketSummaryBar (Compose)
```

```text
StockListAdapter: user taps a card
   │
   ▼
MainActivity: viewModel.onEvent(OnStockClicked(stockCode))
   │
   ▼
StockListViewModel.onStockClicked(stockCode)
   │
   ▼
StockRepository.getStock(code)
   │
   ▼
StockLocalDataSource.getStock(code) → StockDao.getByCode(code)
   │
   │ StockEntity?
   ▼
Stock?  (StockEntityMapper.toDomain())
   │
   ▼
StockUiModel  (StockUiModelMapper.toUiModel())
   │
   ▼
StockListViewModel: _uiEffect.emit(ShowStockDetail(stock))
   │
   ▼
MainActivity.handleEffect() → StockDetailDialogFragment.newInstance(stock).showNow(...)
```

An earlier version of this repository exposed a single `Flow<List<Stock>>` for everything —
list rendering, the market summary, and detail lookups all read the same fully-materialized list.
Paging 3 replaced that for list rendering specifically, but a paged stream can't answer "how many
stocks are up today" or "find stock 2330" without walking every loaded page, so those two concerns
were moved to their own dedicated, smaller queries instead of keeping a second full-list read
around just for them.

`change` is stored in Room as either SQL `NULL` or a clean plain-decimal string — never a raw TWSE
sentinel like `"-"` — because `TwseNumericParser` already normalizes it during `StockMapper.merge()`
before a `Stock` is persisted. `observeMarketSummary()`'s `CAST(change AS REAL)` aggregate relies on
that guarantee; it would not be safe to write directly against the raw remote strings.

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

For instrumented UI tests, `app/src/androidTest`'s `FakeStockRepositoryModule` uses Hilt's
`@TestInstallIn` to replace `StockRepositoryModule` with a `FakeStockRepository` — see
[Testing](#testing).

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

`StockListViewModel.stocksPagingData` (`Flow<PagingData<StockUiModel>>`) is exposed as its own
property rather than a field on `StockListUiState`: `PagingData` isn't meaningfully comparable or
storable in a plain `data class`, and is designed to be collected by exactly one
`PagingDataAdapter`. It's built by `flatMapLatest`-ing a private `sortDirection` `MutableStateFlow`
into `stockRepository.observeStocksPaged(direction)`, so a sort-direction change re-subscribes to a
freshly-queried `Pager` (`ORDER BY` reversed at the SQL level) instead of re-sorting an in-memory
list, then `.cachedIn(viewModelScope)` to survive configuration changes.

The "cache not loaded yet" vs. "loaded but empty" distinction — previously encoded in a
`StockListUiState.hasLoadedCache` flag — is now covered natively by `PagingDataAdapter`'s
`loadStateFlow` (`LoadState.Loading` vs. `LoadState.NotLoading` combined with `itemCount == 0`),
which is why that field no longer exists on `StockListUiState`. `MainActivity` additionally
combines that `LoadState` with `state.isRefreshing`, since they answer different questions: Paging's
`LoadState` reflects the *local Room query* (which finishes almost instantly even on an empty
cache), while `isRefreshing` reflects the *network* refresh that's populating that cache — without
combining both, a cold start on an empty cache would flash the empty-state text for the second or
two the network fetch is still in flight. A third `LoadState.Error` branch is handled the same way,
showing a tappable "load failed, tap to retry" message (`adapter.retry()`) when the local Room
paging query itself fails with no items loaded — distinct from a network refresh failure, which
surfaces as a `Snackbar` instead, since the existing cached list can stay visible while that error
is shown.

Re-selecting the currently active sort direction is a no-op — the `MutableStateFlow` backing
`stocksPagingData` is never reassigned, so no new `Pager` is created and no state emission happens.

### UI

The stock list screen is `RecyclerView`-based, hosted by `MainActivity` (`@AndroidEntryPoint`,
`by viewModels()`):

```text
MainActivity
 ├── MaterialToolbar (sort action)
 ├── ComposeView (MarketSummaryBar — advancing/declining/unchanged counts)
 ├── "最後更新：..." timestamp label
 ├── SwipeRefreshLayout
 │      └── RecyclerView (StockListAdapter / PagingDataAdapter + DiffUtil)
 ├── SortBottomSheetFragment (single-selection RadioGroup for sort direction)
 └── StockDetailDialogFragment (stock detail)
```

`StockUiModel`, `PricePosition` (`ABOVE_AVERAGE`/`BELOW_AVERAGE`/`EQUAL`/`UNKNOWN`),
`ChangeDirection` (`POSITIVE`/`NEGATIVE`/`ZERO`/`UNKNOWN`), and `StockUiModelMapper` all live in
`:feature:stocklist/presentation` — these encode stock-specific business rules (closing price vs.
monthly average, change sign) and formatting, not generic app-level view logic. `PricePosition` and
`ChangeDirection` are kept as separate enums rather than a single ambiguous `PriceTrend.UP/DOWN`,
since "closing price above the monthly average" and "the stock rose today" are different facts.

`StockListAdapter` is a `PagingDataAdapter` using `ViewBinding` and `DiffUtil.ItemCallback` (item
identity by stock code, content equality by full `StockUiModel`) for efficient list updates.

`RecyclerView.itemAnimator` uses a custom `StockItemAnimator` rather than the default
`ItemAnimator` or a blanket `itemAnimator = null`. A full stock-code sort reversal reorders nearly
the entire dataset (1,000+ rows), which `DiffUtil` interprets as a large batch of item-move
operations; animating every one of them was visually indistinguishable from the list scrolling on
its own. `StockItemAnimator` overrides only `animateMove` to skip that specific category, while
ordinary add/remove/change animations (e.g. new stocks appearing on refresh, individual price
updates) are kept — a narrower trade-off than disabling item animations entirely. On a
sort-direction change, `MainActivity` scrolls to the top (`scrollToPosition(0)`) as soon as the new
direction is detected in `StockListUiState`, rather than waiting on a load-completion callback —
since the change now re-queries Room in a new order (see [Offline-First
Architecture](#offline-first-architecture)) instead of re-sorting an already-loaded in-memory list,
there's no single "commit" moment to hook the scroll reset to.

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
   ├── uiState.marketSummary (collectAsStateWithLifecycle)
   │      │
   │      ▲
   │   StockDao.observeMarketSummary()  ← SQL aggregate, see Offline-First Architecture
   ▼
MarketSummaryBar(advancingCount, decliningCount, unchangedCount)
```

`MarketSummary` is now computed as a Room SQL aggregate (`SUM`/`CASE` over `change`) rather than a
Kotlin function classifying a fully-materialized `List<StockUiModel>`, so `MainActivity` just reads
`uiState.marketSummary` directly — no `remember`/re-derivation step is needed in the Compose call
site, since the ViewModel already emits the finished count. `MarketSummaryBar` itself remains a
stateless composable — it receives `MarketSummary` as a parameter instead of observing the
ViewModel directly, keeping the Compose component decoupled from business logic.

`StockLabTheme` (in `:core:ui`) defines an explicit `ColorScheme` and `Typography` rather than
relying on Material 3's unconfigured defaults, and `StockLabColors.priceUp`/`priceDown` mirror the
existing XML color resources (`stock_price_up`/`stock_price_down`) so Compose and XML share the same
stock-market color convention instead of diverging into two separate palettes.

`MarketSummaryBar` also exposes a `MarketSummaryBarTestTags` object (`ROOT`, `ADVANCING`,
`DECLINING`, `UNCHANGED`) and applies `Modifier.testTag(...)` plus
`Modifier.semantics(mergeDescendants = true) {}` on each summary item — the latter collapses the
label and count `Text` composables' semantics onto the tagged node, since a plain `Row` does not
merge its children's semantics by default. Compose UI Test then queries this composable through
its semantics tree (`onNodeWithTag`) instead of View IDs — see [Testing](#testing).

### Feature-Internal Layering

`:feature:stocklist` uses package-level Clean Architecture layering instead of splitting every layer
into a separate Gradle module.

```text
feature/stocklist/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── StockDao.kt
│   │   │   └── MarketSummaryRow.kt
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
│   │   ├── Stock.kt
│   │   ├── SortDirection.kt
│   │   └── MarketChangeSummary.kt
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
- `androidx.room3:room3-paging` — registers `PagingSourceDaoReturnTypeConverter` via
  `@DaoReturnTypeConverters`, so `StockDao` queries can return `PagingSource<Int, StockEntity>`
- Exported and version-controlled Room schemas
- Explicit schema migrations (`Migration` + `MigrationTestHelper`)

**Dependency Injection**

- Hilt 2.60.1
- KSP code generation
- Application-level composition root, modules organized by responsibility (network, database,
  repository)
- Data and domain layers remain Hilt-independent
- `@TestInstallIn` used in instrumented tests to swap production modules for deterministic fakes

**Presentation**

- MVVM
- MVI-style Unidirectional Data Flow
- `StateFlow` for persistent UI state
- `SharedFlow` for one-off UI effects
- Hilt-injected screen-level ViewModel (`@HiltViewModel`)
- Paging 3 (`androidx.paging:paging-runtime` 3.4.2) — `Pager`, `PagingData`, `cachedIn`,
  `PagingDataAdapter`, `LoadState`

**UI**

- XML layouts + ViewBinding
- `RecyclerView` + `PagingDataAdapter` / `DiffUtil` + custom `StockItemAnimator`
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
- Semantics-based test tags (`MarketSummaryBarTestTags`, `Modifier.testTag`,
  `Modifier.semantics(mergeDescendants = true)`) for Compose UI Test

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
- `androidx.paging:paging-testing` (`asSnapshot()`, `PagingData.from(list, sourceLoadStates = ...)`
  for testing `Flow<PagingData<T>>` built on a non-completable upstream)
- AndroidJUnit4 / AndroidX Test
- Room in-memory database tests
- Room `MigrationTestHelper`
- Hilt instrumentation testing (`hilt-android-testing`, custom `HiltTestRunner`)
- Hilt `@TestInstallIn` for swapping the production `StockRepository` with a deterministic
  `FakeStockRepository` in instrumented tests, avoiding real network calls
- Espresso (`espresso-core`, `espresso-contrib` for `RecyclerViewActions`) for XML/View screen tests
- Compose UI Test (`ui-test-junit4`, `ui-test-manifest`) — `createComposeRule()` for isolated
  Compose component tests, `createAndroidComposeRule<MainActivity>()` for mixed XML + Compose
  screen tests

**CI/CD**

- GitHub Actions, three parallel jobs on every push/PR to `main`:
    - `static-analysis` — ktlint + detekt
    - `test-build` — JVM unit tests, JaCoCo unit-test coverage, Android Lint, debug build
    - `secret-scan` — gitleaks against the full commit history
- ktlint reports (plain text + Checkstyle XML), detekt reports (HTML + SARIF), and test/lint
  reports are uploaded as workflow artifacts (7-day retention) when produced
- JVM unit-test coverage for `:feature:stocklist` is generated via the Android Gradle Plugin's
  `enableUnitTestCoverage` (JaCoCo under the hood) and uploaded to
  [Codecov](https://codecov.io/gh/tenSunFree/taiwan-stock-lab-android) on every push/PR

---

## Testing

The project separates fast JVM unit tests from Android instrumentation tests:

```text
src/test/          JVM unit tests
src/androidTest/   Android runtime / integration tests
```

**Code Coverage** — JVM unit-test coverage is generated for `:feature:stocklist` via the Android
Gradle Plugin's `enableUnitTestCoverage` (`createDebugUnitTestCoverageReport`, JaCoCo under the
hood) and uploaded to [Codecov](https://codecov.io/gh/tenSunFree/taiwan-stock-lab-android) on CI.
It's currently the only module with a `src/test` suite — `app`, `core:common`, `core:network`, and
`core:ui` have no JVM tests yet, so they aren't included. This coverage also does not include any
`src/androidTest` tests (`StockDaoTest`, `StockDatabaseMigrationTest`, `MarketSummaryBarTest`,
`StockListEspressoTest`, `StockListMixedComposeEspressoTest`) — those are instrumentation tests
that CI currently only compiles (`assembleDebugAndroidTest`), not executes on an emulator, so
their runtime coverage isn't part of the Codecov report. `codecov.yml`'s status checks are
currently `informational: true` while the reporting baseline gets established, so they surface
data without blocking merges.

**Numeric Parsing** — `TwseNumericParserTest` covers null values, empty values, TWSE missing-value
sentinels, invalid numeric strings, thousands separators, decimal parsing, and integer parsing.

**Dataset Aggregation** — `StockMapperTest` covers joining the three TWSE datasets by stock code,
preserving stocks when optional valuation data is missing, and rejecting invalid rows without a
required code or name.

**Structured Concurrency** — `TwseRemoteDataSourceTest` verifies that three mocked endpoint calls,
each delayed by one virtual second, complete in approximately one virtual second rather than three.

**Offline-First Repository** — `OfflineFirstStockRepositoryTest` covers mapping paged stock entities
to domain stocks (`observeStocksPaged`, via `androidx.paging.testing.asSnapshot()`), single-row
stock lookup (`getStock`, found/not-found), mapping the market-summary aggregate row to a domain
`MarketChangeSummary`, reading the last-refreshed timestamp, successful remote refresh, preserving
cache after network failure/empty snapshot, and propagating `CancellationException`.

**Room DAO** — `StockDaoTest` is an Android instrumentation test using a real in-memory Room
database with `BundledSQLiteDriver`, covering transactional stock+refresh-metadata replacement
(the success path — no test deliberately fails partway through a `replaceAll()` to verify
rollback), ascending/descending paged queries exercised via genuine `Refresh`/`Append`
`PagingSource.load()` calls (not a one-shot "load everything" workaround), single-row lookup by
code, and the market-summary SQL aggregate across every bucket — including the case a `NULL`
`change` column is correctly counted as unchanged, per SQL's three-valued `NULL = 0` semantics.

**Room Migration** — `StockDatabaseMigrationTest` uses Room's `MigrationTestHelper` to verify
`MIGRATION_1_2` preserves existing stock rows and correctly adds the `refresh_metadata` table.

**Dependency Injection** — `StockRepositoryInjectionTest` is an Android instrumentation test
verifying the production Hilt dependency graph resolves and injects `StockRepository`.

**Presentation State** — `StockListViewModelTest` covers `stocksPagingData` reflecting the paged
stream for the default and a re-selected sort direction (via `asSnapshot()`), `OnStart` triggering
the initial refresh exactly once, no-op behavior when re-selecting the current sort direction,
refresh failure state/effect, market-summary exposure through `StockListUiState`, last-refreshed
timestamp exposure, and stock-detail effects for known/unknown stock codes — using JUnit 5, MockK,
`kotlinx-coroutines-test` (`StandardTestDispatcher`), and Turbine. `PagingData.from(list,
sourceLoadStates = ...)` is used with explicit `LoadStates` rather than the no-argument overload,
since `stocksPagingData` is built on a non-completable `MutableStateFlow` upstream — without
explicit `LoadStates`, `asSnapshot()` has no signal that a page finished loading and hangs.

**XML Screen (Espresso)** — `StockListEspressoTest` verifies that tapping the first stock card in
the `RecyclerView` opens `StockDetailDialogFragment` with the matching stock code, using
`RecyclerViewActions` (`espresso-contrib`) and a custom `BoundedMatcher` (`withRecyclerViewItem`)
to assert on a specific adapter position's bound content.

**Compose Component (Compose UI Test)** — `MarketSummaryBarTest` renders `MarketSummaryBar` in
isolation via `createComposeRule()` and asserts on its semantics tree (`onNodeWithTag`,
`assertTextContains`) rather than View IDs — it needs no Activity, Hilt, or repository.

**Mixed XML + Compose Screen** — `StockListMixedComposeEspressoTest` uses
`createAndroidComposeRule<MainActivity>()` to drive Espresso against the XML Toolbar,
`SortBottomSheetFragment`, and `RecyclerView` (changing sort direction) in the same test as Compose
UI Test assertions against the embedded `MarketSummaryBar`. A `waitUntilEspressoAssertionPasses`
helper polls Espresso assertions via `ComposeTestRule.waitUntil`, bridging the asynchronous Room
re-query + Paging diff triggered by the sort change — Espresso does not automatically synchronize
with that kind of application-specific coroutine work.

All three instrumented UI tests run against a `FakeStockRepository` installed via a Hilt
`@TestInstallIn` module (`FakeStockRepositoryModule`, replacing the production
`StockRepositoryModule`), so they exercise the presentation and Paging/adapter layers
deterministically without depending on Room or the real TWSE network call — Room itself remains
covered separately by `StockDaoTest` and `StockDatabaseMigrationTest`.

**UI Formatting Rules** — `StockUiModelMapperTest` covers price-position classification (above/below
monthly average), change-direction classification (positive/negative), null-value placeholders,
thousands-separator formatting, and the `+`/`-` sign on the change value.

**Market Summary Mapping** — `MarketSummaryMapperTest` verifies the trivial field mapping from the
domain `MarketChangeSummary` to the presentation `MarketSummary`. The advancing/declining/unchanged
*counting* logic itself moved into a Room SQL aggregate (`StockDao.observeMarketSummary`); dedicated
coverage for that aggregate — including how a `NULL` `change` column is bucketed — lives in
`StockDaoTest` (see the **Room DAO** entry above).

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
selectingCurrentSortDirection_isANoOp
stocksPagingData_reflectsRepositoryStocksForTheDefaultSortDirection
observeStocksPaged_mapsEntitiesToDomainStocks
observeMarketSummary_mapsTheAggregateRowToADomainSummary
clickingFirstStockCard_opensDetailDialogWithMatchingStock
marketSummaryBar_displaysMarketCounts
sortingViaEspresso_reordersRecyclerView_whileComposeSummaryStaysCorrect
```

---

## Continuous Integration

Every push to `main` and every pull request triggers a GitHub Actions workflow
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) with three parallel jobs:

```text
static-analysis:  checkout → setup JDK 17 → setup Gradle → ktlintCheck → detekt
test-build:       checkout → setup JDK 17 → setup Gradle → test →
                  createDebugUnitTestCoverageReport → upload to Codecov → lint → assembleDebug →
                  assembleDebugAndroidTest
secret-scan:      checkout (full history) → gitleaks
```

`static-analysis`, `test-build`, and `secret-scan` run as independent, parallel jobs — a failure in
one does not block or gate the others. `ktlintCheck` and `detekt` run across all modules and fail
the `static-analysis` job on any enabled rule violation — no detekt baseline is used, so every
finding must be resolved or narrowly suppressed with an explicit rationale. Compose `@Composable`
functions use annotation-scoped naming exceptions, while the existing underscore-based base package
retains an explicit package-naming exemption.

`secret-scan` runs [gitleaks](https://github.com/gitleaks/gitleaks) against the full commit history
(`fetch-depth: 0` — a shallow checkout would only expose the single triggering commit) as a
CI-level backstop independent of the local `pre-commit`/`scripts/secret-scan.sh` checks: those only
run if a contributor has installed the hooks (`./gradlew installGitHooks`) or remembers to run the
manual script, and either can be bypassed with `git commit --no-verify`. `secret-scan` catches what
gets through regardless of local setup.

ktlint reports (plain text + Checkstyle XML), detekt reports (HTML + SARIF), test reports, and
Android Lint reports are uploaded as workflow artifacts (7-day retention) whenever the corresponding
task produces them, regardless of whether the job passes or fails, so a failure can usually be
diagnosed directly from the Actions run without reproducing it locally.

Coverage upload to Codecov uses `fail_ci_if_error: false` — a Codecov outage or misconfigured
token degrades the coverage report/badge but never fails `test-build` itself, since coverage
reporting is treated as observability rather than a merge gate at this stage.

Instrumented tests (`connectedDebugAndroidTest`) are not *run* in this workflow, since Android
emulators in CI add meaningful setup and boot-time complexity and are planned as a separate workflow
rather than blocking every push. `test-build` does compile them (`assembleDebugAndroidTest`),
though — the `androidTest` source set (e.g. `StockDaoTest`, `StockListEspressoTest`,
`MarketSummaryBarTest`, `StockListMixedComposeEspressoTest`) isn't touched by `test` (JVM-only),
`lint`, or `assembleDebug`, so without this step a production API change could silently break an
instrumentation test with no CI job noticing until someone happens to run it against a real
device.

---

## Git Workflow

The repository is developed through small, independently reviewable Pull Requests. Each
architectural concern is introduced in its own focused change, such as: project foundation, network
infrastructure, TWSE aggregation, Room persistence, dependency injection, presentation state, UI,
UI testing, and quality tooling.

Dependencies are introduced when first needed rather than being added up front.

The repository also contains a [Pull Request template](.github/pull_request_template.md) covering
Summary, Changes, Architecture, Verification, Screenshots, Notes, and Related work.
A [GitHub Actions workflow](.github/workflows/ci.yml) runs tests, lint, and a debug build on every
push and pull request against `main`.

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
git clone https://github.com/tenSunFree/taiwan-stock-lab-android.git
cd taiwan-stock-lab-android
```

### Local Git Hooks

This repository tracks Git hooks under `scripts/hooks/`. Configure Git to use them once per clone:

```bash
./gradlew installGitHooks
```

This sets `core.hooksPath` to `scripts/hooks` (verify with `git config --get core.hooksPath`),
rather than copying files into `.git/hooks/` — `.git/hooks/` isn't version-controlled and would
drift out of sync with the tracked source whenever a hook is edited.

| Hook | Runs on | What it checks |
|---|---|---|
| `pre-commit` | every `git commit` | Repository-specific forbidden-file check (`google-services.json`, keystores, `local.properties`, etc. — runs regardless of whether `gitleaks` is installed); secret scan on staged changes (`gitleaks`, or a built-in regex fallback otherwise); `ktlintCheck` on staged Kotlin/Kotlin-DSL files only |
| `commit-msg` | every `git commit` | Commit message follows [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `test:`, `chore:`, etc.) |
| `pre-push` | every `git push` | `ktlintCheck`, `detekt`, `test`, `lint`, `assembleDebug`, and `assembleDebugAndroidTest` — the same non-emulator checks CI runs, so a push that would fail CI fails locally first |

Neither hook modifies the working tree or auto-stages anything — if `pre-commit` fails, fix the
issue explicitly (e.g. `./gradlew ktlintFormat`) and re-`git add` before committing again.
`connectedDebugAndroidTest` (requires an emulator or device) is intentionally not part of any
hook and stays a manual step.

For a full two-pass scan of both git history and the current working tree (recommended before
opening a PR or cutting a release, not on every commit — it's slower than the staged-only
`pre-commit` scan, and requires `gitleaks` to be installed):

```bash
bash scripts/secret-scan.sh
```

Git hooks can be bypassed with `--no-verify` (e.g. `git commit --no-verify`), but this should be
reserved for exceptional cases.

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

Generate JVM unit-test coverage for `:feature:stocklist` (HTML report at
`feature/stocklist/build/reports/coverage/test/debug/index.html`):

```bash
./gradlew :feature:stocklist:createDebugUnitTestCoverageReport
```

Run Room instrumentation tests, including the schema migration test and the Compose UI test for
`MarketSummaryBar` (requires a running emulator or physical device):

```bash
./gradlew :feature:stocklist:connectedDebugAndroidTest
```

Run Hilt dependency-graph instrumentation tests, the Espresso XML screen test, and the mixed
Espresso + Compose UI test (requires a running emulator or physical device):

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
│               ├── StockRepositoryInjectionTest.kt
│               ├── StockListEspressoTest.kt
│               ├── StockListMixedComposeEspressoTest.kt
│               ├── di/
│               │   └── FakeStockRepositoryModule.kt
│               ├── fake/
│               │   └── FakeStockRepository.kt
│               └── util/
│                   ├── EspressoSync.kt
│                   └── RecyclerViewMatcher.kt
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
│           │   │       │   └── MarketSummaryBar.kt
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
│               └── kotlin/.../
│                   ├── data/local/
│                   │   ├── StockDaoTest.kt
│                   │   └── StockDatabaseMigrationTest.kt
│                   └── presentation/compose/
│                       └── MarketSummaryBarTest.kt
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
├── codecov.yml
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
- Paging 3 with a Room `PagingSource`, keeping list rendering, aggregate counts, and single-row
  lookups on separate, appropriately-scoped queries instead of one full-list read for everything
- database schema evolution with migration testing
- XML/Jetpack Compose interoperability
- Espresso and Compose UI Test coverage across XML views, Compose components, and their
  interoperability within the same screen, backed by deterministic fakes injected via Hilt
- JVM unit-test coverage reporting via JaCoCo/Codecov, scoped honestly to what CI actually executes
- repository-wide code-style enforcement
- static-analysis rule governance
- zero-baseline static analysis
- continuous integration
- automated testing
- incremental delivery through reviewable Pull Requests
- local quality gates (Git hooks) that mirror CI checks before code ever reaches a push

---

## License

This repository is currently intended for learning, technical assessment, and demonstration
purposes.
