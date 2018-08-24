package com.company;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
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

        long startMillis = System.currentTimeMillis();
        int[] walker = new int[r];
        int[] bestWalker = new int[r];
        int[] bestAssignment = new int[n];
        double bestObjective = Double.MAX_VALUE;
        for (int i = 0; i < r; i++)
            walker[i] = i;

        while (walker[0] != n - r) {
            int[] assignments = new int[n];
            double obj = calculateCost(walker, demands, distance, n, r, assignments);
            if (obj < bestObjective) {
                bestObjective = obj;
                bestWalker = walker.clone();
                bestAssignment = assignments.clone();
            }
            walker[r - 1]++;
            int i = 0;
            while (walker[r - 1 - i] > n - 1 - i && i < r - 1) {
                walker[r - 1 - i] = 0;
                walker[r - 2 - i]++;
                i++;
            }
            for (i = 0; i < r - 1; i++)
                if (walker[i + 1] < walker[i])
                    walker[i + 1] = walker[i] + 1;

        }
        int[] assignments = new int[n];
        double obj = calculateCost(walker, demands, distance, n, r, assignments);
        if (bestObjective > obj) {
            bestWalker = walker.clone();
            bestObjective = obj;
            bestAssignment = assignments.clone();
        }
        long endMillis = System.currentTimeMillis();


        System.out.println();
        System.out.println("Exhaustive Duration: " + (endMillis - startMillis));
        System.out.printf("Optimum : %f \r\n", bestObjective);
        System.out.println();

        StringBuilder outputText = new StringBuilder();
        for (int i = 0; i < n; i++)
            outputText.append(bestAssignment[i] + 1).append(" ");
        outputText.append("\r\n");
        outputText.append(String.valueOf(bestObjective));


        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(outputText.toString());
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private static double calculateCost(int[] walk, double[] demands, double[][] distance, int n, int r, int[] assignments) {
        double objectiveValue = 0;
        for (int i = 0; i < n; i++) {
            double minDistance = Double.MAX_VALUE;
            for (int j = 0; j < r; j++) {
                int n1 = Math.min(i, walk[j]);
                int n2 = Math.max(walk[j], i);
                double distanceToDepotJ = distance[n1][n2];
                if (distanceToDepotJ < minDistance){
                    minDistance = distanceToDepotJ;
                    assignments[i] = walk[j];
                }
            }
            objectiveValue += minDistance * demands[i];
        }
        return objectiveValue;
    }
}
