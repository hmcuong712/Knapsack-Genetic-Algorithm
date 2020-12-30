import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.stream.DoubleStream;

public class Knapsack {

    public KnapsackObject[] aKnap;
    private final double[] value;
    private final double[] cost;
    private final double capacity;
    ArrayList<ArrayList<Integer>> pop;
    private final double sumCost;
    private final int lambda;
    private final int k;
    private final double alpha;

    /**
    * Initializes the Knapsack problem with random knapsack objects. The objects are stored in an array for future reference.

    * @param seed the number of objects
    * @param lambda the number of population
    * @param k use in k-tournament selection
    * @param alpha the probability of an object being mutated
     * @throws IllegalArgumentException if {@code seed, lambda, k, alpha} is undesirable
     **/
    public Knapsack(int seed, int lambda, int k, double alpha){
        if (seed <= 0 || lambda <= 0 || k <= 0 || alpha < 0)
            throw new IllegalArgumentException("The parameters (except alpha) must be positive");

        value = new double[seed];
        cost = new double[seed];
        KnapsackObject knapObj;
        this.lambda = lambda;
        this.k = k;
        this.alpha = alpha;

        aKnap = new KnapsackObject[seed];
        for (int i = 0; i < seed; i++) {
            knapObj = new KnapsackObject(seed);
            knapObj.id = i;
            aKnap[i] = knapObj;
            value[i] = knapObj.value;
            cost[i] = knapObj.cost;
        }

        sumCost = DoubleStream.of(cost).sum();
        capacity = 0.25 * sumCost;
    }

    /**
     * Based on the Knapsack problem, generate a solution that has the highest objective value and meets the constraint.
     *
     * @return a solution
     */
    public ArrayList<Integer> generateSolution() {
        ArrayList<Integer> temp;
        ArrayList<ArrayList<Integer>> selected;
        ArrayList<ArrayList<Integer>> crossover = new ArrayList<ArrayList<Integer>>();
        int count = 0;

        // initialize the population
        getPopulation();

        for (int i = 0; i < 40; i++) {
            // select from the population
            selected = selection(k);
//            count = 0;
//            // perform crossover and mutate the child
//            while (count < selected.size()) {
//                temp = new ArrayList<Integer>();
//                temp = recombination(selected.get(count), selected.get(count + 1));
//                mutate(temp, 0.05);
//                crossover.add(temp);
//                count += 2;
//            }
            recomMutate(selected, crossover);

            // merge both population and children, and perform elimination
            pop.addAll(crossover);
            pop = elimination(pop);
            Collections.sort(pop, compValue());
//            System.out.println("Total value is: " + fitness(pop.get(lambda - 1)) + " Capacity is :" + cost(pop.get(lambda-1)));
        }
        return pop.get(lambda - 1);
    }

    /**
     * Based on the Knapsack objects, initialize a population as a first step in the evolutionary algorithm. The representation is permutation.
     */
    public void getPopulation() {
        ArrayList<Integer> init = new ArrayList<Integer>(aKnap.length);
        ArrayList<Integer> initCopy = new ArrayList<Integer>(aKnap.length);
        double totalCost;
        int j = 0;

        pop = new ArrayList<ArrayList<Integer>>(lambda);

        for (int i = 0; i < aKnap.length; i++) {
            init.add(i);
        }

        for (int i = 0; i < lambda; i++) {
            totalCost = 0;
            j = 0;
            initCopy = new ArrayList<Integer>(aKnap.length);
            KnapsackObject knap;

            Collections.shuffle(init);

            while (totalCost + aKnap[init.get(j)].cost < capacity) {
                initCopy.add(init.get(j));
                totalCost += aKnap[init.get(j)].cost;
                j++;
            }
            pop.add(initCopy);
        }
    }

    /**
     * Selection of individuals from the population based on the fitness-based competition.
     *
     * @param k: Number of competitors in the k-tournament selection
     * @throws IllegalArgumentException if {@code k} is non-positive
     * @return an extra population with size 2*lambda
     */
    // Now apply EA Algorithm, first selection WITH replacement
    public ArrayList<ArrayList<Integer>> selection(int k) {
        if (k <= 0) throw new IllegalArgumentException();

        int ind;
        int champion = 0;
        double max = 0;
        double val;
        ArrayList<ArrayList<Integer>> selected = new ArrayList<ArrayList<Integer>>();

        for (int i = 0; i < lambda * 2; i++) {
            // organize the k-tournament
            for (int j = 0; j < k; j++) {
                ind = getRandomNumberInRange(0, lambda - 1);
                val = fitness(pop.get(ind));
                if (val > max) {
                    max = val;
                    champion = ind;
                }
            }

            // add the champion to the 'selected' arraylist
            selected.add(pop.get(champion));
        }
        return selected;
    }

    /**
     * Based on the subset representation, overlap recombination is used (slides 35 session 2)
     *
     * @param dad first 'parent'
     * @param mom second 'parent'
     * @throws IllegalArgumentException if a parent is missing
     * @return offspring that is likely to have better properties than its parents
     */
    public ArrayList<Integer> recombination(ArrayList<Integer> dad, ArrayList<Integer> mom) {
        if (dad == null || mom == null) throw new IllegalArgumentException("A parent(s) is missing!");

        ArrayList<Integer> parent = new ArrayList<Integer>();
        ArrayList<Integer> child = new ArrayList<Integer>();
        parent.addAll(dad);
        parent.addAll(mom);
        Collections.sort(parent);

        int pros;
        int pros2;

        int i = 0;
        while (i < parent.size() - 1) {
            pros = parent.get(i);
            pros2 = parent.get(i+1);
            if (pros == pros2) {
                child.add(pros);
                i += 2;
            }
            else if (Math.random() < 0.5) {
                child.add(pros);
                i++;
            }
        }
        return child;
    }

    /**
     * Resetting mutation:
     * To maintain the stochastic property of the algorithm, an offspring is mutated with a probability of {@code p}.
     * If it is mutate, a Knapsack object in the offspring will be replaced by another random Knapsack object.
     *
     * @param candidate the offspring
     * @param p probability of being mutated
     * @throws IllegalArgumentException if argument is null
     */
    public void mutate(ArrayList<Integer> candidate, double p) {
        if (candidate == null )throw new IllegalArgumentException("The offspring is missing");

        int mutate;
        for (int i = 0; i < candidate.size(); i++)
            if (Math.random() < p) {
                mutate = getRandomNumberInRange(0, aKnap.length - 1);
                while (candidate.contains(mutate)) {
                    mutate = getRandomNumberInRange(0, aKnap.length - 1);
                }
                candidate.set(i, mutate);
            }
    }

    /**
     * Perform recombination and mutation sequentially at every iteration of the algorithm.
     *
     * @param src selected individuals, with size 2*lambda
     * @param dest offsprings, with size lambda
     * @throws IllegalArgumentException if either {@code src} or {@code dest} is null
     */
    public void recomMutate(ArrayList<ArrayList<Integer>> src, ArrayList<ArrayList<Integer>> dest) {
        if (src == null || dest == null) throw new IllegalArgumentException("Must provide both selected and offspring population");
        int count = 0;
        ArrayList<Integer> temp;

        while (count < lambda) {
            temp = new ArrayList<Integer>();
            temp = recombination(src.get(count), src.get(count + 1));
            mutate(temp, alpha);
            dest.add(temp);
            count += 2;
        }
    }

    /**
     * After joining both crossover and the seed population, this method eliminate the worst performing individuals in the population.
     *
     * @param popMerged joined population with size 2*lambda
     * @throws IllegalArgumentException if joined population is not provided
     * @return a new, supposedly better population with size lambda
     */
    public ArrayList<ArrayList<Integer>> elimination(ArrayList<ArrayList<Integer>> popMerged) {
        int count = 0;
        ArrayList<ArrayList<Integer>> popNew = new ArrayList<ArrayList<Integer>>();
        Collections.sort(popMerged, compValue());
        Collections.reverse(popMerged);
        for (ArrayList<Integer> sol : popMerged) {
            if (cost(sol) < capacity) {
                popNew.add(sol);
                count++;
            }
            if (count == lambda) break;
        }
        return popNew;
    }

    /**
     * A comparator that helps sort the individuals in the population based on their objective values
     *
     * @return a {@code Comparator} that can be used in Collections.sort()
     */
    private Comparator<ArrayList<Integer>> compValue() {
        Comparator<ArrayList<Integer>> comp = new Comparator<ArrayList<Integer>>() {

            @Override
            public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
                return compareTo(o1, o2);
            }

            public int compareTo(ArrayList<Integer> o1, ArrayList<Integer> o2) {
                double valFirst = fitness(o1);
                double valSecond = fitness(o2);
                if (valFirst < valSecond) return -1;
                else if (valFirst == valSecond) return 0;
                else return 1;
            }
        };
        return comp;
    }

    /**
     * Calculate the objective value of an individual by a loop.
     *
     * @param q individual
     * @throws IllegalArgumentException if individual is null
     * @return objective (or fitness) value for an individual
     */
    public double fitness(ArrayList<Integer> q) {
        if (q == null) throw new IllegalArgumentException("Individual is null");

        double fitness = 0;
        for (Integer i : q) {
            fitness += aKnap[i].value;
        }
        return fitness;
    }

    /**
     * Calculate the cost of an individual by a loop.
     *
     * @param q individual
     * @throws IllegalArgumentException if individual is null
     * @return cost of an individual
     */
    public double cost(ArrayList<Integer> q) {
        if (q == null) throw new IllegalArgumentException("Individual is null");

        double cost = 0;
        for (Integer i : q) {
            cost += aKnap[i].cost;
        }
        return cost;
    }

    /**
     * Random number generator (inclusive)
     *
     * @return a number that lies within the range of {@code min} and {@code max}
     */
    private static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static void main(String[] args) {
        int nObject = 100;
        int lambda = 200;
        int k = 3;
        double alpha = 0.05;

        Knapsack test = new Knapsack(nObject, lambda, k, alpha);
        System.out.println("Capacity is " + test.capacity);
        test.generateSolution();
        System.out.println("Final solution contains the following objects " + test.pop.get(lambda - 1));
    }
}
