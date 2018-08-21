clear;
clc;
numOfLocations = 1000;
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

gravity = [0 0]';
sumDemand = 0;
fileID = fopen('input.txt','w');
fprintf(fileID, '%d %d %d\r\n', numOfLocations, numOfDepots, vehicleCapacity);
for i=1:numOfLocations
    fprintf(fileID, '%f %f %f ', coordinates(1,i), coordinates(2,i), demand(i));
end
fprintf(fileID, '\r\n');
for i=1:numOfLocations-1
   for j=i+1:numOfLocations
    fprintf(fileID, '%f\r\n', distance(i, j));
   end
end

fclose(fileID);

system('java -jar bottik.jar');

sizeA = [1 numOfLocations];
sizeB = [2 numOfDepots];
sizeC = [numOfLocations numOfLocations];
fileID = fopen('output.txt','r');
A = fscanf(fileID, '%d', sizeA);
B = fscanf(fileID, '%f', sizeB);
C = fscanf(fileID, '%d', sizeC);
fclose(fileID);

arbitrary2 = ones(3, 3) - eye(3,3);
hold on;
G = graph(C);
G.Nodes.Size = A';

plot(G, 'XData', coordinates(1, :), 'YData', coordinates(2 , :));
daspect([1 1 1]);