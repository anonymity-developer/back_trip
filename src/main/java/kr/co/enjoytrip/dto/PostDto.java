package kr.co.enjoytrip.dto;

public record PostDto(long id,long userId,String author,String kind,String title,String body,String createdAt,String updatedAt) {}
