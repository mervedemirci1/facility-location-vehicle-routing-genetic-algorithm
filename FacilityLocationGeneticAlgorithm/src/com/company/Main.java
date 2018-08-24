package com.company;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        final String fileName = "input.txt";
        final String outputFile = "output.txt";
        int n;
        int r;
        double[] demands;
        double[][] distance;

        File file = new File(fileName);
        try {
            Scanner sc = new Scanner(file).useLocale(Locale.US);
            n = sc.nextInt();
            r = sc.nextInt();
            demands = new double[n];
            distance = new double[n][n];
            for (int i = 0; i < n; i++)
                demands[i] = sc.nextDouble();

            for (int i = 0; i < n - 1; i++)
                for (int j = i + 1; j < n; j++)
                    distance[i][j] = sc.nextDouble();

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < n - 1; i++)
            for (int j = i + 1; j < n; j++)
                distance[j][i] = distance[i][j];

        long startMillis = System.currentTimeMillis();
        //Set parameters
        int d = (int) Math.ceil((double) n / r);
        int populationSize = Math.max(2, (int) Math.ceil(((double) n / 100) * (Math.log(choose(n, r)) / d))) * d;
        int groupSize = (int) Math.ceil((double) n / r);
        int groupCount = populationSize / groupSize;
        int maxIter = 500;


        int[][] population = new int[populationSize][r];
        int lastUpdated = -1;
        for (int j = 0; j < groupCount; j++) {
            int number = 0;
            for (int i = 0; i < groupSize; i++) {
                population[i + j * groupSize][0] = (number % n) + 1;
                boolean broke = false;
                for (int o = 0; o < r; o++) {
                    if (number >= n + 1)
                        broke = true;
                    population[i + j * groupSize][o] = (number % n) + 1;
                    lastUpdated = i + j * groupSize;
                    number += (j + 1);
                }
                if (broke) break;
            }
        }

        Random rand = new Random();
        for (int i = lastUpdated + 1; i < populationSize; i++) {
            boolean[] bucket = new boolean[n];
            for (int j = 0; j < r; j++) {
                int nextRand;
                do {
                    nextRand = rand.nextInt(n);
                } while (bucket[nextRand]);
                bucket[nextRand] = true;
                population[i][j] = nextRand + 1;
            }
        }

        for (int i = 0; i < populationSize; i++) {
            for (int j = 0; j < r; j++)
                population[i][j] -= 1;
        }


        double[] fitnessMatrix = new double[populationSize];
        for (int i = 0; i < populationSize; i++) {
            int[] assignments = new int[n]; //Value based
            int[] assignmentCount = new int[n]; //Also value based;

            for (int j = 0; j < n; j++) {
                double min = Double.MAX_VALUE;
                for (int k = 0; k < r; k++) {
                    int currentDepot = population[i][k];
                    double dist = distance[currentDepot][j];
                    if (min > dist) {
                        min = dist;
                        assignments[j] = currentDepot;
                    }
                }
                assignmentCount[assignments[j]]++;
            }

            for (int j = 0; j < r; j++) {
                int currentDepot = population[i][j];
                int countOfElement = assignmentCount[currentDepot];
                int[] nodes = new int[countOfElement];
                int nodeCount = 0;
                for (int k = 0; k < n; k++) {
                    if (assignments[k] == currentDepot) {
                        nodes[nodeCount] = k;
                        nodeCount++;
                    }
                    if (nodeCount == countOfElement) break;
                }

                double bestAlternativeFitness = Double.MAX_VALUE;
                int alternativeDepot = currentDepot;
                for (int k : nodes) {
                    double fitness = 0;
                    for (int k2 : nodes) {
                        fitness += distance[k][k2] * demands[k2];
                    }
                    if (fitness < bestAlternativeFitness) {
                        bestAlternativeFitness = fitness;
                        alternativeDepot = k;
                    }
                }

                population[i][j] = alternativeDepot;
                fitnessMatrix[i] += bestAlternativeFitness;
            }

            for (int j = i; j > 0; j--) {
                if (fitnessMatrix[j - 1] > fitnessMatrix[j]) {
                    double temp = fitnessMatrix[j - 1];
                    fitnessMatrix[j - 1] = fitnessMatrix[j];
                    fitnessMatrix[j] = temp;

                    int[] temp2 = population[j - 1].clone();
                    population[j - 1] = population[j].clone();
                    population[j] = temp2;
                } else break;
            }
        }


        for (int i = 0; i < maxIter; i++) {
            int n1 = pick(populationSize, rand);
            int n2 = pick(populationSize, rand);
            int[] bucket = new int[n];
            for (int j = 0; j < r; j++) {
                bucket[population[n1][j]]++;
                bucket[population[n2][j]]++;
            }
            int nonIdenticalGeneNumber = 0;
            for (int j = 0; j < n; j++)
                if (bucket[j] == 1)
                    nonIdenticalGeneNumber++;


            if (nonIdenticalGeneNumber == 0) {
                continue;
            }
            int swapAmount = rand.nextInt(Math.min(nonIdenticalGeneNumber, r - 1)) + 1;


            int[] offspring1 = population[n1].clone();
            int[] offspring2 = population[n2].clone();

            int j1 = 0;
            int j2 = 0;
            while (swapAmount > 0) {
                while (bucket[population[n1][j1]] != 1)
                    j1++;
                while (bucket[population[n2][j2]] != 1)
                    j2++;

                offspring1[j1] = population[n2][j2];
                offspring2[j2] = population[n1][j1];
                swapAmount--;
            }

            //calculate fitness of 2 offsprings
            //add them to list if their fitness is better than the worst
            double fitness1 = 0;
            double fitness2 = 0;

            for (int j = 0; j < n; j++) {
                double closestDistanceA = Double.MAX_VALUE;
                double closestDistanceB = Double.MAX_VALUE;

                for (int k = 0; k < r; k++) {
                    if (closestDistanceA > distance[j][offspring1[k]]) {
                        closestDistanceA = distance[j][offspring1[k]];
                    }
                    if (closestDistanceB > distance[j][offspring2[k]]) {
                        closestDistanceB = distance[j][offspring2[k]];
                    }
                }
                fitness1 += closestDistanceA * demands[j];
                fitness2 += closestDistanceB * demands[j];
            }

            if (fitness1 < fitnessMatrix[populationSize - 1]) {
                fitnessMatrix[populationSize - 1] = fitness1;
                population[populationSize - 1] = offspring1;
                for (int j = populationSize - 1; j > 0; j--) {
                    if (fitnessMatrix[j] < fitnessMatrix[j - 1]) {
                        double temp = fitnessMatrix[j];
                        fitnessMatrix[j] = fitnessMatrix[j - 1];
                        fitnessMatrix[j - 1] = temp;
                        int[] tempA = population[j].clone();
                        population[j] = population[j - 1].clone();
                        population[j - 1] = tempA;
                    } else break;
                }
            }

            if (fitness2 < fitnessMatrix[populationSize - 1]) {
                int swapN = 50;
                fitnessMatrix[populationSize - 1] = fitness2;
                population[populationSize - 1] = offspring2;
                for (int j = populationSize - 1; j > 0; j--) {
                    if (fitnessMatrix[j] < fitnessMatrix[j - 1]) {
                        double temp = fitnessMatrix[j];
                        fitnessMatrix[j] = fitnessMatrix[j - 1];
                        fitnessMatrix[j - 1] = temp;
                        swapN++;
                        int[] tempA = population[j].clone();
                        population[j] = population[j - 1].clone();
                        population[j - 1] = tempA;
                    } else break;
                }
            }
        }

        long endMillis = System.currentTimeMillis();

        int[] assignments = new int[n];
        int[] bestSol = population[0];
        for (int i = 0; i < n; i++) {
            double dist = Double.MAX_VALUE;
            for (int j = 0; j < r; j++) {
                if (dist > distance[i][bestSol[j]]) {
                    dist = distance[i][bestSol[j]];
                    assignments[i] = bestSol[j];
                }
            }
        }

        System.out.println();
        System.out.println("Genetic Duration : " + (endMillis - startMillis));
        System.out.printf("Best objective : %f \r\n", fitnessMatrix[0]);
        System.out.println();
        StringBuilder outputText = new StringBuilder();
        for (int i = 0; i < n; i++)
            outputText.append(assignments[i] + 1).append(" ");
        outputText.append("\r\n");
        outputText.append(fitnessMatrix[0]);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(outputText.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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
