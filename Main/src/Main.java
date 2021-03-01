import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        final String fileName = "input.txt";
        final String outputFile = "output.txt";
        File file = new File(fileName);
        try {
            Scanner sc = new Scanner(file).useLocale(Locale.US);
            //n is the amount of nodes
            //r is the amount of depots
            int n = sc.nextInt();
            int r = sc.nextInt();
            int vehicleCapacity = sc.nextInt();
            boolean[][] adjecenyMatrix = new boolean[n][n];
            double distanceIJ[][] = new double[n][n];
            double angles[] = new double[r];
            for (int i = 0; i < r; i++)
                angles[i] = i * (360.0 / r);
            double xDense = 0;
            double yDense = 0;
            double sumDemand = 0;
            double[][] coordinatesDemand = new double[n][3];
            int[] regions = new int[n]; //region[0] € [1, r]
            int[][] solution = new int[r][2];
            double[][] regionGravityCenters = new double[r][4];
            for (int i = 0; i < n; i++) {
                double x = sc.nextDouble();
                double y = sc.nextDouble();
                coordinatesDemand[i][0] = x;
                coordinatesDemand[i][1] = y;
                double d = sc.nextDouble();
                coordinatesDemand[i][2] = d;
                sumDemand += d;
                xDense += d * x;
                yDense += d * y;
            }
            xDense = xDense / sumDemand;
            yDense = yDense / sumDemand;

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    distanceIJ[i][j] = sc.nextDouble();
                }
            }


            StringBuilder output = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double x = coordinatesDemand[i][0] - xDense;
                double y = coordinatesDemand[i][1] - yDense;
                double d = coordinatesDemand[i][2];
                double angle = Math.atan2(y, x) * 180 / Math.PI;
                if (angle < 0) angle += 360;
                coordinatesDemand[i][0] = x;
                coordinatesDemand[i][1] = y;
                int k = 0;
                while (k < r && angle > angles[k]) k++;
                regionGravityCenters[k - 1][0] += x * d;
                regionGravityCenters[k - 1][1] += y * d;
                regionGravityCenters[k - 1][2] += d;
                regionGravityCenters[k - 1][3]++;
                regions[i] = k;
                output.append(String.valueOf(k)).append(" ").append("\r\n");
            }

            for (int i = 0; i < r; i++) {
                regionGravityCenters[i][0] = regionGravityCenters[i][0] / regionGravityCenters[i][2];
                regionGravityCenters[i][1] = regionGravityCenters[i][1] / regionGravityCenters[i][2];
                double cX = regionGravityCenters[i][0];
                double cY = regionGravityCenters[i][1];
                output.append(regionGravityCenters[i][0]).append(" ").append(regionGravityCenters[i][1]).append(" ");
                double minJVal = 0;
                int minJ = -1;
                for (int j = 0; j < n; j++) {
                    if (regions[j] != i + 1) continue;
                    solution[i][1]++;
                    double x = coordinatesDemand[j][0];
                    double y = coordinatesDemand[j][1];
                    double distance = Math.sqrt(Math.pow(x - cX, 2) + Math.pow(y - cY, 2));
                    if (minJ == -1 || distance < minJVal) {
                        minJ = j;
                        minJVal = distance;
                    }
                }
                solution[i][0] = minJ;
            }


            for (int i = 0; i < r; i++) {
                //r tane depo var, i burda depo indisi
                int subSize = solution[i][1];
                double[][] subDistance = new double[subSize][subSize];
                double[] subDemand = new double[subSize];

                int[] nodes = new int[subSize];
                nodes[0] = solution[i][0];

                int next = 1;
                for (int j = 0; j < n; j++) {
                    if (regions[j] != i + 1 || j == solution[i][0]) continue;
                    subDemand[next] = coordinatesDemand[j][2];
                    nodes[next] = j;
                    next++;
                }

                for (int j = 0; j < subSize - 1; j++) {
                    for (int k = j + 1; k < subSize; k++) {
                        int node1 = Math.min(nodes[j], nodes[k]);
                        int node2 = Math.max(nodes[j], nodes[k]);
                        subDistance[j][k] = distanceIJ[node1][node2];
                    }
                }


                boolean[][] solAdj = clarkeAndWright(subDistance, subDemand, subSize, vehicleCapacity);
                for (int j = 0; j < subSize; j++) {
                    for (int k = 0; k < subSize; k++) {
                        adjecenyMatrix[nodes[j]][nodes[k]] = solAdj[j][k];
                    }
                }
            }

            System.out.println();

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    output.append(adjecenyMatrix[i][j] ? 1 : 0).append("\r\n");
                }
            }
            String text = output.toString();
            FileWriter writer = new FileWriter(outputFile);
            writer.write(text);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }


    public static boolean[][] clarkeAndWright(double[][] distance, double[] demand, int n, int vehicleCapacity) {
        double[] sumDemand = new double[n];
        boolean[][] adjacency = new boolean[n][n];
        int[][] cycle = new int[n - 1][n + 2];
        int[] node2Cycle = new int[n];
        for (int i = 1; i < n; i++) {
            cycle[i - 1][0] = 3;
            cycle[i - 1][2] = i;
            sumDemand[i - 1] = demand[i];
            node2Cycle[i] = i - 1;
        }
        int[] indices = new int[n * n / 2];
        double[] savings = new double[n * n / 2];
        int next = 0;

        for (int i = 1; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                double current = distance[0][i] + distance[0][j] - distance[i][j];
                if (current <= 0) continue;
                int key = (i * n) + j;
                if (next == 0) {
                    indices[0] = key;
                    savings[0] = current;
                    next++;
                    continue;
                }
                //linear search for now
                int k = 0;
                while (savings[k] > current) k++;
                for (int o = next - 1; o >= k; o--) {
                    savings[o + 1] = savings[o];
                    indices[o + 1] = indices[o];
                }
                savings[k] = current;
                indices[k] = key;
                next++;

            }
        }

        for (int i = 0; i < next; i++) {
            int n2 = indices[i] % n;
            int n1 = (indices[i] - n2) / n;

            int c1 = node2Cycle[n1];
            int c2 = node2Cycle[n2];

            int len1 = cycle[c1][0];
            int len2 = cycle[c2][0];


            if (cycle[c2][len2 - 1] == n2 && cycle[c1][2] == n1) {
                //if n2 is the end and n1 is the start, swap
                int swap = n2;
                n2 = n1;
                n1 = swap;
                swap = c1;
                c1 = c2;
                c2 = swap;
                swap = len1;
                len1 = len2;
                len2 = swap;
            } else if (cycle[c2][2] == n2 && cycle[c1][2] == n1) {
                //reverse the c1
                for (int j = 0; j < len1 / 2; j++) {
                    int temp = cycle[c1][j + 1];
                    cycle[c1][j + 1] = cycle[c1][len1 - j];
                    cycle[c1][len1 - j] = temp;
                }
            }

            if (c1 != c2 && sumDemand[c1] + sumDemand[c2] < vehicleCapacity) {
                if (cycle[c1][len1 - 1] == n1) {
                    //n1 is the end
                    if (cycle[c2][2] == n2) {
                        //n2 is the start
                        for (int j = 0; j < len2 - 1; j++) {
                            int val = cycle[c2][2 + j];
                            cycle[c1][len1 + j] = val;
                            node2Cycle[val] = c1;
                        }
                        sumDemand[c1] += sumDemand[c2];
                        sumDemand[c2] = 0;
                        cycle[c1][0] = len1 + len2 - 2;
                        cycle[c2][0] = 0;
                    } else if (cycle[c2][len2 - 1] == n2) {
                        for (int j = 0; j < len2 - 1; j++) {
                            //n2 is the end
                            int val = cycle[c2][len2 - 1 - j];
                            cycle[c1][len1 + j] = val;
                            node2Cycle[val] = c1;
                        }
                        sumDemand[c1] += sumDemand[c2];
                        sumDemand[c2] = 0;
                        cycle[c1][0] = len1 + len2 - 2;
                        cycle[c2][0] = 0;
                    }
                }
            }
        }

        for (int c = 0; c < n - 1; c++) { //c index for cycle
            int cycleLength = cycle[c][0];
            for (int i = 1; i < cycleLength; i++) {
                adjacency[cycle[c][i]][cycle[c][i + 1]] = true;
                adjacency[cycle[c][i + 1]][cycle[c][i]] = true;
            }
        }

        return adjacency;
    }
}
