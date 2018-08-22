clear;
clc;
numOfLocations = 100;
numOfMaxRoutes = numOfLocations;
%We need to determine a radius and angle for each node
networkRadius = 25000; %in meters
vehicleCapacity = 250;
%First row is for radius, second is for angle, it will change soon!
coordinates = zeros(2, numOfLocations);
coordinates(1, 2:numOfLocations) = rand(1, numOfLocations - 1) * networkRadius;
coordinates(2, 2:numOfLocations) = rand(1, numOfLocations - 1) * 2 * pi;
distance = zeros(numOfLocations, numOfLocations); %in meters
demand = (rand(numOfLocations, 1) * 10) + (10 * ones(numOfLocations, 1));
demand(1) = 0;
for k=1:numOfLocations
    coordinates(:,k) = [coordinates(1,k) * cos(coordinates(2,k)) ; coordinates(1, k) * sin(coordinates(2, k))];
end
clear k;
%First row is for x coordinate, second row is for y coordinate

for i=1:numOfLocations
    for j=i+1:numOfLocations
        distance(i, j) = sqrt((coordinates(1, i) - coordinates(1, j))^2 + (coordinates(2, i) - coordinates(2,j))^2);;
    end
end
clear i j;
distance = distance + distance';

fileID = fopen('input.txt','w');

fprintf(fileID, '%d %d\r\n', numOfLocations, vehicleCapacity);

for i=1:numOfLocations-1
    for j=i+1:numOfLocations
        fprintf(fileID, '%d %d %f', i, j, distance(i, j));
        fprintf(fileID, '\r\n');
    end
end

for i=1:numOfLocations
    fprintf(fileID, '%f ', demand(i));
end

fclose(fileID);
system('java -jar ClarkAndWright.jar');
formatSpec = '%d';
sizeA = [numOfLocations numOfLocations];
fileID = fopen('output.txt','r');
A = fscanf(fileID,formatSpec, sizeA);
B = fscanf(fileID,'%f', [1 1]);
fclose(fileID);
G = graph(A);
G.Nodes.Size = demand;
%plot(G, 'XData', coordinates(1, :), 'YData', coordinates(2 , :), 'NodeLabel', G.Nodes.Size);
plot(G, 'XData', coordinates(1, :), 'YData', coordinates(2 , :));
title( strcat('Objective :', num2str(B)));