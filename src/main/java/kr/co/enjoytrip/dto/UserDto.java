package kr.co.enjoytrip.dto;

public record UserDto(long id, String loginId, String email, String name, String passwordHash,
                      String role, boolean mustChangePassword) {
    public boolean admin() { return "ADMIN".equals(role); }
}
