# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Lookup Stage** - Join collections with type-safe DSL
    - Simple lookups with equality matching
    - Pipeline-based lookups with filtering and transformations
    - `let` variables for accessing parent document fields
    - `lookupAndMerge` helper for flattening joined data
    - Support for multiple lookups in single pipeline
- **Unwind Stage** - Flatten array fields
    - `preserveNullAndEmptyArrays` option
    - Works seamlessly with lookup results
- **Add Fields Stage** - Add computed fields to documents
    - Support for expressions and calculations
    - Array operations (sum, avg, min, max, size)
    - String concatenation
    - Conditional field creation with when expressions
- **Expression System** - Natural Kotlin operators for calculations
    - Arithmetic operators: `*`, `+`, `-`, `/`
    - Works in group accumulators, addFields, and match
    - Type-safe property references
- **Expr Support in Match** - Complex field comparisons
    - Compare fields to each other
    - Use expressions in match conditions
    - Variable references in lookup pipelines
- **Type Conversion** - `into()` method for result type conversion
    - Convert at any pipeline stage
    - Type-safe with `into<T>()`
    - Support for KClass parameter
- **Field Name Mapping** - Respect `@Field` annotations
    - Automatic handling of custom MongoDB field names
    - Works across all builders (match, group, sort, etc.)

### Changed
- **Group Stage** - Now auto-adds grouped fields to output
    - Grouped fields appear at top level, not just in `_id`
    - Uses `$addFields` stage internally
    - Returns multiple stages for complete functionality
- **Sort Functions** - Changed from infix to regular functions
    - `Product::price.asc()` instead of `Product::price asc`
    - Cleaner syntax without Unit parameter workaround
- **ArrayOperation** - Unified sealed class for group and addFields
    - Shared logic between aggregation stages
    - Consistent API across builders

### Fixed
- Infix function compatibility issues with default parameters
- Public inline functions accessing internal/private members
- Type conversion issues in paginated results

## [0.1.0-SNAPSHOT] - 2026-01-11

### Added
- Initial project structure
- Core `Konduct` entry point class
- `AggregationPipeline` interface with basic operations
- `match` stage with comparison operators (eq, ne, gt, gte, lt, lte)
- `match` stage with set operators (in, nin)
- `match` stage with string operators (regex)
- `match` stage with existence checks (exists, isNull, isNotNull)
- `sort` stage with ascending and descending order
- `skip` and `limit` stages for pagination
- `group` stage with accumulators (sum, avg, min, max, count, etc.)
- `facet` stage for multi-metric queries
- `paginate` helper with PagedResult wrapper
- `customStage` builder for unsupported MongoDB stages
- Terminal operations: `toList()`, `firstOrNull()`, `count()`
- Debug support: `toJson()`, `toAggregation()`
- Type-safe field references using Kotlin property references
- Extension function `MongoTemplate.konduct()`
- Comprehensive integration tests with Testcontainers
- Complete documentation with MkDocs + Dokka

### Documentation
- User guides for all core features
- Real-world examples (e-commerce, analytics, time-series)
- API reference with Dokka
- Quick start guide
- Contributing guidelines

---

## Migration Guides

### From 0.1.0 to Current

**Sort Syntax Changed:**
```kotlin
// Old (won't compile)
.sort { Product::price asc }

// New
.sort { Product::price.asc() }
```

**Lookup Now Requires Type Conversion:**
```kotlin
// Old (returned Order)
.lookup<Customer> { /* ... */ }
.toList()  // List<Order>

// New (use into() to convert)
.lookup<Customer> { /* ... */ }
.into<Document>()  // or .into<OrderWithCustomer>()
.toList()
```

**Group Auto-Adds Fields:**
```kotlin
// Now automatically includes grouped fields
.group {
    by(Product::category)
    accumulate { "count" count Unit }
}
// Result has both _id and category fields
```

---

## Upgrade Instructions

### Update Dependencies
```kotlin
dependencies {
    implementation("io.github.denofbits:konduct:0.2.0-SNAPSHOT")
}
```

### Update Code

1. **Fix sort syntax:**
```bash
   # Find and replace
   Find: (\w+::\w+) asc
   Replace: $1.asc()
   
   Find: (\w+::\w+) desc
   Replace: $1.desc()
```

2. **Add into() after lookup:**
```kotlin
   // Add .into<Document>() or .into<YourType>()
   .lookup<Customer> { /* ... */ }
   .into<Document>()  // ADD THIS
   .toList()
```

3. **Update imports for new features:**
```kotlin
   import io.github.denofbits.konduct.builders.LookupBuilder
   import io.github.denofbits.konduct.builders.AddFieldsBuilder
   import io.github.denofbits.konduct.expressions.*
```

---

## What's Next?

See our [roadmap](https://github.com/DenOfBits/konduct/issues) for upcoming features:

- [ ] Window functions ($setWindowFields)
- [ ] Text search integration
- [ ] Geospatial queries
- [ ] Atlas Search support
- [ ] Performance optimizations
- [ ] More built-in patterns

---

**Questions?** Open an [issue](https://github.com/DenOfBits/konduct/issues) or [discussion](https://github.com/DenOfBits/konduct/discussions)!