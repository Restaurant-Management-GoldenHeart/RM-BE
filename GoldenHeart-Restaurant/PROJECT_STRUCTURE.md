# GoldenHeart Restaurant Backend Structure

## Basis

This structure is based on:

- your local [jvSpringBootSkill.md](D:/CDIO/RM-GoldenHeart/RM-BE/GoldenHeart-Restaurant/jvSpringBootSkill.md)
- the flow learned from [SPM-Health-Care/SPM-BE](https://github.com/SPM-Health-Care/SPM-BE)

The good parts to keep from that project are:

- clear controller -> service -> repository flow
- request/response DTOs for API boundaries
- global exception handling
- security/config separated from business code

The parts to avoid copying as-is are:

- one giant top-level `controller`, `service`, `repository`, `dto` package
- `service.imp`
- `IUserService`, `IDailyMealRepository` style naming everywhere
- putting unrelated features in the same DTO folder
- exposing entities directly to controllers

## Recommended Package Structure

```text
org.example.goldenheartrestaurant
|-- common
|   |-- config
|   |-- exception
|   |-- response
|   |-- security
|   |-- util
|   `-- validation
|-- modules
|   |-- auth
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   `-- service
|   |-- identity
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   |-- entity
|   |   |-- mapper
|   |   |-- repository
|   |   `-- service
|   |-- restaurant
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   |-- entity
|   |   |-- repository
|   |   `-- service
|   |-- customer
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   |-- entity
|   |   |-- repository
|   |   `-- service
|   |-- menu
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   |-- entity
|   |   |-- mapper
|   |   |-- repository
|   |   `-- service
|   |-- inventory
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   |-- entity
|   |   |-- repository
|   |   `-- service
|   |-- order
|   |   |-- controller
|   |   |-- dto
|   |   |   |-- request
|   |   |   `-- response
|   |   |-- entity
|   |   |-- repository
|   |   `-- service
|   `-- billing
|       |-- controller
|       |-- dto
|       |   |-- request
|       |   `-- response
|       |-- entity
|       |-- repository
|       `-- service
|-- entity
|   `-- temporary-legacy
`-- GoldenHeartRestaurantApplication.java
```

## Module Boundaries

- `auth`: login, refresh token, logout, current-user auth flow
- `identity`: `User`, `Role`, later `Permission` if you add RBAC
- `restaurant`: `Restaurant`, `Branch`, `RestaurantTable`
- `customer`: customer profile and loyalty
- `menu`: `Category`, `MenuItem`, `Recipe`
- `inventory`: `Ingredient`, `Inventory`
- `order`: `Order`, `OrderItem`
- `billing`: `Bill`, `Payment`

## Concrete Mapping From Current Entities

- `Role`, `User`, `UserProfile`, `UserStatus` -> `modules.identity.entity`
- `Restaurant`, `Branch`, `RestaurantTable`, `RestaurantTableStatus` -> `modules.restaurant.entity`
- `Customer` -> `modules.customer.entity`
- `Category`, `MenuItem`, `Recipe`, `MenuItemStatus` -> `modules.menu.entity`
- `Ingredient`, `Inventory` -> `modules.inventory.entity`
- `Order`, `OrderItem`, `OrderStatus`, `OrderItemStatus` -> `modules.order.entity`
- `Bill`, `Payment`, `BillStatus`, `PaymentMethod` -> `modules.billing.entity`

## What To Implement Next

1. Move shared classes into `common`
2. Add `ApiResponse` and `GlobalExceptionHandler`
3. Create request/response DTOs per module
4. Add repositories under each module
5. Add services with constructor injection only
6. Refactor entities from the current flat `entity` package into module packages in small batches

## Naming Rules

- use `UserService`, not `IUserService`
- use `UserRepository`, not `IUserRepository`
- if an interface is truly needed, name the implementation `UserServiceImpl`
- keep DTOs as `CreateOrderRequest`, `OrderResponse`, `UpdateMenuItemRequest`
- keep controllers thin and return DTOs, not entities
