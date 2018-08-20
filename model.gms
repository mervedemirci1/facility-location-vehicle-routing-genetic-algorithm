Sets
i locations
r routes;

alias(i, j);
alias(r, r2);

Parameters
d(i) demand i
f(i) fixed cost of opening a depot on i
c(i, j) per unit cost of transportation i j
pdis(i) per unit cost of sending to i from plant
rent(i, j) rent cost of i to j
V capacity of route
numDepot number of depots to  open
M a sufficiently big number /50000/;

Variables
obj objective function val
flow(i, j) amount of flow from i to j
sn(i) amount of flow from super node to node i;

$GDXIN 'input.gdx'
$LOAD i, r, f, c, d, numDepot , V, rent, pdis
$GDXIN



Binary variable fa(i) 1 if depot should be opened 0 ow;
Binary variable ra(i,r) 1 if the location i is in the route r;
Binary variable un(i, r) linearization variable if i is the depot of the route r
Binary Variable arc(i, j) 1 if flow possible from i to j;
Binary variable rarc(i, j, r) 1 if flow possible from i to j on route r;
Binary variable ruse(r) 1 if the route r is used 0 ow;


Equations

objective                objective function
lin1(i, r)               linearization of variable un
lin2(i, r)               cont of lin1
routeDepot(r)            each used route can contain a single depot
routeUsed(i,r)             if r is not used it cannot be assigned
routeCapacity(r)         each route has a capacity
depotNumber              total number of depots
flowBalance(i)           flow balance constraint
superFlow(i)             super node only sends to depots
detRarc(i, j, r)         determine rarc
detArc(i, j)             determine arc
arcFlow(i, j)            if there is no arc there is no flow
depotLeaving(i)          determine the amount of arcs that are leaving depot
depotLeaving2(i)         determine the amount of arcs that are leaving depot
depotEntering(i)         determine the amount of arcs that are leaving depot
depotEntering2(i)        determine the amount of arcs that are leaving depot
nonDepotLeaving(i)       number of leaving arc is one
nonDepotEntering(i)      number of entering arc is one
flowNonNegative(i, j)    non-negativity constraint
nonDepotCustomers(i)     non-depot customers can only belong to one route
nonDepotCustomers2(i)    cont
*useOfRoutes(r, r2)       in order to use r2 r must be used

;


objective              .. obj =e= sum(i, fa(i) * f(i)) + sum(i, sum(j, flow(i, j) * c(i, j))) + sum(i, sum(j, arc(i, j) * rent(i, j))) + sum(i, pdis(i) * sn(i));
lin1(i, r)             .. un(i, r) =l= (fa(i) + ra(i, r)) / 2;
lin2(i, r)             .. un(i, r) =g= fa(i) + ra(i, r) - 1;
routeDepot(r)          .. sum(i, un(i, r)) =e= ruse(r);
routeUsed(i,r)         .. ra(i, r) =l= ruse(r);
routeCapacity(r)       .. sum(i, d(i) * (ra(i, r)- un(i,r))) =l= V;

depotNumber            .. sum(i, fa(i)) =e= numDepot ;

flowBalance(i)         .. sum(j, flow(j, i)) + sn(i) - sum(j, flow(i, j)) =e= d(i);
superFlow(i)           .. sn(i) =l= M * fa(i);
detRarc(i, j, r)       .. rarc(i, j, r) =l= (ra(i, r) + ra(j, r)) / 2;
detArc(i, j)           .. arc(i, j) =l= sum(r, rarc(i, j, r));
arcFlow(i, j)          .. flow(i, j) =l= M * arc(i, j);
depotLeaving(i)        .. sum(j, arc(i, j)) =l= sum(r, ra(i, r)) + M * (1 - fa(i));
depotLeaving2(i)       .. sum(j, arc(i, j)) + M * (1 - fa(i)) =g= sum(r, ra(i, r));

depotEntering(i)       .. sum(j, arc(j, i)) =l= sum(r, ra(i, r)) + M * (1 - fa(i));
depotEntering2(i)      .. sum(j, arc(j, i)) + M * (1 - fa(i)) =g= sum(r, ra(i, r));


nonDepotLeaving(i)     .. sum(j, arc(i, j)) =l= 1 + M * fa(i);
nonDepotEntering(i)    .. sum(j, arc(j, i)) =l= 1 + M * fa(i);


flowNonNegative(i, j)  .. flow(i, j) =g= 0;

nonDepotCustomers(i)   .. sum(r, ra(i, r)) =l= 1 + M * fa(i);
nonDepotCustomers2(i)  .. sum(r, ra(i, r)) + M * fa(i) =g= 1;

*useOfRoutes(r, r2) $(ord(r) eq ord(r2) - 1)     .. ruse(r2) =l= ruse(r);

Model m3 /All/;
Solve m3 using MIP minimizing obj;


execute_unload 'output'









