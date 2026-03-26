package org.backend;

import org.backend.domain.advice.repository.AdviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AdviceRepoTest {
    @Autowired
    private AdviceRepository adviceRepository;
    @Test
    public void test() {
        System.out.println("ADVICE COUNT: " + adviceRepository.count());
        System.out.println("STATS: " + adviceRepository.getHourlyStatistics().size());
    }
}
