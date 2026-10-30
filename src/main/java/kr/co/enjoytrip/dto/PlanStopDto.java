package kr.co.enjoytrip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanStopDto(long id,long planId,long attractionId,String attractionTitle,LocalDate visitDate,int order,
                          BigDecimal budget,String memo,Double latitude,Double longitude) {}
