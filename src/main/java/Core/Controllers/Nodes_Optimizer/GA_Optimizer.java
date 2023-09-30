package Core.Controllers.Nodes_Optimizer;

import Core.Controllers.GRID_Core.Cell;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static Core.Controllers.Nodes_Optimizer.Nodes_Optimizer_Controller.stop_localization_optimization_process;

class GA_Optimizer {

    static long current_generation = 0;
    private static int mutation_rate;
    private static final int pairs_of_parents_for_breeding = 50;
    private static final int parents_for_breeding = pairs_of_parents_for_breeding*2;
    static final int population_size = (parents_for_breeding*2)+1+4; // 2:To be able to purge 2, 1:Elite and 4:Random
    static final int percentage = 5;
    static int gene_length;
    static int setup_size;
    static ArrayList<Cell> border_cells;
    private static ArrayList<int[]> interconnections;
    private static int amount_of_weakest_outliers;
    static HashMap<Integer, Beacon> nodeID_to_nodeObject;
    private static ArrayList<Cell> all_cells_list;
    Population population = new Population("geom_mean");

    GA_Optimizer(ArrayList<Cell> border_cells, ArrayList<int[]> interconnections,
                 HashMap<Integer, Beacon> nodeID_to_nodeObject, ArrayList<Cell> all_cells_list){
        GA_Optimizer.border_cells = border_cells;
        GA_Optimizer.gene_length = border_cells.size();
        GA_Optimizer.interconnections = interconnections;
        GA_Optimizer.amount_of_weakest_outliers = ((percentage*GA_Optimizer.interconnections.size())/100);
        GA_Optimizer.nodeID_to_nodeObject = nodeID_to_nodeObject;
        GA_Optimizer.all_cells_list = all_cells_list;
    }

    GA_Optimizer(Integer setup_size, ArrayList<Cell> border_cells, ArrayList<int[]> interconnections,
                 HashMap<Integer, Beacon> nodeID_to_nodeObject, int mutation_rate, ArrayList<Cell> all_cells_list) {
        GA_Optimizer.setup_size = setup_size;
        GA_Optimizer.border_cells = border_cells;
        GA_Optimizer.gene_length = border_cells.size();
        GA_Optimizer.interconnections = interconnections;
        GA_Optimizer.amount_of_weakest_outliers = ((percentage*GA_Optimizer.interconnections.size())/100);
        GA_Optimizer.nodeID_to_nodeObject = nodeID_to_nodeObject;
        GA_Optimizer.mutation_rate = mutation_rate;
        GA_Optimizer.all_cells_list = all_cells_list;
    }

    void start() {

        current_generation = 0;

        //Initialize population
        population.initializePopulation();

        //Calculate fitness of each individual
        population.updateFittest();

        //While population gets an individual with maximum fitness
        while (!stop_localization_optimization_process) {

            current_generation++;

            // Get current state of the population
            //population.get_state("Intro");

            //Do selection (get the Individuals being sorted by their performances)
            int[][] individual_indices = selection();

            // Use only for debugging
            //report_rating(individual_indices);

            //Do crossover
            crossover(individual_indices);

            //Calculate new fitness value
            population.updateFittest();
        }
    }

    private void report_rating(int[][] individual_indices){
        System.out.println("Reporting Rating");
        System.out.println("=========");

        String report = "";

        for (int[] individual: individual_indices){
            if (individual!= null){
                report += Arrays.toString(individual);
                report += " ";
            }
        }

        System.out.println(report);

        System.out.println("=========");
    }

    //Selection
    private int[][] selection() {

        double[] individual_scores = new double[GA_Optimizer.population_size];
        int[] sorted_individual_indices = new int[GA_Optimizer.population_size];

        //Prepare the reversely sorted version of the arrays
        for (int individual_iterator = 0; individual_iterator < GA_Optimizer.population_size; individual_iterator++) {
            sorted_individual_indices[individual_iterator] = individual_iterator;
            individual_scores[individual_iterator] = population.individuals[individual_iterator].performance_measurement;
        }

        // Sort the individuals depending on their performance
        quicksort(individual_scores, sorted_individual_indices);

        // This array will hold the fittest parents_for_breeding
        int[][] fittest_individuals = new int[parents_for_breeding][];
        for (int i=0; i < parents_for_breeding; i++){
            fittest_individuals[i] = population.individuals[sorted_individual_indices[GA_Optimizer.population_size-i-1]]
                    .gene_sequence.clone();
        }

        // Now re-shuffle them
        //shuffleArray(fittest_individuals);

        return fittest_individuals;
    }

    private static void quicksort(double[] main, int[] index) {
        quicksort(main, index, 0, index.length - 1);
    }

    // quicksort a[left] to a[right]
    private static void quicksort(double[] values_array, int[] indices_array, int left, int right) {
        if (right <= left) return;
        int i = partition(values_array, indices_array, left, right);
        quicksort(values_array, indices_array, left, i-1);
        quicksort(values_array, indices_array, i+1, right);
    }

    // partition a[left] to a[right], assumes left < right
    private static int partition(double[] a, int[] index, int left, int right) {
        int i = left - 1;
        int j = right;

        while (true) {                          // find item on left to swap
            while (less(a[++i], a[right]));     // a[right] acts as sentinel
            while (less(a[right], a[--j]))      // find item on right to swap
                if (j == left) break;           // don't go out-of-bounds
            if (i >= j) break;                  // check if pointers cross
            exchange(a, index, i, j);           // swap two elements into place
        }
        exchange(a, index, i, right);           // swap with partition element
        return i;
    }

    // is x < y ?
    private static boolean less(double x, double y) {
        return (x < y);
    }

    // exchange a[i] and a[j]
    private static void exchange(double[] a, int[] index, int i, int j) {
        double swap = a[i];
        a[i] = a[j];
        a[j] = swap;
        int b = index[i];
        index[i] = index[j];
        index[j] = b;
    }

    //Crossover
    private void crossover(int[][] fittest_individuals) {

        // We pass directly the fittest Individual to the new generation as the Elite
        population.individuals[0].gene_sequence = population.best_genes.clone();
        population.individuals[0].performance_measurement = population.best_performance_found;
        for (int setup_iter = 0; setup_iter<population.best_node_setup_array.length(); setup_iter++){
            population.individuals[0].node_setup_array[setup_iter] = population.best_node_setup_array.get(setup_iter);
        }

        // We also create 2 totally random individuals to be always able to escape from local optima
        population.individuals[population_size-4].gene_sequence = GA_Optimizer.generate_random_gene_sequence();
        population.individuals[population_size-4].update_node_setup_array_and_performance_based_on_existing_genes();

        population.individuals[population_size-3].gene_sequence = GA_Optimizer.generate_random_gene_sequence();
        population.individuals[population_size-3].update_node_setup_array_and_performance_based_on_existing_genes();

        population.individuals[population_size-2].gene_sequence = GA_Optimizer.generate_random_gene_sequence();
        population.individuals[population_size-2].update_node_setup_array_and_performance_based_on_existing_genes();

        population.individuals[population_size-1].gene_sequence = GA_Optimizer.generate_random_gene_sequence();
        population.individuals[population_size-1].update_node_setup_array_and_performance_based_on_existing_genes();


        int individual_iterator = 1;

        for(int pair_iter = 0; pair_iter < pairs_of_parents_for_breeding; pair_iter = pair_iter+2){
            put_a_new_child(individual_iterator, fittest_individuals[pair_iter], fittest_individuals[pair_iter+1]);
            individual_iterator = individual_iterator + 4;
        }
    }


    private void put_a_new_child(int individual_position, int[] genes_of_parent_1, int[] genes_of_parent_2){

        Random rand = ThreadLocalRandom.current();

        //Swap values among parents
        for (int i = 0; i < GA_Optimizer.gene_length; i++) {
            // Use this for mutations
            if (rand.nextInt(mutation_rate)==0){
                population.individuals[individual_position].gene_sequence[i] = rand.nextInt(2);
            }
            else {
                // Check if the genes are different
                if (genes_of_parent_1[i] != genes_of_parent_2[i]){
                    population.individuals[individual_position].gene_sequence[i] = rand.nextInt(2);
                    population.individuals[individual_position+1].gene_sequence[i] = rand.nextInt(2);
                    population.individuals[individual_position+2].gene_sequence[i] = rand.nextInt(2);
                    population.individuals[individual_position+3].gene_sequence[i] = rand.nextInt(2);
                }
                else {
                    population.individuals[individual_position].gene_sequence[i] = genes_of_parent_1[i];
                    population.individuals[individual_position+1].gene_sequence[i] = genes_of_parent_1[i];
                    population.individuals[individual_position+2].gene_sequence[i] = genes_of_parent_1[i];
                    population.individuals[individual_position+3].gene_sequence[i] = genes_of_parent_1[i];
                }
            }
        }

        // We need to ensure that the size of the node setup is fixed and that the performance metrics are updated
        population.individuals[individual_position].validate();
        population.individuals[individual_position+1].validate();
        population.individuals[individual_position+2].validate();
        population.individuals[individual_position+3].validate();
    }

    private static void shuffleArray(int[][] ar) {
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int[] a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    private static int getRandomNumberInRange(int max) {
        Random r = new Random();
        return r.ints(0, (max + 1)).findFirst().getAsInt();
    }

    //Generate a random gene sequence
    static int[] generate_random_gene_sequence(){
        int[] new_gene_sequence = new int[GA_Optimizer.gene_length];

        int good_genes_injected = 0;
        while (good_genes_injected != GA_Optimizer.setup_size){
            int candidate_position_for_node = GA_Optimizer.getRandomNumberInRange(GA_Optimizer.gene_length-1);

            // Check first if a node has already been placed there
            if (new_gene_sequence[candidate_position_for_node] == 0){
                new_gene_sequence[candidate_position_for_node] = 1;
                good_genes_injected++;
            }
        }
        return new_gene_sequence;
    }

    private static double get_squared_distance_between_vectors(double[] rss_vector_A, double[] rss_vector_B){
        double result = 0;
        for (int component_iterator = 0; component_iterator<rss_vector_A.length; component_iterator++){
            result = result + Math.pow(rss_vector_A[component_iterator]-rss_vector_B[component_iterator], 2);
        }

        return result;
    }

    static double get_minimum_separation_distance(ArrayList<Beacon> beacon_set){

        double temp_minimum_distance_found = Double.MAX_VALUE;

        for (int[] interconnection: GA_Optimizer.interconnections){
            double[] rss_vector_A = new double[beacon_set.size()];
            double[] rss_vector_B = new double[beacon_set.size()];

            // Get the nodes at current interconnection
            Node node_A = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[0]);
            Node node_B = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[1]);

            // Build the vectors
            for (int rss_vector_iterator=0; rss_vector_iterator<beacon_set.size(); rss_vector_iterator++){
                rss_vector_A[rss_vector_iterator] = node_A.cell_receptions_1D[beacon_set.get(rss_vector_iterator).nodeID];
                rss_vector_B[rss_vector_iterator] = node_B.cell_receptions_1D[beacon_set.get(rss_vector_iterator).nodeID];
            }

            double new_vector_distance = GA_Optimizer.get_squared_distance_between_vectors(rss_vector_A, rss_vector_B);

            if (new_vector_distance < temp_minimum_distance_found){
                temp_minimum_distance_found = new_vector_distance;
            }
        }
        return temp_minimum_distance_found;
    }

    static double get_minimum_separation_distance(int[] input_node_setup_array){

        double temp_minimum_distance_found = Double.MAX_VALUE;

        for (int[] interconnection: GA_Optimizer.interconnections){
            double[] rss_vector_A = new double[GA_Optimizer.setup_size];
            double[] rss_vector_B = new double[GA_Optimizer.setup_size];

            // Get the nodes at current interconnection
            Node node_A = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[0]);
            Node node_B = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[1]);

            // Build the vectors
            for (int rss_vector_iterator=0; rss_vector_iterator<GA_Optimizer.setup_size; rss_vector_iterator++){
                rss_vector_A[rss_vector_iterator] = node_A.cell_receptions_1D[input_node_setup_array[rss_vector_iterator]];
                rss_vector_B[rss_vector_iterator] = node_B.cell_receptions_1D[input_node_setup_array[rss_vector_iterator]];
            }

            double new_vector_distance = GA_Optimizer.get_squared_distance_between_vectors(rss_vector_A, rss_vector_B);

            if (new_vector_distance < temp_minimum_distance_found){
                temp_minimum_distance_found = new_vector_distance;
            }
        }
        return temp_minimum_distance_found;
    }

    static double get_product_performance(ArrayList<Beacon> beacon_set){

        ArrayList<Double> distances = new ArrayList<>();

        for (int[] interconnection: GA_Optimizer.interconnections){
            double[] rss_vector_A = new double[beacon_set.size()];
            double[] rss_vector_B = new double[beacon_set.size()];

            // Get the nodes at current interconnection
            Node node_A = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[0]);
            Node node_B = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[1]);

            // Build the vectors
            for (int rss_vector_iterator=0; rss_vector_iterator<beacon_set.size(); rss_vector_iterator++){
                rss_vector_A[rss_vector_iterator] = node_A.cell_receptions_1D[beacon_set.get(rss_vector_iterator).nodeID];
                rss_vector_B[rss_vector_iterator] = node_B.cell_receptions_1D[beacon_set.get(rss_vector_iterator).nodeID];
            }

            distances.add(GA_Optimizer.get_squared_distance_between_vectors(rss_vector_A, rss_vector_B));
        }
        return 0;
    }

    static double get_product_performance(int[] input_node_setup_array){

        ArrayList<Double> distances = new ArrayList<>();

        for (int[] interconnection: GA_Optimizer.interconnections){
            double[] rss_vector_A = new double[GA_Optimizer.setup_size];
            double[] rss_vector_B = new double[GA_Optimizer.setup_size];

            // Get the nodes at current interconnection
            Node node_A = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[0]);
            Node node_B = GA_Optimizer.nodeID_to_nodeObject.get(interconnection[1]);

            // Build the vectors
            for (int rss_vector_iterator=0; rss_vector_iterator<GA_Optimizer.setup_size; rss_vector_iterator++){
                rss_vector_A[rss_vector_iterator] = node_A.cell_receptions_1D[input_node_setup_array[rss_vector_iterator]];
                rss_vector_B[rss_vector_iterator] = node_B.cell_receptions_1D[input_node_setup_array[rss_vector_iterator]];
            }

            distances.add(GA_Optimizer.get_squared_distance_between_vectors(rss_vector_A, rss_vector_B));
            //product = product * Math.pow(Math.E, Math.log(GA_Optimizer.get_squared_distance_between_vectors(rss_vector_A, rss_vector_B))/2);
        }

        Collections.sort(distances);

        double product = 1;
        for (int index=0; index < GA_Optimizer.amount_of_weakest_outliers; index++){
            product = product*distances.get(index);
        }

        return product;
    }

    // Function for calculating variance
    private static double variance(double[] a, int n){
        // Compute mean (average of elements)
        double sum = 0;
        for (int i = 0; i < n; i++)
            sum += a[i];

        double mean = sum / (double)n;

        // Compute sum squared differences with mean.
        double sqDiff = 0;
        for (int i = 0; i < n; i++)
            sqDiff += (a[i] - mean) * (a[i] - mean);

        return sqDiff / n;
    }

    static int[] get_node_setup_array_from_gene_sequence(int[] input_gene_sequence){
        int[] temp_node_setup_array = new int[GA_Optimizer.setup_size];

        int node_index_in_node_setup_array = 0;

        // For every node object
        for (int gene_position=0; gene_position<GA_Optimizer.gene_length; gene_position++){
            if (input_gene_sequence[gene_position] == 1){
                temp_node_setup_array[node_index_in_node_setup_array] = GA_Optimizer.border_cells.get(gene_position).grid_index_int_1d;
                node_index_in_node_setup_array++;
            }
        }
        return temp_node_setup_array;
    }

    static int count_good_genes(int[] gene_sequence){
        int good_genes_counter = 0;
        for (int gene: gene_sequence){
            if (gene==1){
                good_genes_counter++;
            }
        }
        return good_genes_counter;
    }

    private static int[] get_gene_sequence_from_node_setup_array(int[] node_setup){

        int[] new_gene_sequence = new int[gene_length];

        //Iterate all the nodes and assign a good gene at the positions where they are placed
        for (int node_pos_in_node_setup = 0; node_pos_in_node_setup < setup_size; node_pos_in_node_setup++){
            int nodeID = GA_Optimizer.nodeID_to_nodeObject.get(node_setup[node_pos_in_node_setup]).nodeID;
            new_gene_sequence[GA_Optimizer.border_cells.indexOf(GA_Optimizer.all_cells_list.get(nodeID))] = 1;
        }

        return new_gene_sequence;
    }
}

//Population class
class Population {

    Individual[] individuals = new Individual[GA_Optimizer.population_size];
    double best_performance_found;
    int[] best_genes;
    AtomicIntegerArray best_node_setup_array;

    Population(String metric){
        if (metric.equals("geom_mean")){
            best_performance_found = Double.MIN_VALUE;
        }
        else{
            best_performance_found = Double.MIN_VALUE;
        }
    }

    //Initialize population
    void initializePopulation() {
        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = new Individual();
        }
    }

    //Get the fittest individual
    void updateFittest() {
        for (Individual individual : individuals) {
            if (individual.performance_measurement > best_performance_found) { //todo change that according to the metric
                best_performance_found = individual.performance_measurement;
                best_node_setup_array = new AtomicIntegerArray(individual.node_setup_array);
                best_genes = individual.gene_sequence.clone();
            }
        }
    }

    void get_state(String msg){

        System.out.println("Individuals:");
        System.out.println("============" + " " + msg);

        int individual_position = 0;
        for (Individual individual: individuals){
            System.out.println(individual_position + ": " + individual.get_report());
            individual_position++;
        }
        System.out.println("============");
    }

}

//Individual class
class Individual {

    double performance_measurement;
    int[] gene_sequence;
    int[] node_setup_array = new int[GA_Optimizer.setup_size];

    Individual() {
        gene_sequence = GA_Optimizer.generate_random_gene_sequence();
        update_node_setup_array_and_performance_based_on_existing_genes();
    }

    String get_report() {

        String message = "";

        // Check if current
        if (!genes_match_node_setup()){
            message = "Warning! ";
        }

        message += "Distance: " + performance_measurement + " Nodes: " + Arrays.toString(node_setup_array);

        return message;
    }

    private boolean genes_match_node_setup(){
        int node_index_in_node_setup_array = 0;

        for (int gene_position = 0; gene_position< gene_sequence.length; gene_position++){
            if (gene_sequence[gene_position] == 1){
                Node simulated_node = GA_Optimizer.nodeID_to_nodeObject.get(GA_Optimizer.border_cells.get(gene_position).grid_index_int_1d);

                if (node_setup_array[node_index_in_node_setup_array] != simulated_node.nodeID){
                    System.out.println(" ");
                    System.out.println("Report of wrong Genes to Node Setup: ");
                    System.out.println("Gene sequence: " + Arrays.toString(gene_sequence));
                    System.out.println("Nodes: " + Arrays.toString(node_setup_array));
                    System.out.println("Current position of good gene: " + gene_position);
                    System.out.println("Cell index where this gene corresponds: " + GA_Optimizer.border_cells.get(gene_position).grid_index_int_1d);
                    System.out.println("ID of the Node that is in this Cell: " + simulated_node.nodeID);
                    return false;
                };
                node_index_in_node_setup_array++;
            }
        }

        return true;
    }

    //Calculate fitness
    void update_node_setup_array_and_performance_based_on_existing_genes() {
        int[] new_node_setup_array = GA_Optimizer.get_node_setup_array_from_gene_sequence(gene_sequence);
        node_setup_array = new_node_setup_array;
        performance_measurement = GA_Optimizer.get_minimum_separation_distance(new_node_setup_array);
        //performance_measurement = GA_Optimizer.get_product_performance(new_node_setup_array); //todo change according
    }

    void validate(){
        Random rn = new Random();

        int nodes_found = 0;
        for (int node_position=0; node_position<GA_Optimizer.gene_length; node_position++){
            nodes_found += gene_sequence[node_position];
        }

        // Check if we are missing nodes
        if (nodes_found < GA_Optimizer.setup_size){
            // In this case, add the corresponding nodes
            while(nodes_found != GA_Optimizer.setup_size){
                //Select a random crossover point
                int mutationPoint = rn.nextInt(GA_Optimizer.gene_length);
                if (gene_sequence[mutationPoint] == 0){
                    gene_sequence[mutationPoint] = 1;
                    nodes_found++;
                }
            }
        }
        // Or check if we have more nodes than we should
        else if (nodes_found > GA_Optimizer.setup_size){
            // In this case, remove the corresponding nodes
            while(nodes_found != GA_Optimizer.setup_size){
                //Select a random crossover point
                int mutationPoint = rn.nextInt(GA_Optimizer.gene_length);
                if (gene_sequence[mutationPoint] == 1){
                    gene_sequence[mutationPoint] = 0;
                    nodes_found--;
                }
            }
        }

        update_node_setup_array_and_performance_based_on_existing_genes();
    }
}