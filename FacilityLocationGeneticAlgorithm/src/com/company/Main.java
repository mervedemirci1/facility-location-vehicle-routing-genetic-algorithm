package com.company;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class Main {

    private static class SecondLevelGeneticAlgorithmResult {
        int[][] bestCycle;
        double objective;

        private SecondLevelGeneticAlgorithmResult(int[][] bestCycle, double objective) {
            this.bestCycle = bestCycle;
            this.objective = objective;
        }
    }

    private static class Individual {
        int n;
        int r;
        double fitness;
        int[] depots;
        int[] assignments;
        int[] assignmentCount;
        int[] lastBucket;

        private Individual(int n, int r) {
            this.r = r;
            this.n = n;

            fitness = 0;
            depots = new int[r];
            assignmentCount = new int[n];
            assignments = new int[n];
        }

        private int calculateNonIdenticalGeneNumber(Individual other) { //Bucket search
            int[] bucket = new int[n];
            int result = 0;
            for (int i = 0; i < r; i++)
                bucket[depots[i]] += 1;

            for (int i = 0; i < r; i++)
                bucket[other.depots[i]] += 1;

            for (int i = 0; i < n; i++)
                if (bucket[i] == 1) result++;

            lastBucket = bucket;

            return result / 2;
        }

        private Individual deepClone() {
            Individual cloned = new Individual(n, r);
            cloned.fitness = this.fitness;
            cloned.depots = this.depots.clone();
            cloned.assignments = this.assignments.clone();
            cloned.assignmentCount = this.assignmentCount.clone();
            return cloned;
        }
    }

    public static void main(String[] args) {
        final String fileName = "input.txt";
        int n;
        int r;
        double[][] distance;
        double[] demand;
        double[] fixedCost;
        double[] plantDistance;
        Random rand = new Random();
        File file = new File(fileName);
        try {
            Scanner sc = new Scanner(file).useLocale(Locale.US);
            n = sc.nextInt();
            r = sc.nextInt();
            fixedCost = new double[n];
            distance = new double[n][n];
            plantDistance = new double[n];
            demand = new double[n];
            for (int i = 0; i < n - 1; i++)
                for (int j = i + 1; j < n; j++)
                    distance[i][j] = sc.nextDouble();

            for (int i = 0; i < n; i++)
                fixedCost[i] = sc.nextDouble();

            for (int i = 0; i < n; i++)
                plantDistance[i] = sc.nextDouble();

            for (int i = 0; i < n; i++)
                demand[i] = sc.nextDouble();

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < n - 1; i++)
            for (int j = i + 1; j < n; j++)
                distance[j][i] = distance[i][j];

        long startMillis = System.currentTimeMillis();
        int firstLevelIteration = 500;
        int d = (int) Math.ceil((double) n / r);
        int populationSize = Math.max(2, (int) Math.ceil(((double) n / 100) * (Math.log(choose(n, r)) / d))) * d;
        double mutationProbability = 0.05;

        Individual[] solution = solveFirstLevelWithGeneticAlgorithm(r, populationSize, distance,
                fixedCost, plantDistance, mutationProbability, firstLevelIteration, rand);


        long endMillis = System.currentTimeMillis();

        System.out.println();
        System.out.println("Genetic Duration : " + (endMillis - startMillis));
        System.out.printf("Best objective : %f \r\n", solution[0].fitness);
        System.out.println();
        StringBuilder outputText = new StringBuilder();
        for (int i = 0; i < n; i++)
            outputText.append(solution[0].assignments[i] + 1).append(" ");
        outputText.append("\r\n");
        outputText.append(solution[0].fitness);

        try {
            File outputFile = new File("output.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(outputText.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static SecondLevelGeneticAlgorithmResult solveSecondLevelWithGeneticAlgorithm(int n, double[][] distance,
                                                                                          int secondLevelIteration, Random rand, double[] demand, int vehicleCapacity) {
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

        int[][] bestCycle = new int[n - 1][n + 2];
        double bestObjective = Double.MAX_VALUE;

        for (int iteration = 0; iteration < secondLevelIteration; iteration++) {
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

            } else {
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

            if (objective < bestObjective) {
                bestObjective = objective;
                for (int i = 0; i < n - 1; i++) {
                    System.arraycopy(cycle[i], 0, bestCycle[i], 0, n + 2);
                }
                indices = newIndices.clone();
                savings = newSavingsList.clone();
            }
        }

        return new SecondLevelGeneticAlgorithmResult(bestCycle, bestObjective);
    }

    private static void fillThePopulationDepots(Individual[] population, int groupSize, int groupCount, int n, int r, Random rand) {
        int lastUpdated = -1;
        int populationSize = population.length;
        for (int i = 0; i < populationSize; i++) {
            population[i] = new Individual(n, r);
        }

        for (int j = 0; j < groupCount; j++) {
            int number = 0;
            for (int i = 0; i < groupSize; i++) {
                population[i + j * groupSize].depots[0] = (number % n) + 1;
                boolean broke = false;
                for (int o = 0; o < r; o++) {
                    if (number >= n + 1)
                        broke = true;
                    population[i + j * groupSize].depots[o] = (number % n) + 1;
                    lastUpdated = i + j * groupSize;
                    number += (j + 1);
                }
                if (broke) break;
            }
        }

        for (int i = lastUpdated + 1; i < populationSize; i++) {
            boolean[] bucket = new boolean[n];
            for (int j = 0; j < r; j++) {
                int nextRand;
                do {
                    nextRand = rand.nextInt(n);
                } while (bucket[nextRand]);
                bucket[nextRand] = true;
                population[i].depots[j] = nextRand + 1;
            }
        }

        for (Individual individual : population)
            for (int j = 0; j < r; j++)
                individual.depots[j] -= 1;
    }

    private static Individual[] solveFirstLevelWithGeneticAlgorithm(int r, int populationSize, double[][] distance, double[] fixedCost, double[] plantDistance, double mutationProbability, int firstLevelIteration, Random rand) {
        int n = distance.length;
        int groupSize = (int) Math.ceil((double) n / r);
        int groupCount = populationSize / groupSize;

        Individual[] population = new Individual[populationSize];
        fillThePopulationDepots(population, groupSize, groupCount, n, r, rand); //To fill the depots we need to know group size group count etc.

        for (int i = 0; i < populationSize; i++) {
            for (int j = 0; j < n; j++) {
                double min = Double.MAX_VALUE;
                for (int k = 0; k < r; k++) {
                    int currentDepot = population[i].depots[k];
                    double dist = distance[currentDepot][j];
                    if (min > dist) {
                        min = dist;
                        population[i].assignments[j] = currentDepot;
                    }
                }
                population[i].assignmentCount[population[i].assignments[j]]++;
            }
            //Assignments for initial solution

            for (int j = 0; j < r; j++) {
                int currentDepot = population[i].depots[j];
                int countOfElement = population[i].assignmentCount[currentDepot];
                int[] nodes = new int[countOfElement];
                int nodeCount = 0;
                for (int k = 0; k < n; k++) {
                    if (population[i].assignments[k] == currentDepot) {
                        nodes[nodeCount] = k;
                        nodeCount++;
                    }
                    if (nodeCount == countOfElement) break;
                }
                //Load nodes that are connected to current node
                //Local search
                double bestAlternativeFitness = Double.MAX_VALUE;
                int alternativeDepot = currentDepot;
                for (int k : nodes) {
                    double fitness = fixedCost[k] + plantDistance[k];
                    for (int k2 : nodes) {
                        fitness += distance[k][k2];
                    }
                    if (fitness < bestAlternativeFitness) {
                        bestAlternativeFitness = fitness;
                        alternativeDepot = k;
                    }
                }
                for (int k : nodes)
                    population[i].assignments[k] = alternativeDepot;
                population[i].depots[j] = alternativeDepot;
                population[i].fitness += bestAlternativeFitness;
            }
            //Sort
            for (int j = i; j > 0; j--) {
                if (population[j - 1].fitness > population[j].fitness) {
                    Individual individual = population[j - 1];
                    population[j - 1] = population[j];
                    population[j] = individual;
                } else break;
            }
        }

        //Genetic start
        for (int i = 0; i < firstLevelIteration; i++) {
            int n1 = pick(populationSize, rand);
            int n2 = pick(populationSize, rand);

            Individual parent1 = population[n1];
            Individual parent2 = population[n2];

            int nonIdenticalGeneNumber = parent1.calculateNonIdenticalGeneNumber(parent2);
            if (nonIdenticalGeneNumber == 0)
                continue;

            int swapAmount = rand.nextInt(nonIdenticalGeneNumber) + 1;
            int[] bucket = parent1.lastBucket;
            int j1 = 0;
            int j2 = 0;

            Individual offspring1, offspring2;

            offspring1 = parent1.deepClone();
            offspring2 = parent2.deepClone();

            while (swapAmount > 0) {
                while (bucket[parent1.depots[j1]] != 1)
                    j1++;
                while (bucket[parent2.depots[j2]] != 1)
                    j2++;

                offspring1.depots[j1] = parent2.depots[j2];
                offspring2.depots[j2] = parent1.depots[j1];
                swapAmount--;
            }

            if (rand.nextDouble() < mutationProbability)
                mutate(offspring1, n, rand);
            if (rand.nextDouble() < mutationProbability)
                mutate(offspring2, n, rand);

            offspring1.fitness = 0;
            offspring1.assignments = new int[n];
            offspring1.assignmentCount = new int[n];


            offspring2.fitness = 0;
            offspring2.assignments = new int[n];
            offspring2.assignmentCount = new int[n];

            for (int k = 0; k < r; k++) {
                offspring1.fitness += fixedCost[offspring1.depots[k]] + plantDistance[offspring1.depots[k]];
                offspring2.fitness += fixedCost[offspring2.depots[k]] + plantDistance[offspring2.depots[k]];
            }

            for (int j = 0; j < n; j++) {
                double closestDistanceA = Double.MAX_VALUE;
                double closestDistanceB = Double.MAX_VALUE;
                int depotIndexA = -1;
                int depotIndexB = -1;
                for (int k = 0; k < r; k++) {
                    if (closestDistanceA > distance[j][offspring1.depots[k]]) {
                        closestDistanceA = distance[j][offspring1.depots[k]];
                        depotIndexA = k;
                        offspring1.assignments[j] = offspring1.depots[k];
                    }
                    if (closestDistanceB > distance[j][offspring2.depots[k]]) {
                        closestDistanceB = distance[j][offspring2.depots[k]];
                        depotIndexB = k;
                        offspring2.assignments[j] = offspring2.depots[k];
                    }
                }
                offspring1.assignmentCount[depotIndexA]++;
                offspring2.assignmentCount[depotIndexB]++;

                offspring1.fitness += closestDistanceA;
                offspring2.fitness += closestDistanceB;
            }

            if (offspring1.fitness < population[populationSize - 1].fitness) {
                insertInstanceAndSort(offspring1, population);
            }

            if (offspring2.fitness < population[populationSize - 1].fitness) {
                insertInstanceAndSort(offspring2, population);
            }

        }

        return population;
    }

    private static void mutate(Individual offspring, int pointCount, Random rand) {
        int nextDepot = 0;
        boolean exists = true;
        while (exists) {
            nextDepot = rand.nextInt(pointCount);
            exists = false;
            for (int node : offspring.depots) {
                if (node == nextDepot) {
                    exists = true;
                    break;
                }
            }
        }
        offspring.depots[rand.nextInt(offspring.depots.length)] = nextDepot;
    }

    private static void insertInstanceAndSort(Individual offspring, Individual[] population) {
        int populationSize = population.length;
        population[populationSize - 1] = offspring;
        for (int j = populationSize - 1; j > 0; j--) {
            if (population[j].fitness < population[j - 1].fitness) {
                Individual temp = population[j - 1].deepClone();
                population[j - 1] = population[j].deepClone();
                population[j] = temp;
            } else break;
        }

    }

    private static int choose(final int n, final int r) {
        int a = Math.max(r, n - r);
        int b = Math.min(r, n - r);
        int result = 1;
        for (int i = n; i > a; i--)
            result *= i;
        for (int i = 2; i <= b; i++)
            result /= i;
        return result;
    }

    private static int pick(int L, @NotNull Random rand) {
        return L - (int) Math.floor((Math.sqrt(((rand.nextDouble() * 4) * (L * (L + 1))) + 1) - 1) / 2) - 1;
    }

}
