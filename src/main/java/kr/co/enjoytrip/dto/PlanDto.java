package kr.co.enjoytrip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlanDto(long id,long userId,String author,String title,LocalDate startDate,LocalDate endDate,
                      BigDecimal budget,String details,boolean isPublic) {}
