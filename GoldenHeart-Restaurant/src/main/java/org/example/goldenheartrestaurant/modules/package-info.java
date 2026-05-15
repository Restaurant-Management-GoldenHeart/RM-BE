/**
 * Toàn bộ module nghiệp vụ của hệ thống.
 *
 * Mỗi module được tách theo domain-first:
 * - tự giữ entity, repository, service, controller và dto
 * - hạn chế để business logic tản mạn giữa các package kỹ thuật
 * - dễ đọc theo luồng nghiệp vụ thay vì đọc theo loại file
 */
package org.example.goldenheartrestaurant.modules;
