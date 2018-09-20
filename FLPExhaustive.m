clear;
clc;
numOfLocations = 200;
numOfDepots = 4;
vehicleCapacity = 30;
networkRadius = 25000;
coordinates = zeros(2, numOfLocations);
coordinates(1, 1:numOfLocations) = rand(1, numOfLocations) * networkRadius;
coordinates(2, 1:numOfLocations) = rand(1, numOfLocations) * 2 * pi;
distance = zeros(numOfLocations, numOfLocations); %in meters
demand = (rand(numOfLocations, 1) * 10) + (10 * ones(numOfLocations, 1));
fixedCost = (rand(numOfLocations, 1) * 500) + (1000 * ones(numOfLocations, 1));
plantDist = zeros(numOfLocations);

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
for i=1:numOfLocations-1
   for j=i+1:numOfLocations
    fprintf(fileID, '%f\r\n', distance(i, j));
   end
end

for i=1:numOfLocations
    plantDist(i) = sqrt(coordinates(1, i) ^ 2 + coordinates(2, i) ^ 2);
    fprintf(fileID, '%f ', fixedCost(i));
end
fprintf(fileID, '\r\n');

for i=1:numOfLocations
    fprintf(fileID, '%f ',  plantDist(i));
end

for i=1:numOfLocations
    fprintf(fileID, '%f ',  demand(i));
end
fclose(fileID);

sizeA = [numOfLocations 1];
sizeB = [1 1];
sizeC = [numOfLocations numOfLocations];
system('java -jar FacilityLocationGeneticAlgorithm.jar');
fileID = fopen('output.txt', 'r');
genetic = fscanf(fileID, '%d', sizeA);
assignments = fscanf(fileID, '%f', sizeB);
adj = fscanf(fileID, '%f', sizeC);
fclose(fileID);

%for i=1:numOfLocations
%    assignments(i, genetic(i)) = 1;
%    assignments(genetic(i), i) = 1;
%end

gplot(adj ,coordinates');
%title('Genetic');
%daspect([1 1 1]);