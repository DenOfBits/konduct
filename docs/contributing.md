# Contributing to Konduct

Thank you for your interest in contributing to Konduct!

## Development Setup

1. **Fork and clone**
```bash
   git clone https://github.com/YOUR_USERNAME/konduct.git
   cd konduct
```

2. **Build**
```bash
   ./gradlew build
```

3. **Run tests**
```bash
   ./gradlew test
```

## Documentation

### Building Documentation

**Generate API docs (Dokka):**
```bash
./gradlew dokkaHtml
```

**Serve MkDocs locally:**
```bash
mkdocs serve
```

Then open http://127.0.0.1:8000

**Build static site:**
```bash
mkdocs build
```

### Documentation Structure

- **MkDocs** (`docs/`) - User guides, tutorials, examples
- **Dokka** (`docs/api/`) - Generated API reference

### Writing Documentation

1. **User Guides** - Add `.md` files to `docs/guide/`
2. **Examples** - Add to `docs/examples/`
3. **API Docs** - Use KDoc comments in source code

Example KDoc:
```kotlin
/**
 * Filters documents using the $match stage.
 *
 * @param block DSL block for building match conditions
 * @return A new pipeline with the match stage added
 *
 * Example:
 * ```kotlin
 * .match {
 *     Product::status eq "active"
 *     Product::price gte 100
 * }
 * ```
*/
fun match(block: MatchBuilder<T>.() -> Unit): AggregationPipeline<T>
```

## Pull Request Process

1. Create a feature branch
2. Make your changes
3. Add tests
4. Update documentation
5. Run `./gradlew build`
6. Submit PR

## Questions?

Open an issue or start a discussion on GitHub!