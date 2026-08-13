# Horizon API - AI Agents Documentation

## Overview
This document describes the AI agents and their roles in the Horizon API project.

## Project Architecture
This project follows **Hexagonal Architecture** (Ports and Adapters pattern) to ensure separation of concerns and maintainability.

### Architecture Layers

#### 1. Domain Layer
- **Package**: `br.com.horizon.horizon_api.domain`
- **Purpose**: Core business logic and entities
- **Components**:
  - Entities: Core business objects
  - Value Objects: Immutable values
  - Repository Interfaces: Contracts for data access
  - Domain Services: Business rules

#### 2. Application Layer
- **Package**: `br.com.horizon.horizon_api.application`
- **Purpose**: Use cases and application orchestration
- **Components**:
  - Use Cases: Application business logic
  - DTOs: Data transfer objects
  - Mappers: Object transformation
  - Application Services: Orchestration layer

#### 3. Infrastructure Layer
- **Package**: `br.com.horizon.horizon_api.infrastructure`
- **Purpose**: External implementations and technical details
- **Components**:
  - Persistence: JPA repositories, database entities
  - External Services: Email, Redis, etc.
  - Adapters: Implementation of domain ports

#### 4. Interface Layer
- **Package**: `br.com.horizon.horizon_api.interfaces`
- **Purpose**: External communication interfaces
- **Components**:
  - REST Controllers: API endpoints
  - Request/Response Models: API contracts
  - Configuration: Spring configuration classes

## Technology Stack
- **Java**: 25
- **Spring Boot**: 4.1.0
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis
- **Security**: Spring Security
- **Build Tool**: Maven
- **Testing**: Spring Boot Test

## Development Guidelines

### Code Organization
- Keep domain layer independent of frameworks
- Use interfaces for repositories in domain layer
- Implement repositories in infrastructure layer
- Controllers should only handle HTTP concerns
- Business logic belongs to application layer

### Naming Conventions
- Entities: `{Name}Entity` (infrastructure) / `{Name}` (domain)
- Repositories: `{Name}Repository` (interface) / `{Name}RepositoryImpl` (implementation)
- Use Cases: `{Name}UseCase` or `{Action}{Name}UseCase`
- Controllers: `{Name}Controller`
- DTOs: `{Name}DTO` / `{Name}Request` / `{Name}Response`

### Dependencies Direction
- Interface → Application → Domain
- Infrastructure → Domain
- Infrastructure → Application (for implementations)

## Project Structure
```
horizon-api/
├── src/main/java/br/com/horizon/horizon_api/
│   ├── HorizonApiApplication.java
│   ├── domain/
│   │   ├── entity/
│   │   ├── valueobject/
│   │   ├── repository/
│   │   └── service/
│   ├── application/
│   │   ├── usecase/
│   │   ├── dto/
│   │   ├── mapper/
│   │   └── service/
│   ├── infrastructure/
│   │   ├── persistence/
│   │   ├── messaging/
│   │   └── config/
│   └── interfaces/
│       ├── rest/
│       └── config/
└── src/main/resources/
    ├── application.yml
    └── db/migration/
```

## AI Agent Roles

### Cascade (Primary Agent)
- **Role**: Full-stack development and architecture
- **Responsibilities**: 
  - Implement hexagonal architecture
  - Create domain models and use cases
  - Implement REST APIs
  - Ensure code quality and best practices

### Future Agents
To be defined as project requirements evolve.

## Notes
- This is a Spring Boot 4.1.0 project with Java 25
- Uses PostgreSQL for persistence
- Redis for caching
- Follows clean architecture principles
- Domain-driven design (DDD) concepts applied where appropriate
