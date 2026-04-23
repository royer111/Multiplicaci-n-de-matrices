package ejemplo1;

/**
 * Algoritmo 3: NAIV LOOP UNROLLING FOUR
 * Complejidad: O(n³) — desdoblamiento por 4 para mayor aprovechamiento
 * del pipeline de ejecución superescalar (M2 tiene 8 unidades ALU).
 */
public class NaivLoopUnrollingFour {

    public static long[][] run(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = 0;
                int k = 0;
                for (; k <= n - 4; k += 4)
                    s += B[i][k]     * C[k][j]     + B[i][k + 1] * C[k + 1][j]
                       + B[i][k + 2] * C[k + 2][j] + B[i][k + 3] * C[k + 3][j];
                for (; k < n; k++) s += B[i][k] * C[k][j];
                A[i][j] = s;
            }
        return A;
    }
}
