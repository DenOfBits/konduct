# Contributing to Konduct

First off, thank you for considering contributing to Konduct! It's people like you that make Konduct such a great tool.

## Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

* **Use a clear and descriptive title**
* **Describe the exact steps which reproduce the problem**
* **Provide specific examples to demonstrate the steps**
* **Describe the behavior you observed after following the steps**
* **Explain which behavior you expected to see instead and why**
* **Include code snippets and stack traces if applicable**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

* **Use a clear and descriptive title**
* **Provide a step-by-step description of the suggested enhancement**
* **Provide specific examples to demonstrate the steps**
* **Describe the current behavior and explain which behavior you expected to see instead**
* **Explain why this enhancement would be useful**

### Pull Requests

* Fill in the required template
* Follow the Kotlin coding style
* Include tests for new functionality
* Update documentation as needed
* End all files with a newline

## Development Setup

1. Fork and clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/konduct.git
cd konduct
```

2. Build the project
```bash
./gradlew build
```

3. Run tests
```bash
./gradlew test
```

## Coding Style

* Follow Kotlin coding conventions
* Use meaningful variable and function names
* Add KDoc comments for public APIs
* Keep functions small and focused
* Write tests for new functionality

## Testing

* Write unit tests for new functionality
* Write integration tests for complex features
* Ensure all tests pass before submitting PR
* Aim for high test coverage

## Documentation

* Update README.md if adding new features
* Add KDoc comments to public APIs
* Include code examples in documentation
* Update CHANGELOG.md

## Commit Messages

* Use present tense ("Add feature" not "Added feature")
* Use imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit first line to 72 characters
* Reference issues and pull requests after the first line

Example:
```
Add support for $group stage

- Implement GroupBuilder with accumulator operations
- Add support for time-based grouping
- Include comprehensive tests

Closes #42
```

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
