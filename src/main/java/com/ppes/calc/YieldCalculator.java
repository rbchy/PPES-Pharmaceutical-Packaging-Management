package com.ppes.calc;

/**
 * cGMP batch yield reconciliation — the calculation behind the
 * "Batch Yield Reconciliation Calculator" screen, kept free of Swing so it can be unit-tested.
 *
 * <pre>
 *   I = input quantity          P = packed good yield
 *   S = QC / sampling quantity  R = rejected / destroyed   U = unused returned
 *
 *   Actual output yield  = P / I x 100
 *   Reconciliation yield = (P + S + R + U) / I x 100
 *
 *   99.0 % &lt;= reconciliation &lt;= 101.0 %  -> PASS
 *   below 99.0 %                         -> FAIL, unaccounted loss
 *   above 101.0 %                        -> FAIL, surplus discrepancy
 * </pre>
 */
public final class YieldCalculator {

    public static final double LOWER_LIMIT = 99.0;
    public static final double UPPER_LIMIT = 101.0;

    public enum Status { PASS, UNACCOUNTED_LOSS, SURPLUS_DISCREPANCY, INVALID_INPUT }

    /** Result of one calculation; percentages are 0 when the input is invalid. */
    public static final class Result {
        public final double actualYield;
        public final double reconciliationYield;
        public final Status status;

        Result(double actualYield, double reconciliationYield, Status status) {
            this.actualYield = actualYield;
            this.reconciliationYield = reconciliationYield;
            this.status = status;
        }

        /** Percentage points missing below 100 % (only meaningful for UNACCOUNTED_LOSS). */
        public double unaccountedPercent() {
            return 100.0 - reconciliationYield;
        }
    }

    private YieldCalculator() {
    }

    public static Result calculate(double input, double packed, double sampling, double rejected, double unused) {
        // A count can never be negative, and nothing can be reconciled against a zero input.
        if (!(input > 0) || packed < 0 || sampling < 0 || rejected < 0 || unused < 0
                || Double.isNaN(packed + sampling + rejected + unused)) {
            return new Result(0, 0, Status.INVALID_INPUT);
        }
        double actual = packed / input * 100.0;
        double reconciliation = (packed + sampling + rejected + unused) / input * 100.0;

        Status status;
        if (reconciliation < LOWER_LIMIT) {
            status = Status.UNACCOUNTED_LOSS;
        } else if (reconciliation > UPPER_LIMIT) {
            status = Status.SURPLUS_DISCREPANCY;
        } else {
            status = Status.PASS;
        }
        return new Result(actual, reconciliation, status);
    }
}
