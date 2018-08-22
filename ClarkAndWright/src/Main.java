import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        final int maxIterationCount = 5000;
        final String fileName = "input.txt";
        final String outputFile = "output.txt";
        final  File file = new File(fileName);
        final int n;
        final int vehicleCapacity;
        double[][] distance;
        double[] demand;
        final Random rand = new Random();

        //region Read Inputs(n, vehicleCapacity distance demand) from file
        try {
            Scanner sc = new Scanner(file).useLocale(Locale.US);
            n = sc.nextInt();
            vehicleCapacity = sc.nextInt();
            distance = new double[n][n];
            demand = new double[n];
            for (int a = 0; a < n * (n - 1) / 2; a++) {
                int i = sc.nextInt() - 1;
                int j = sc.nextInt() - 1;
                double d = sc.nextDouble();
                distance[i][j] = d;
            }
            for (int a = 0; a < n; a++)
                demand[a] = sc.nextDouble();
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        //endregion
        //region Calculate Savings and indices start sort and keep em
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
        //endregion

        int[][] bestCycle = new int[n - 1][n + 2];
        double bestObjective = Double.MAX_VALUE;

        for (int iteration = 0; iteration < maxIterationCount; iteration++) {
            //Decision variables
            int[][] cycle = new int[n - 1][n + 2];
            int[] node2Cycle = new int[n];
            double[] sumDemand = new double[n];
            double objective = 0;
            //Decision variables
            double[] newSavingsList;
            int[] newIndices;
            if (iteration != 0) {
                newSavingsList = new double[next];
                newIndices = new int[next];
                boolean[] used = new boolean[next];

                //In each iteration we have to fill one savings list and one indices list
                //We do it by making tournaments
                for (int i = 0; i < next - 1; i++) {
                    int tournamentSize = rand.nextInt(Math.min(next - i - 1, 10)) + 1;
                    int[] tournamentNodes = new int[tournamentSize];
                    double[] probabilities = new double[tournamentSize];
                    double sumProbability = 0;

                    int pickedNodes = 0;
                    int index = 0;

                    while (pickedNodes < tournamentSize) {
                        if (!used[index]) {
                            tournamentNodes[pickedNodes] = index;
                            probabilities[pickedNodes] = savings[index];
                            sumProbability += savings[index];
                            pickedNodes++;
                        }
                        index++;
                    }

                    //normalize the probability
                    for (int j = 0; j < tournamentSize; j++) {
                        probabilities[j] = probabilities[j] / sumProbability;
                    }

                    int winner = -1;
                    double win = rand.nextDouble();
                    double stack = 0;
                    for (int j = 0; j < tournamentSize; j++) {
                        stack += probabilities[j];
                        if (stack >= win) {
                            used[tournamentNodes[j]] = true;
                            winner = j;
                            break;
                        }
                    }

                    newSavingsList[i] = savings[tournamentNodes[winner]];
                    newIndices[i] = indices[tournamentNodes[winner]];
                }

                for (int i = 0; i < next; i++) {
                    if (!used[i]) {
                        newIndices[next - 1] = indices[i];
                        newSavingsList[next - 1] = savings[i];
                        used[i] = true;
                        break;
                    }
                }

            }else{
                newSavingsList = savings.clone();
                newIndices = indices.clone();
            }
            //Initial solution
            for (int i = 1; i < n; i++) {
                cycle[i - 1][0] = 3;
                cycle[i - 1][2] = i;
                sumDemand[i - 1] = demand[i];
                objective += 2 * distance[0][i];
                node2Cycle[i] = i - 1;
            }

            for (int i = 0; i < next; i++) {
                int n2 = newIndices[i] % n;
                int n1 = (newIndices[i] - n2) / n;

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
                            objective -= newSavingsList[i];
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
                            objective -= newSavingsList[i];
                        }
                    }
                }
            }

            if(objective < bestObjective){
                System.out.println("Update " + (bestObjective - objective));
                bestObjective = objective;
                for (int i = 0; i < n -1; i++) {
                    System.arraycopy(cycle[i], 0, bestCycle[i], 0, n + 2);
                }
                indices = newIndices.clone();
                savings = newSavingsList.clone();
            }


        }


        //calculate the adjacency
        boolean[][] adjacency = new boolean[n][n];
        for (int c = 0; c < n - 1; c++) { //c index for cycle
            int cycleLength = bestCycle[c][0];
            for (int i = 1; i < cycleLength; i++) {
                adjacency[bestCycle[c][i]][bestCycle[c][i + 1]] = true;
                adjacency[bestCycle[c][i + 1]][bestCycle[c][i]] = true;
            }
        }

        StringBuilder outputText = new StringBuilder();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++)
                outputText.append(adjacency[i][j] ? 1 : 0).append(" ");
            outputText.append("\r\n");
        }

        System.out.println(bestObjective);
        outputText.append(bestObjective);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(outputText.toString());
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
