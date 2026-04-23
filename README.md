# Multiplicación de Matrices Grandes


Implementación y comparación de **15 algoritmos de multiplicación de matrices** en Java, diseñados y optimizados para arquitectura **Mac M2 (ARM64 / Apple Silicon)**.

---

## Estructura del proyecto

```
Src/main
└── java/
    └── ejemplo1/
        ├── MatrizHelper.java               # Utilidades compartidas (add, sub, pad, getSub…)
        ├── NaivOnArray.java                # Algoritmo 1
        ├── NaivLoopUnrollingTwo.java       # Algoritmo 2
        ├── NaivLoopUnrollingFour.java      # Algoritmo 3
        ├── WinogradOriginal.java           # Algoritmo 4
        ├── WinogradScaled.java             # Algoritmo 5
        ├── StrassenNaiv.java               # Algoritmo 6
        ├── StrassenWinograd.java           # Algoritmo 7
        ├── SeqBlockRC.java                 # Algoritmo 8
        ├── ParallelBlockRC.java            # Algoritmo 9
        ├── EnhancedParallelBlockRC.java    # Algoritmo 10
        ├── SeqBlockRR.java                 # Algoritmo 11
        ├── ParallelBlockRR.java            # Algoritmo 12
        ├── EnhancedParallelBlockRR.java    # Algoritmo 13
        ├── SeqBlockCC.java                 # Algoritmo 14
        ├── ParallelBlockCC.java            # Algoritmo 15
        └── Main.java                       # Orquestador principal
```

---

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| Java JDK | 11 o superior |
| Memoria RAM | ≥ 16 GB recomendado para n ≥ 2048 |
| Arquitectura | Mac M2 (ARM64) — también compatible con x86_64 |

---

## Compilación y ejecución

### Compilar
Desde la raíz del proyecto (donde se encuentra la carpeta `com/`):

```bash
javac com/ejemplo/*.java
```

### Ejecutar
```bash
java com.ejemplo.Main [n1] [n2]
```

| Parámetro | Descripción | Por defecto |
|---|---|---|
| `n1` | Tamaño de la primera matriz cuadrada (n×n) | `4096` |
| `n2` | Tamaño de la segunda matriz cuadrada (n×n) | `n1 * 2` |

### Ejemplos

```bash
# Caso pequeño para pruebas rápidas
java com.ejemplo.Main 256 512

# Caso mediano
java com.ejemplo.Main 512 1024

# Caso completo (requiere memoria suficiente)
java com.ejemplo.Main 2048 4096
```

> **Tip:** Para tamaños grandes se recomienda aumentar el heap de la JVM:
> ```bash
> java -Xmx12g com.ejemplo.Main 2048 4096
> ```

---

## Salidas generadas

Al ejecutarse, el programa crea la carpeta `resultados/` con los siguientes archivos:

| Archivo | Contenido |
|---|---|
| `caso1_B_n<n>.csv` | Matriz B del caso 1 (persistida para reutilización) |
| `caso1_C_n<n>.csv` | Matriz C del caso 1 (persistida para reutilización) |
| `caso2_B_n<n>.csv` | Matriz B del caso 2 |
| `caso2_C_n<n>.csv` | Matriz C del caso 2 |
| `tiempos_ejecucion.csv` | Tiempos de todos los algoritmos en ambos casos |

### Formato de `tiempos_ejecucion.csv`

```
Caso,n,Algoritmo,Tiempo_ms
Caso1,256,1-NaivOnArray,142
Caso1,256,2-NaivLoopUnrollingTwo,138
...
```

---

## Descripción de los algoritmos

### Grupo 1 — Naiv (O(n³))

| # | Clase | Descripción |
|---|---|---|
| 1 | `NaivOnArray` | Triple bucle directo. Referencia base. |
| 2 | `NaivLoopUnrollingTwo` | Desdoblamiento del bucle interno por 2. Mejor aprovechamiento del pipeline. |
| 3 | `NaivLoopUnrollingFour` | Desdoblamiento por 4. Explota las 8 unidades ALU del M2. |

### Grupo 2 — Winograd (O(n³), ~n³/2 multiplicaciones)

| # | Clase | Descripción |
|---|---|---|
| 4 | `WinogradOriginal` | Precalcula factores de fila y columna para reducir multiplicaciones a la mitad. |
| 5 | `WinogradScaled` | Variante de Winograd con unrolling ×2 en el precómputo y el bucle principal. |

### Grupo 3 — Strassen (O(n^2.807))

| # | Clase | Descripción |
|---|---|---|
| 6 | `StrassenNaiv` | Divide y vencerás con 7 productos recursivos. Caso base: `NaivOnArray`. |
| 7 | `StrassenWinograd` | Igual que StrassenNaiv pero el caso base es `WinogradOriginal`. |

> Umbral de recursión: **64×64** (constante `STRASSEN_THRESHOLD`). Las matrices se rellenan a la potencia de 2 más cercana.

### Grupo 4 — Bloques secuenciales y paralelos Row×Column (O(n³))

| # | Clase | Descripción |
|---|---|---|
| 8  | `SeqBlockRC`              | Bloques secuenciales. Mejora la localidad de caché. |
| 9  | `ParallelBlockRC`         | Paraleliza sobre bloques de filas con `IntStream.parallel()`. |
| 10 | `EnhancedParallelBlockRC` | 2 hilos explícitos sobre mitades del rango de filas. |

### Grupo 5 — Bloques secuenciales y paralelos Row×Row (O(n³))

| # | Clase | Descripción |
|---|---|---|
| 11 | `SeqBlockRR`              | Acceso fila×fila: `A[i][k] += B[i][j] * C[j][k]`. Mejor localidad en B. |
| 12 | `ParallelBlockRR`         | Versión paralela con `IntStream.parallel()`. |
| 13 | `EnhancedParallelBlockRR` | 2 hilos explícitos sobre mitades del rango de filas. |

### Grupo 6 — Bloques secuenciales y paralelos Column×Column (O(n³))

| # | Clase | Descripción |
|---|---|---|
| 14 | `SeqBlockCC`              | Acceso columna×columna: `A[k][i] += B[k][j] * C[j][i]`. Peor localidad de caché. |
| 15 | `ParallelBlockCC`         | Paraleliza sobre bloques de columna. Distintos hilos acceden a columnas distintas de A (sin condición de carrera). |

---

## Parámetros de configuración

Los siguientes valores pueden ajustarse directamente en cada clase antes de compilar:

| Constante | Valor por defecto | Ubicación | Efecto |
|---|---|---|---|
| `BLOCK_SIZE` | `64` | Clases de bloques (8–15) | Tamaño del bloque para caché (óptimo para L1=128KB del M2) |
| `STRASSEN_THRESHOLD` | `64` | `StrassenNaiv`, `StrassenWinograd` | Tamaño mínimo de submatriz antes de usar caso base |

---

## Clase auxiliar: MatrizHelper

Provee los métodos de bajo nivel reutilizados por los algoritmos de Strassen:

| Método | Descripción |
|---|---|
| `addM(A, B)` | Suma elemento a elemento |
| `subM(A, B)` | Resta elemento a elemento |
| `getSub(M, r, c, sz)` | Extrae una submatriz de tamaño `sz×sz` |
| `setSub(M, S, r, c)` | Inserta una submatriz en la posición `(r, c)` |
| `pad(M, p)` | Rellena la matriz a tamaño `p×p` con ceros |
| `unpad(M, n)` | Recorta la matriz a `n×n` |
| `nextP2(n)` | Devuelve la siguiente potencia de 2 ≥ n |

---

## Notas de diseño

- **Matrices de entradas persistidas:** si los archivos CSV ya existen en `resultados/`, el programa los carga en lugar de regenerarlos, garantizando la reproducibilidad de las mediciones.
- **Valores de 6 dígitos:** todas las matrices se generan con valores entre 100 000 y 999 999 para reflejar condiciones realistas de carga numérica.
- **Tipo `long`:** se usa `long` en lugar de `int` o `double` para evitar desbordamientos en matrices grandes con multiplicaciones acumuladas.
- **Medición justa:** antes de cada algoritmo se invoca `System.gc()` para minimizar el impacto del recolector de basura en los tiempos registrados.
- **Sin condiciones de carrera:** en los algoritmos paralelos de bloques, cada hilo escribe en filas o columnas disjuntas de la matriz resultado, por lo que no se requiere sincronización.

---

## Ejemplo de salida en consola

```
=== Multiplicación de Matrices Grandes ===
Arquitectura: Mac M2 (ARM64 / Apple Silicon)
Casos: n1=256  n2=512
Directorio de salida: resultados/

=== CASO 1  (n=256) ===
  Generando matrices aleatorias (6 dígitos mínimo)...
  Matrices guardadas en disco.

  1-NaivOnArray...                          142 ms
  2-NaivLoopUnrollingTwo...                 138 ms
  3-NaivLoopUnrollingFour...                121 ms
  4-WinogradOriginal...                     109 ms
  5-WinogradScaled...                        98 ms
  6-StrassenNaiv...                          87 ms
  7-StrassenWinograd...                      83 ms
  8-III3-SeqBlockRC...                       95 ms
  9-III4-ParallelBlockRC...                  31 ms
  10-III5-EnhParallelBlockRC...              48 ms
  11-IV3-SeqBlockRR...                       92 ms
  12-IV4-ParallelBlockRR...                  29 ms
  13-IV5-EnhParallelBlockRR...               46 ms
  14-V3-SeqBlockCC...                       118 ms
  15-V4-ParallelBlockCC...                   38 ms

Tiempos guardados en: resultados/tiempos_ejecucion.csv

=== Fin de la ejecución ===
```

---
NOTA:
El repositorio no cuenta con la carpeta /resultados la cual es donde estan los CSV de las matrices y de los resultados, 
debido a que git no permite exceder cierta cantidad de MB cuando de quiere subir un repositorio. Por lo tanto los resultados correspondientes estan en el siguiente enlace https://docs.google.com/document/d/1-FICUXmPJKaTz0h4zLWnkropgvbo-zP-/edit?usp=sharing&ouid=103242792916122143766&rtpof=true&sd=true
