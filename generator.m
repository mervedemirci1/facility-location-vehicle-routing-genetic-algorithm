clear;
clc;
numOfLocations = 10;
numOfMaxRoutes = numOfLocations;
%We need to determine a radius and angle for each node
networkRadius = 10000; %in meters
vehicleCapacity = 10000;
%First row is for radius, second is for angle, it will change soon!
coordinates = zeros(2, numOfLocations);
coordinates(1, 1:numOfLocations) = rand(1, numOfLocations) * networkRadius;
coordinates(2, 1:numOfLocations) = rand(1, numOfLocations) * 2 * pi;
distance = zeros(numOfLocations, numOfLocations); %in meters
for k=1:numOfLocations
    coordinates(:,k) = [coordinates(1,k) * cos(coordinates(2,k)) ; coordinates(1, k) * sin(coordinates(2, k))];
end
clear k;

plant = zeros(2,1);

plantDist = zeros(numOfLocations, 1);

%First row is for x coordinate, second row is for y coordinate
for i=1:numOfLocations
    plantDist(i, 1) = sqrt((plant(1) - coordinates(1, i)) ^ 2 + (plant(2) - coordinates(2, i)) ^ 2);
    for j=i+1:numOfLocations
        dij = sqrt((coordinates(1, i) - coordinates(1, j))^2 + (coordinates(2, i) - coordinates(2,j))^2);
        distance(i, j) = dij;
    end
end
clear i j dij;
distance = distance + distance';

%demand = rand(numOfLocations, 1) * 100;
demand = ones(numOfLocations, 1) * 100;

cost = distance / 100;
fixedCost = rand(numOfLocations , 1) * 60 + ones(numOfLocations, 1) * 90;

cost = cost + cost';

delete 'input.gdx'

nodes.name = 'i';
nodes.uels = {1: numOfLocations};

fixedCostPar.name = 'f';
fixedCostPar.type = 'parameter';
fixedCostPar.val = fixedCost;
fixedCostPar.dim = 1;
fixedCostPar.form = 'full';

costPar.name = 'c';
costPar.type = 'parameter';
costPar.val = cost;
costPar.dim = 2;
costPar.form = 'full';

numOfFacPar.name = 'numDepot';
numOfFacPar.type = 'parameter';
numOfFacPar.val = 3;
numOfFacPar.dim = 0;
numOfFacPar.form = 'full';

fixedIJPar.name = 'rent';
fixedIJPar.type = 'parameter';
fixedIJPar.val = cost * 100;
fixedIJPar.dim = 2;
fixedIJPar.form = 'full';


demandPar.name = 'd';
demandPar.type = 'parameter';
demandPar.val = demand;
demandPar.dim = 1;
demandPar.form = 'full';


vehicleCapPar.name = 'V';
vehicleCapPar.type = 'parameter';
vehicleCapPar.val = vehicleCapacity;
vehicleCapPar.dim = 0;
vehicleCapPar.form = 'full';

numOfMaxRoutesPar.name = 'r';
numOfMaxRoutesPar.uels = {1: numOfLocations};

plantDistPar.name = 'pdis';
plantDistPar.type = 'parameter';
plantDistPar.val = plantDist / 100;
plantDistPar.dim = 1;
plantDistPar.form = 'full';

wgdx('input',plantDistPar, nodes, numOfMaxRoutesPar, fixedCostPar, costPar, numOfFacPar, demandPar, vehicleCapPar, fixedIJPar);
clear plantDistPar nodes numOfMaxRoutesPar fixedCostPar costPar numOfFacPar demandPar vehicleCapPar fixedIJPar;
gams('model.gms')

%read.name = 'n';
%read.form = 'full';
%a = rgdx('output', read);
%G1 = digraph(a.val);

read.name = 'arc';
read.form = 'full';
a = rgdx('output', read);
G2 = digraph(a.val);



%hold on;
%plot(G1, 'XData', coordinates(1, :), 'YData', coordinates(2 , :));
plot(G2, 'XData', coordinates(1, :), 'YData', coordinates(2 , :));
