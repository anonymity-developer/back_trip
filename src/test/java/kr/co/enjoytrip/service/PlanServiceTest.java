package kr.co.enjoytrip.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PlanServiceTest {
    private final PlanService plans=new PlanService();

    @Test void rejectsDatesAndBudgetBeforePersisting() {
        assertThrows(ValidationException.class,()->plans.create(1L,"여행","2026-11-02","2026-11-01","0","",false));
        assertThrows(ValidationException.class,()->plans.create(1L,"여행","2026-11-01","2026-11-02","-1","",false));
        assertThrows(ValidationException.class,()->plans.create(1L,"여행","invalid","2026-11-02","0","",false));
    }
}
