/**
 * Các thành phần bảo mật dựa trên JWT.
 *
 * Package này nơi Bearer token được:
 * - parse và verify
 * - biến thành Authentication trong SecurityContext
 * - nối vào UserDetails để Spring Security có current user và role
 */
package org.example.goldenheartrestaurant.common.security;
