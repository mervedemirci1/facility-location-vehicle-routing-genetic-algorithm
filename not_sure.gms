sets
i facility locations

;

alias (i,j);
alias (i,l);

parameters
f(i) fixed cost of facility i
d(i) demand of location i
c(i,j) transportation cost i to location j;

scalar R 'number of depots';
scalar V 'vehicle capacity ';
scalar M 'big M' /500/;


$GDXIN 'input.gdx'
$LOAD i, f, c, R, d , V
$GDXIN


variables
objZ obj
n(i, j) 'flow sent from i to j'
rl(i) 'route length of i, 0 if i is not a route starter';

binary variable Fa(i) '1 if it is opened 0 otherwise';
binary variable Ge(i,j) '1 if location i allocated to j to be served by';
binary variable z(i) '1 if customer i is route starter 0 ow'
binary variable q(i, j) '1 if customer i is on the route of customer j';
binary variable arc(i, j)  '1 if there is an arc between i and j, 0 ow';


equations

obj                     'objective func'
const1                  'number of facilities'
const2(i)               'each customer has its supplier'
const3(i, j)    'if there is no depot there it cannot be allocated'
const4(i, j)    'if customer j is not a route starter, i cannot be allocated to j'
const5(j)               'route has a demand capacity'
const6(j)               'each route starter assigned to itself'
const7(i)               'each customer besides depots should be in a route'
const7_2(i)             'continue of const7'
const8(i)               'calculate route length for each route starter'

const9(i, j)    'flow between depot i and seed j'
const10(i, j)   'continue of const9'

const11(i, j)   'if j does not belong to i no flow'
const12(i, j)   'continue of const11'

*const13(i)      'basic flow balance where i is not a depot'
*const14(i)      'continue of const13'
const15(i, j)   'non-negative flow balance'
*arcolabilmesii(i,j) 'arc oluþabilmesi için orada q(i,j) baðlantýsý olmalý, ayný route'
const16(i, j)   'if there is a flow, there is an arc'
const17(i, j)   'if i is a depot, and j is its seed, then i is in the route of j'
const18(i, j)   'continue of const17'
*const19(i, j,l) 'if i and j is not on the same route there is no flow between them'
*const20(i, j,l) 'continue of const19'
const21(i)       'if i is depot, it cannot be route starter'
;


obj                                     .. objZ =e= sum(i,sum(j, c(i,j)*Ge(i,j))) + sum(i,f(i)*Fa(i));
const1                                  .. sum(i,Fa(i)) =e= R;

const2(i)                               .. sum(j,Ge(i,j))=e= 1 - Fa(i);


const3(i, j)                            .. Ge(i,j) =l= Fa(j);
const4(i, j)                            .. q(i, j) =l= z(j);
const5(j)                               .. sum(i, q(i, j) * d(i)) =l= V;
const6(j)                               .. q(j, j) =e= z(j);

const7(i)                               .. sum(j, q(i, j)) =l= 1 + M * Fa(i);
const7_2(i)                             .. sum(j, q(i, j)) + M * Fa(i) =g= 1;

const8(i)                               .. sum(j, q(j, i)) =e= rl(i);


const9(i, j)                    .. n(i, j) =l= rl(j) + M * (3 - (Fa(i) + Z(j) + Ge(j, i)));
const10(i, j)                   .. n(i, j) + M * (3 - (Fa(i) + Z(j) + Ge(j, i))) =g= rl(j);


const11(i, j)                   .. n(i, j) =l= M * (Z(j) + Ge(j, i) - 1);
const12(i, j)                   .. n(i, j) + M * (Z(j) + Ge(j, i) - 1) =g= 0;

*const13(i)                              .. sum(j, n(j,i)) - sum(j, n(i, j)) =l= 1 + M * Fa(i);
*const14(i)                              .. sum(j, n(j,i)) - sum(j, n(i, j)) + M * Fa(i) =g= 1;
const15(i, j)                   .. n(i, j) =g= 0;

*arcolabilmesii(i,j)              .. arc(i,j)=l= q(i,j);

const16(i, j)                   .. n(i, j) =l= M * arc(i, j);


const17(i, j)                   .. q(i, j) =l= 1 + M * (3 - Fa(i) - Ge(j, i) - z(j));
const18(i, j)                   .. q(i, j) + M * (3 - Fa(i) - Ge(j, i) - z(j)) =g= 1;
*const19(i, j,l)                .. n(i, j) =l= M * (2 - q(i, l) - q(j, l));
*const20(i, j,l)                .. n(i, j) + M * (2 - q(i, l) - q(j, l)) =g= 0;
const21(i)                      .. z(i) =l= 1 - Fa(i);


Model m3 /All/;
Solve m3 using MIP minimizing objZ;


execute_unload 'output'
