# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
- Terminal operations: `toList()`, `firstOrNull()`, `count()`
- Debug support: `toJson()`, `toAggregation()`
- Type-safe field references using Kotlin property references
- Extension function `MongoTemplate.konduct()`
- Comprehensive integration tests with Testcontainers
- Documentation: README, LICENSE, CONTRIBUTING

### Changed
- Nothing yet

### Deprecated
- Nothing yet

### Removed
- Nothing yet

### Fixed
- Nothing yet

### Security
- Nothing yet

## [0.1.0-SNAPSHOT] - 2026-01-11

Initial development release with basic aggregation pipeline functionality.
