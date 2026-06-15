package com.soulmate;

import com.soulmate.domain.dto.MemoryStatsDTO;
import com.soulmate.service.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MemoryStatsTest {

    @Autowired
    private MemoryService memoryService;

    @Test
    public void testStatsForSpecificUser() {
        System.out.println("====== SPECIFIC USER STATS TEST ======");
        try {
            Long targetUserId = 2062831345381486594L;
            MemoryStatsDTO statsAll = memoryService.getMemoryStats(targetUserId, null);
            System.out.println("Stats for userId " + targetUserId + " (all companions): " + statsAll);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("====== END SPECIFIC USER STATS TEST ======");
    }
}
