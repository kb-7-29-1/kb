package com.salgosipo.safety.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafetyScoreCalculatorTest {

    private final SafetyScoreCalculator calculator = new SafetyScoreCalculator();

    @Test
    void cctvDensityPenaltyUsesConfiguredBoundaries() {
        assertEquals(0, calculator.calculateCctvDensityPenalty(50.0));
        assertEquals(5, calculator.calculateCctvDensityPenalty(75.0));
        assertEquals(10, calculator.calculateCctvDensityPenalty(100.0));
        assertEquals(15, calculator.calculateCctvDensityPenalty(150.0));
        assertEquals(20, calculator.calculateCctvDensityPenalty(150.1));
    }

    @Test
    void cctvCoveragePenaltyUsesConfiguredBoundaries() {
        assertEquals(0, calculator.calculateCctvCoveragePenalty(80.0));
        assertEquals(4, calculator.calculateCctvCoveragePenalty(60.0));
        assertEquals(8, calculator.calculateCctvCoveragePenalty(40.0));
        assertEquals(12, calculator.calculateCctvCoveragePenalty(20.0));
        assertEquals(15, calculator.calculateCctvCoveragePenalty(19.9));
    }

    @Test
    void streetLightCoveragePenaltyUsesConfiguredBoundaries() {
        assertEquals(0, calculator.calculateStreetLightCoveragePenalty(80.0));
        assertEquals(10, calculator.calculateStreetLightCoveragePenalty(60.0));
        assertEquals(20, calculator.calculateStreetLightCoveragePenalty(40.0));
        assertEquals(30, calculator.calculateStreetLightCoveragePenalty(20.0));
        assertEquals(45, calculator.calculateStreetLightCoveragePenalty(19.9));
    }
}
