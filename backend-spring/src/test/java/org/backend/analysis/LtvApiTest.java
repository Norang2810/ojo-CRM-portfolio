//package org.backend.analysis;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//
//@SpringBootTest
//@AutoConfigureMockMvc(addFilters = false)
//public class LtvApiTest {
//    // @Autowired
//    // private MockMvc mockMvc;
//    //
//    // @Test
//    // @WithMockUser
//    // @DisplayName("특정 고객 LTV 조회 test")
//    // void getLtvTest() throws Exception{
//    // Long testMemberId = 1L;
//    //
//    // mockMvc.perform(get("/api/analysis/ltv/" + testMemberId))
//    // .andExpect(status().isOk())
//    // .andExpect(jsonPath("$.memberId").exists())
//    // .andExpect(jsonPath("$.ltv").isNumber())
//    // .andExpect(jsonPath("$.lifecycleStage").isString());
//    // }
//    //
//    // @Test
//    // @WithMockUser
//    // @DisplayName("고객 통합 분석 요약 test")
//    // void getAnalysisSummaryTest() throws Exception{
//    // Long testMemberId = 1L;
//    //
//    // mockMvc.perform(get("/api/analysis/summary/" + testMemberId))
//    // .andExpect(status().isOk())
//    // .andExpect(jsonPath("$.rfmType").isString())
//    // .andExpect(jsonPath("$.ltv").exists());
//    // }
//
//    @Test
//    void helloTest() {
//        System.out.println("Hello OJO Project!");
//        Assertions.assertEquals(2, 1 + 1);
//    }
//}