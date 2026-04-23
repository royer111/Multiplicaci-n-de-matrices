package ejemplo1;

/**
 * Algoritmo 4: WINOGRAD ORIGINAL
 * Complejidad: O(n³) pero con ~n³/2 multiplicaciones (la mitad).
 * Precalcula factores de fila y columna para reducir multiplicaciones
 * en el bucle principal.
 */
public class WinogradOriginal {

    public static long[][] run(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        int half = n >> 1;
        long[] rf = new long[n], cf = new long[n];

        for (int i = 0; i < n; i++)
            for (int k = 0; k < half; k++)
                rf[i] += B[i][2 * k] * B[i][2 * k + 1];

        for (int j = 0; j < n; j++)
            for (int k = 0; k < half; k++)
                cf[j] += C[2 * k][j] * C[2 * k + 1][j];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                long s = -rf[i] - cf[j];
                for (int k = 0; k < half; k++)
                    s += (B[i][2*k] + C[2*k+1][j]) * (B[i][2*k+1] + C[2*k][j]);
                if ((n & 1) == 1) s += B[i][n - 1] * C[n - 1][j];
                A[i][j] = s;
            }
        return A;
    }
}
