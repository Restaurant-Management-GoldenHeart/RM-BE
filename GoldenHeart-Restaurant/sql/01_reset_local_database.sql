/*
  Reset local database to a clean state for deterministic Postman testing.

  Recommended sequence:
  1. Keep app.bootstrap.sample-data.enabled=false
  2. Run this script
  3. Start Spring Boot once so Hibernate creates schema + bootstrap the minimal auth/unit data
  4. Run 02_seed_reference_data.sql
*/

DROP DATABASE IF EXISTS goldenheart_restaurant;

CREATE DATABASE goldenheart_restaurant
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE goldenheart_restaurant;
