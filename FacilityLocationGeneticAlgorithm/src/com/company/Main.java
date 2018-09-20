package com.company;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class Main {

    private static class Offer implements Comparable<Offer> {
        int i;
        int j;
        double save;

        private Offer(int i, int j, double save) {
            this.i = i;
            this.j = j;
            this.save = save;
        }

        @Override
        public int compareTo(Offer o) {
            return Double.compare(o.save, save);
        }
    }

    private static class Individual {
        final int n;
        final int r;
        double fitness;
        double clarkeAndWrightFitness;
        int[] depots;
        int[] assignments;
        int[] assignmentCount;
        int[] lastBucket;
        int[][] adj;

        private Individual(int n, int r) {
            this.r = r;
            this.n = n;

            fitness = 0;
            clarkeAndWrightFitness = 0;
            depots = new int[r];
            assignmentCount = new int[n];
            assignments = new int[n];
            adj = new int[n][n];
        }

        private void unionAdjacency(int[][] other) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (other[i][j] == 1) {
                        adj[i][j] = 1;
                    }
                }
            }
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

    }

    private static class ClarkeWrightComparator implements Comparator<Individual> {
        @Override
        public int compare(Individual o1, Individual o2) {
            return Double.compare(o1.clarkeAndWrightFitness, o2.clarkeAndWrightFitness);
        }
    }

    public static void main(String[] args) {
        final String fileName = "input.txt";
        final int n;
        final int r;
        double[][] distance;
        double[] demand;
        double[] fixedCost;
        double[] plantDistance;
        double vehicleCapacity = 100;

        Random rand = new Random();

        //region Read From Input File
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
        //endregion


        int firstLevelIteration = 1000;

        int secondLevelIteration = 1000;

        int d = (int) Math.ceil((double) n / r);

        int populationSize = Math.max(2, (int) Math.ceil(((double) n / 100) * (Math.log(choose(n, r)) / d))) * d;

        double mutationProbability = 0.05;

        Individual[] population = locateDepots(r, populationSize, distance,
                fixedCost, plantDistance, mutationProbability, firstLevelIteration, rand);

        int scan = 0;
        List<Individual> topThree = new ArrayList<>(4);
        while (topThree.size() != 3 && scan < populationSize) {
            Individual individual = population[scan];
            boolean add = true;
            for (Individual i2 : topThree) {
                if (i2.calculateNonIdenticalGeneNumber(individual) == 0) {
                    add = false;
                    break;
                }
            }
            if (add) topThree.add(individual);
            scan++;
        }
        Individual[] topThreeArr = new Individual[3];
        topThree.toArray(topThreeArr);


        applySavingsAlgorithm(topThreeArr, distance, demand, vehicleCapacity,
                fixedCost, plantDistance, secondLevelIteration, rand);
        ClarkeWrightComparator comparator = new ClarkeWrightComparator();
        Arrays.sort(topThreeArr, comparator);

        StringBuilder outputText = new StringBuilder();
        for (int i = 0; i < n; i++)
            outputText.append(topThreeArr[0].assignments[i] + 1).append(" ");
        outputText.append("\r\n");
        outputText.append(topThreeArr[0].fitness);
        outputText.append("\r\n");
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                outputText.append(topThreeArr[0].adj[i][j]).append(" ");
            }
        }
        try {
            File outputFile = new File("output.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(outputText.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void applySavingsAlgorithm(Individual[] population, double[][] distance, double[] demands,
                                              double vehicleCapacity, double[] fixedCost, double[] plantDistance,
                                              int iterationCount, Random rand) {
        int n = population[0].assignments.length;
        int r = population[0].depots.length;

        for (Individual individual : population) {  //For each individual in population
            //Starting fitness, base fitness we can say
            for (int i = 0; i < r; i++) {
                int depot = individual.depots[i];
                individual.clarkeAndWrightFitness += fixedCost[depot] + plantDistance[depot];
            }
            for (int i = 0; i < r; i++) { //For each depot in individual, solve Genetic Clarke and Wright
                double bestObjective = Double.MAX_VALUE;
                int[][] bestAdj = null;
                int depot = individual.depots[i];
                int[] nodes = getNodesOfDepot(depot, individual.assignments, individual.assignmentCount[depot]);
                Offer[] offers = calculateSavings(nodes, depot, distance);
                for (int iteration = 0; iteration < iterationCount; iteration++) {
                    int[][] adj = new int[n][n];
                    int[] cycleList = new int[n];
                    boolean[] won = new boolean[offers.length];
                    double[] totalDemands = new double[n];
                    int[] order = new int[offers.length];
                    for (int j = 0; j < offers.length; j++) {
                        double sumSaving = 0;
                        double luck = rand.nextDouble();
                        int tournamentSize = rand.nextInt(Math.min(10, offers.length - j)) + 1;
                        int[] contestants = new int[tournamentSize];
                        int picked = 0;
                        int scan = 0;
                        while (picked != tournamentSize) {
                            if (!won[scan]) { //If this contestant has not won before
                                contestants[picked] = scan;
                                picked++;
                                sumSaving += offers[scan].save;
                            }
                            scan++;
                        }
                        for (int k = 0; k < tournamentSize; k++) {
                            luck -= offers[contestants[k]].save / sumSaving;
                            if (luck <= 0) {
                                //winner is k
                                order[j] = contestants[k];
                                won[contestants[k]] = true;
                                break;
                            }
                        }

                    }

                    double objective = 0;
                    for (int c = 0; c < nodes.length; c++) {
                        int node = nodes[c];
                        cycleList[node] = c + 1;
                        totalDemands[c + 1] = demands[node];
                        adj[depot][node] = 2;
                        adj[node][depot] = 2;
                        objective += 2 * distance[node][depot];
                    }


                    for (int k = 0; k < offers.length; k++) {
                        Offer offer = offers[order[k]];
                        int ord1 = adj[depot][offer.i];
                        int ord2 = adj[depot][offer.j];
                        int c1 = cycleList[offer.i];
                        int c2 = cycleList[offer.j];
                        if (c1 != c2 && totalDemands[c1] + totalDemands[c2] <= vehicleCapacity) {
                            if (ord1 == 2 && ord2 == 1) {
                                ord1 = 1;
                                ord2 = 2;
                                int tc = c1;
                                c1 = c2;
                                c2 = tc;
                                tc = offer.i;
                                offer.i = offer.j;
                                offer.j = tc;
                            }
                            if (ord1 == 1 && ord2 == 1) {
                                for (int j = 0; j < n; j++)
                                    if (cycleList[j] == c2)
                                        cycleList[j] = c1;
                            }
                            if (ord1 >= 1 && ord2 >= 1) {
                                totalDemands[c1] += totalDemands[c2];
                                totalDemands[c2] = 0;
                                cycleList[offer.j] = cycleList[offer.i];
                                adj[depot][offer.i]--;
                                adj[depot][offer.j]--;
                                adj[offer.i][depot]--;
                                adj[offer.j][depot]--;
                                adj[offer.j][offer.i] = 1;
                                adj[offer.i][offer.j] = 1;
                                objective -= offer.save;
                            }
                        }
                    }

                    if (objective < bestObjective) {
                        bestObjective = objective;
                        bestAdj = adj;
                    }
                }
                individual.unionAdjacency(bestAdj);
                individual.clarkeAndWrightFitness += bestObjective;

            } //End for(i=0 : r) each depot


        } //End for(Individual individual : population)
    }

    private static int[] getNodesOfDepot(int depot, int[] assignments, int count) {
        int[] nodes = new int[count - 1];
        int in = 0;
        for (int j = 0; j < assignments.length; j++) {
            if (j != depot && assignments[j] == depot) {
                nodes[in] = j;
                in++;

            }
        }
        return nodes;
    }

    private static Offer[] calculateSavings(int[] nodes, int depot, double[][] distance) {
        Offer[] offers = new Offer[nodes.length * (nodes.length - 1) / 2];
        int in = 0;
        for (int j = 0; j < nodes.length - 1; j++) {
            int nodeA = nodes[j];
            for (int k = j + 1; k < nodes.length; k++) {
                int nodeB = nodes[k];
                double saving = distance[nodeA][depot] + distance[nodeB][depot] - distance[nodeA][nodeB];
                Offer offer = new Offer(nodeA, nodeB, saving);
                offers[in] = offer;
                in++;
            }
        }
        Arrays.sort(offers);
        return offers;
    }

    private static void fillThePopulationDepots(Individual[] population, int groupSize, int groupCount, int n,
                                                int r, Random rand) {
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

    private static Individual[] locateDepots(int r, int populationSize, double[][] distance, double[] fixedCost,
                                             double[] plantDistance, double mutationProbability,
                                             int iterationCount, Random rand) {
        int n = distance.length;
        int groupSize = (int) Math.ceil((double) n / r);
        int groupCount = populationSize / groupSize;
        double mutationStep = (0.5 - mutationProbability) / iterationCount;

        Individual[] population = new Individual[populationSize];

        fillThePopulationDepots(population, groupSize, groupCount, n, r, rand); //To fill the depots we need to know group size group count etc.

        localSearch(population, n, r, distance, fixedCost, plantDistance);

        for (int i = 0; i < iterationCount; i++) {
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

            offspring1 = new Individual(n, r);
            offspring1.depots = parent1.depots.clone();


            offspring2 = new Individual(n, r);
            offspring2.depots = parent2.depots.clone();

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

            mutationProbability += mutationStep;

            for (int k = 0; k < r; k++) {
                offspring1.fitness += fixedCost[offspring1.depots[k]] + plantDistance[offspring1.depots[k]];
                offspring2.fitness += fixedCost[offspring2.depots[k]] + plantDistance[offspring2.depots[k]];
            }

            for (int j = 0; j < n; j++) {
                double closestDistanceA = Double.MAX_VALUE;
                double closestDistanceB = Double.MAX_VALUE;
                int depotA = -1;
                int depotB = -1;
                for (int k = 0; k < r; k++) {
                    if (closestDistanceA > distance[j][offspring1.depots[k]]) {
                        closestDistanceA = distance[j][offspring1.depots[k]];
                        depotA = offspring1.depots[k];
                        offspring1.assignments[j] = offspring1.depots[k];
                    }
                    if (closestDistanceB > distance[j][offspring2.depots[k]]) {
                        closestDistanceB = distance[j][offspring2.depots[k]];
                        depotB = offspring2.depots[k];
                        offspring2.assignments[j] = offspring2.depots[k];
                    }
                }
                offspring1.assignmentCount[depotA]++;
                offspring2.assignmentCount[depotB]++;

                offspring1.fitness += closestDistanceA;
                offspring2.fitness += closestDistanceB;
            }

            if (offspring1.fitness < population[populationSize - 1].fitness)
                insertInstanceAndSort(offspring1, population);

            if (offspring2.fitness < population[populationSize - 1].fitness)
                insertInstanceAndSort(offspring2, population);

            if (population[populationSize - 1].fitness < population[0].fitness * 1.010) break;

        }

        return population;
    }

    private static void localSearch(Individual[] population, int n, int r, double[][] distance, double[] fixedCost,
                                    double[] plantDistance) {
        for (int i = 0; i < population.length; i++) {

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
                //change the assignments
                //change the assignment counts
                population[i].assignmentCount[currentDepot] = 0;
                population[i].assignmentCount[alternativeDepot] = countOfElement;

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
                Individual temp = population[j - 1];
                population[j - 1] = population[j];
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

    private static int pick(int L, Random rand) {
        return L - (int) Math.floor((Math.sqrt(((rand.nextDouble() * 4) * (L * (L + 1))) + 1) - 1) / 2) - 1;
    }

}
