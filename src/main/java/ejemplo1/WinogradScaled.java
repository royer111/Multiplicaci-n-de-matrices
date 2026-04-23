package ejemplo1;

/**
 * Algoritmo 5: WINOGRAD SCALED
 * Complejidad: O(n³) — variante de Winograd con precómputo de factores
 * desdoblado por 2 para mayor rendimiento en el pipeline.
 */
public class WinogradScaled {

    public static long[][] run(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        int half = n >> 1;
        long[] rf = new long[n], cf = new long[n];

        // Factores de fila con unrolling x2
        for (int i = 0; i < n; i++) {
            long r = 0; int k = 0;
            for (; k <= half - 2; k += 2)
                r += B[i][2*k] * B[i][2*k+1] + B[i][2*k+2] * B[i][2*k+3];
            for (; k < half; k++) r += B[i][2*k] * B[i][2*k+1];
            rf[i] = r;
        }

        // Factores de columna con unrolling x2
        for (int j = 0; j < n; j++) {
            long c = 0; int k = 0;
            for (; k <= half - 2; k += 2)
                c += C[2*k][j] * C[2*k+1][j] + C[2*k+2][j] * C[2*k+3][j];
            for (; k < half; k++) c += C[2*k][j] * C[2*k+1][j];
            cf[j] = c;
        }

        // Cómputo principal con unrolling x2
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = -rf[i] - cf[j];
                int k = 0;
                for (; k <= half - 2; k += 2) {
                    s += (B[i][2*k]   + C[2*k+1][j]) * (B[i][2*k+1] + C[2*k][j]);
                    s += (B[i][2*k+2] + C[2*k+3][j]) * (B[i][2*k+3] + C[2*k+2][j]);
                }
                for (; k < half; k++)
                    s += (B[i][2*k] + C[2*k+1][j]) * (B[i][2*k+1] + C[2*k][j]);
                if ((n & 1) == 1) s += B[i][n-1] * C[n-1][j];
                A[i][j] = s;
            }
        return A;
    }
}
