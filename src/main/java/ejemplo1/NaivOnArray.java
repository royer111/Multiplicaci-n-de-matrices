package ejemplo1;

/**
 * Algoritmo 1: NAIV ON ARRAY
 * Complejidad: O(n³) — triple bucle directo sobre arreglos.
 */
public class NaivOnArray {

    public static long[][] run(long[][] B, long[][] C) {
        int n = B.length;
        long[][] A = new long[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++)
                    A[i][j] += B[i][k] * C[k][j];
        return A;
    }
}
