package kr.co.enjoytrip.dto;

public record AttractionDto(long id, long contentId, String title, int contentType, int areaCode, Integer sigunguCode,
                            String address, String description, Double latitude, Double longitude,
                            String imageUrl, String sourceUpdatedAt) {}
