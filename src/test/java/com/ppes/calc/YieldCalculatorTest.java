package com.ppes.calc;

import com.ppes.calc.YieldCalculator.Result;
import com.ppes.calc.YieldCalculator.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/** cGMP batch reconciliation: 99.0 % to 101.0 % of the input must be accounted for. */
class YieldCalculatorTest {

    @Test
    void aTypicalBatchPasses() {
        // 50,000 tablets in: 48,900 packed, 300 QC samples, 500 rejected, 250 returned unused
        Result r = YieldCalculator.calculate(50000, 48900, 300, 500, 250);
        assertEquals(97.8, r.actualYield, 1e-9);
        assertEquals(99.9, r.reconciliationYield, 1e-9);
        assertEquals(Status.PASS, r.status);
    }

    @ParameterizedTest(name = "input {0}, accounted {1} -> {2}")
    @CsvSource({
            "50000, 49500, PASS",                 // exactly 99.0 %
            "50000, 50500, PASS",                 // exactly 101.0 %
            "50000, 50000, PASS",
            "50000, 49499, UNACCOUNTED_LOSS",     // 98.998 %
            "50000, 50501, SURPLUS_DISCREPANCY",  // 101.002 %
            "3000,  2970,  PASS",
            "3000,  3030,  PASS",
            "70000, 69300, PASS",
            "70000, 70700, PASS"
    })
    void theLimitsAreInclusive(double input, double accounted, Status expected) {
        assertEquals(expected, YieldCalculator.calculate(input, accounted, 0, 0, 0).status);
    }

    @Test
    void aLossReportsHowMuchIsUnaccounted() {
        Result r = YieldCalculator.calculate(10000, 9500, 100, 100, 0);
        assertEquals(Status.UNACCOUNTED_LOSS, r.status);
        assertEquals(97.0, r.reconciliationYield, 1e-9);
        assertEquals(3.0, r.unaccountedPercent(), 1e-9);
    }

    @Test
    void moreAccountedThanIssuedIsASurplus() {
        Result r = YieldCalculator.calculate(10000, 10000, 200, 0, 0);
        assertEquals(Status.SURPLUS_DISCREPANCY, r.status);
        assertEquals(102.0, r.reconciliationYield, 1e-9);
    }

    @Test
    void zeroOrNegativeInputCannotBeReconciled() {
        assertEquals(Status.INVALID_INPUT, YieldCalculator.calculate(0, 0, 0, 0, 0).status);
        assertEquals(Status.INVALID_INPUT, YieldCalculator.calculate(-100, 99, 0, 0, 0).status);
        assertEquals(Status.INVALID_INPUT, YieldCalculator.calculate(Double.NaN, 99, 0, 0, 0).status);
    }

    @Test
    void aNegativeCountIsInvalidEvenIfTheTotalWouldPass() {
        // Before the fix, -500 rejected could "cancel out" a 500-tablet surplus and show PASS.
        Result r = YieldCalculator.calculate(50000, 50000, 0, -500, 500);
        assertEquals(Status.INVALID_INPUT, r.status);
        assertEquals(Status.INVALID_INPUT, YieldCalculator.calculate(100, -1, 0, 0, 101).status);
        assertEquals(Status.INVALID_INPUT, YieldCalculator.calculate(100, 99, Double.NaN, 0, 0).status);
    }
}
