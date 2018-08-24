clear;
clc;
numOfLocations = 300;
numOfDepots = 3;
vehicleCapacity = 100;
networkRadius = 25000;
coordinates = zeros(2, numOfLocations);
coordinates(1, 1:numOfLocations) = rand(1, numOfLocations) * networkRadius;
coordinates(2, 1:numOfLocations) = rand(1, numOfLocations) * 2 * pi;
distance = zeros(numOfLocations, numOfLocations); %in meters
demand = (rand(numOfLocations, 1) * 10) + (10 * ones(numOfLocations, 1));

for k=1:numOfLocations
    coordinates(:,k) = [coordinates(1,k) * cos(coordinates(2,k)) ; coordinates(1, k) * sin(coordinates(2, k))];
end
clear k;

for i=1:numOfLocations
    for j=i+1:numOfLocations
        distance(i, j) = sqrt((coordinates(1, i) - coordinates(1, j))^2 + (coordinates(2, i) - coordinates(2,j))^2);;
    end
end


fileID = fopen('input.txt','w');
fprintf(fileID, '%d %d\r\n', numOfLocations, numOfDepots);
for i=1:numOfLocations
    fprintf(fileID, '%f ', demand(i));
end
fprintf(fileID, '\r\n');
for i=1:numOfLocations-1
   for j=i+1:numOfLocations
    fprintf(fileID, '%f\r\n', distance(i, j));
   end
end

fclose(fileID);

system('java -jar FacilityLocationExhaustiveSearch.jar');

sizeA = [1 numOfLocations];
sizeB = [1 1];
fileID = fopen('output.txt','r');
exhaustive = fscanf(fileID, '%d', sizeA);
optimalSolution = fscanf(fileID, '%f', sizeB);

fclose(fileID);

system('java -jar FacilityLocationGeneticAlgorithm.jar');
fileID = fopen('output.txt', 'r');
genetic = fscanf(fileID, '%d', sizeA);
geneticSolution = fscanf(fileID, '%f', sizeB);
fclose(fileID);

absoluteGap = geneticSolution - optimalSolution;
relativeGap = absoluteGap / optimalSolution;

sprintf('Absoulte Gap: %f \r\n Relative Gap: %f', absoluteGap, relativeGap)

optimumAdjacency = zeros(numOfLocations, numOfLocations);
for i=1:numOfLocations
   optimumAdjacency(i, exhaustive(i)) = 1;
   optimumAdjacency(exhaustive(i), i) = 1;
end

geneticResult = zeros(numOfLocations, numOfLocations);
for i=1:numOfLocations
   geneticResult(i, genetic(i)) = 1;
   geneticResult(genetic(i), i) = 1;
end


G = graph(optimumAdjacency);
G2 = graph(geneticResult);

figure
subplot(1,2,1)  
plot(G, 'XData', coordinates(1, :), 'YData', coordinates(2 , :));
title('Optimum')

subplot(1,2,2)
plot(G2, 'XData', coordinates(1, :), 'YData', coordinates(2 , :));
title('Genetic')

