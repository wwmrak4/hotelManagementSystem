# Hotel Management System

## Purpose

The purpose of this application is to demonstrate my typical coding style, testing approach, and architectural decisions.

## Application Overview

The application currently exposes a single endpoint:

### `GET /availability`

Returns available rooms and pricing information for a specified hotel and check-in/check-out dates.

The availability response is composed by aggregating data from the following external services:

- Customer Profile API
- Hotel Inventory API
- Hotel Pricing API

### Planned Features

- `POST /booking` endpoint for room reservations (work in progress)

## Testing Approach

### Unit and Integration Tests

The unit tests follow a behavior-driven style and focus on verifying behaviour rather than implementation details.

Key principles:

- Test only the public API of a component.
- Prefer behavioral testing over implementation-focused testing.
- Use interaction verification where appropriate (`verifyInteractions`).
- Follow behavioural naming conventions using `should...` and `when...`.
- Extract common setup and assertions into helper methods when it improves maintainability without significantly reducing readability.
- Use parameterized tests for similar scenarios where appropriate
