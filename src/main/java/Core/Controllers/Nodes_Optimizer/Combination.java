package Core.Controllers.Nodes_Optimizer;

// Java program to get all combination of size r in an array of size n

import java.util.ArrayList;
import java.util.Collections;

class Combination {

    private static ArrayList<ArrayList<Integer>> all_combinations = new ArrayList<>();
    private static ArrayList<Integer> current_temp_combination = new ArrayList<>();

    /* arr[]  ---> Input Array
    data[] ---> Temporary array to store current combination
    start & end ---> Staring and Ending indexes in arr[]
    index  ---> Current index in data[]
    r ---> Size of a combination to get */
    private static void combinationUtil(int arr[], int data[], int start,
                                        int end, int index, int r) {
        if (index == r) {
            for (int j = 0; j < r; j++) {
                current_temp_combination.add(data[j]);
            }
            all_combinations.add(current_temp_combination);
            current_temp_combination = new ArrayList<>();
            return;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr[i];
            combinationUtil(arr, data, i + 1, end, index + 1, r);
        }
    }

    // The main function that gets all combinations of size r
    // in arr[] of size n. This function mainly uses combinationUtil()
    static ArrayList<ArrayList<Integer>> getCombination(int arr[], int n, int r) {
        all_combinations = new ArrayList<>();
        // A temporary array to store all combination one by one
        int[] data = new int[r];
        // Get all combination using temporary array 'data[]'
        combinationUtil(arr, data, 0, n - 1, 0, r);

        // shuffle
        Collections.shuffle(all_combinations);

        return all_combinations;
    }
}