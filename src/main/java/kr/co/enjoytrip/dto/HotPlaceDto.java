package kr.co.enjoytrip.dto;

import java.time.LocalDate;

public record HotPlaceDto(long id,long userId,String author,String title,String placeName,String placeType,
                          LocalDate visitedOn,String description,Double latitude,Double longitude,String imagePath) {}
