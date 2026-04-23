package ejemplo1;

/**
 * Algoritmo 2: NAIV LOOP UNROLLING TWO
 * Complejidad: O(n³) — igual que Naiv pero el compilador/CPU aprovecha
 * mejor el pipeline al desdoblar el bucle interno de a 2.
 */
public class NaivLoopUnrollingTwo {

    public static long[][] run(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = 0;
                int k = 0;
                for (; k <= n - 2; k += 2)
                    s += B[i][k] * C[k][j] + B[i][k + 1] * C[k + 1][j];
                if (k < n) s += B[i][k] * C[k][j];
                A[i][j] = s;
            }
        return A;
    }
}
