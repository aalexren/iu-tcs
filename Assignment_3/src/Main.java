import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, MyException {
	    Scanner scanner = new Scanner(new File("input.txt"));
        BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

	    String statesStr = scanner.nextLine();
	    String alphaStr = scanner.nextLine();
	    String initialStr = scanner.nextLine();
	    String acceptStr = scanner.nextLine();
        String transStr = scanner.nextLine();

        String[] states = null;
        String[] alpha = null;
        String[] initial = null;
        String[] accept = null;
        String[] trans = null;

        try {
            states = getStates(statesStr);
            alpha = getAlphas(alphaStr);
            initial = getInitial(initialStr, states);
            accept = getAccept(acceptStr, states);
            trans = getTransitions(transStr, alpha, states);
        }
        catch (MyException e) {
            writer.write("Error:\n");
            writer.append(e.getMessage());
            writer.close();
            return;
        }

        HashMap<String, Integer> state_index = new HashMap<>();
        HashMap<Integer, String> index_state = new HashMap<>();
        for (int i = 0; i < states.length; ++i) {
            state_index.put(states[i], i);
            index_state.put(i, states[i]);
        }

        try {
            checkDisjoint(states, trans, state_index);
        }
        catch (MyException e) {
            writer.write("Error:\n");
            writer.append(e.getMessage());
            writer.close();
            return;
        }

        try {
            checkNDFSA(trans);
        }
        catch(MyException e) {
            writer.write("Error:\n");
            writer.append(e.getMessage());
            writer.close();
            return;
        }

        if (accept.length == 0) {
            writer.write("{}");
            writer.close();
            return;
        }

        AdjacencyMatrixGraph<Integer, String> graph = makeGraph(states, trans, state_index);
        writer.write(makeRegex(graph, initial, accept, state_index, index_state));
//        System.out.println(makeRegex(graph, initial, accept, state_index, index_state));
//        graph.printEdges();

        writer.close();
    }

/*
       _.---._    /\\
    ./'       "--`\//
  ./              o \          .-----.
 /./\  )______   \__ \        ( help! )
./  / /\ \   | \ \  \ \       /`-----'
   / /  \ \  | |\ \  \7--- ooo ooo ooo ooo ooo ooo

 */

    public static String makeRegex(AdjacencyMatrixGraph<Integer, String> graph, String[] initial, String[] accept,
                                   HashMap<String, Integer> state_index, HashMap<Integer, String> index_state) {
        String[][][] R = new String[graph.matrix.size() + 1][graph.matrix.size()][graph.matrix.size()];

        for (int i = 0; i < R[0].length; ++i) {
            for (int j = 0; j < R[0].length; ++j) {
                if (graph.hasEdge(graph.findVertex(i), graph.findVertex(j))) {
                    StringBuilder sb = new StringBuilder();
                    for (String s: graph.findEdge(i, j).weights) {
                        sb.append(s).append("|");
                    }
                    sb.reverse().deleteCharAt(0).reverse(); // delete extra "|"
                    if (i == j) {
                        sb.append("|eps");
                    }
                    R[0][i][j] = sb.toString();
                }
                else {
                    if (i == j) {
                        R[0][i][j] = "eps";
                    }
                    else {
                        R[0][i][j] = "{}";
                    }
                }
            }
        }

        for (int k = 1; k < R.length; ++k) {
            for (int i = 0; i < R[k].length; ++i) {
                for (int j = 0; j < R[k][i].length; ++j) {
                    R[k][i][j] = "(" + R[k-1][i][k-1] + ")" + "(" + R[k-1][k-1][k-1] + ")*" + "(" + R[k-1][k-1][j] + ")|(" + R[k-1][i][j] + ")";
                }
            }
        }

        int start = state_index.get(initial[0]);
        int target;
        StringBuilder sb = new StringBuilder();

        for (String s: accept) {
            target = state_index.get(s);
            sb.append(R[graph.matrix.size()][start][target]).append("|");
        }
        sb.reverse().deleteCharAt(0).reverse(); // delete extra "|"

        return sb.toString();
    }

    public static AdjacencyMatrixGraph<Integer, String> makeGraph(String[] states, String[] trans,
                                                                  HashMap<String, Integer> state_index) {
        AdjacencyMatrixGraph<Integer, String> graph = new AdjacencyMatrixGraph<>();

        for (int i = 0; i < states.length; ++i) {
            graph.addVertex(i);
        }

        for (String s : trans) {
            String[] tran = s.split(">");
            Vertex<Integer> start = graph.findVertex(state_index.get(tran[0]));
            Vertex<Integer> end = graph.findVertex(state_index.get(tran[2]));
            graph.addEdge(start, end, tran[1]);
        }

        return graph;
    }

    public static void checkNDFSA(String[] trans) throws MyException {
        HashMap<String, HashSet<String>> ret = new HashMap<>();
        for (String s: trans) {
            String start_st = s.split(">")[0];
            String alpha = s.split(">")[1];
            String end_st = s.split(">")[2]; // useless
            // TODO there is possible to have 1>a>1,1>a>1 it should NOT be nondet...
            if (ret.get(start_st) == null) {
                ret.put(start_st, new HashSet<>());
                ret.get(start_st).add(alpha);
            } else if (ret.get(start_st).contains(alpha)){
                throw new MyException("E5: FSA is nondeterministic");
            } else {
                ret.get(start_st).add(alpha);
            }
        }
    }

    public static void checkDisjoint(String[] states, String[] trans,
                                           HashMap<String, Integer> state_index) throws MyException {
        AdjacencyMatrixGraph<Integer, String> graph = new AdjacencyMatrixGraph<>();

        for (int i = 0; i < states.length; ++i) {
            graph.addVertex(i);
        }

        for (String s : trans) {
            String[] tran = s.split(">");
            Vertex<Integer> start = graph.findVertex(state_index.get(tran[0]));
            Vertex<Integer> end = graph.findVertex(state_index.get(tran[2]));
            graph.addEdge(start, end, tran[1]);
            graph.addEdge(end, start, tran[1]);
        }

        if (!graph.isConected(graph.findVertex(state_index.get(states[0])))) {
            throw new MyException("E2: Some states are disjoint");
        }
    }

    public static String[] getStates(String s) throws MyException {
        if (s.startsWith("states=[") && s.endsWith("]")) {
            s = s.replace("states=[", "");
            s = s.replace("]", "");
        }
        else {
            throw new MyException("E0: Input file is malformed");
        }

        // TODO check if state is empty

        String[] states = s.split(",");
        for (String state: states) {
            if (!state.matches("[A-z0-9]+")) {
                throw new MyException("E0: Input file is malformed");
            }
        }

        return states;
    }

    public static String[] getAlphas(String s) throws MyException {
        if (s.startsWith("alpha=[") && s.endsWith("]")) {
            s = s.replace("alpha=[", "");
            s = s.replace("]", "");
        }
        else {
            throw new MyException("E0: Input file is malformed");
        }

        // TODO check if alpha is empty

        String[] alphas = s.split(",");
        for (String alpha: alphas) {
            if (!alpha.matches("[A-z0-9_]+")) {
                throw new MyException("E0: Input file is malformed");
            }
        }

        return alphas;
    }

    public static String[] getInitial(String s, String[] states) throws MyException {
        if (s.startsWith("initial=[") && s.endsWith("]")) {
            s = s.replace("initial=[", "");
            s = s.replace("]", "");
        }
        else {
            throw new MyException("E0: Input file is malformed");
        }

        String initial = s;
        if (s.isEmpty()) {
            throw new MyException("E4: Initial state is not defined");
        }
        if (!Arrays.asList(states).contains(initial)) {
            throw new MyException(String.format("E1: A state '%s' is not in the set of states", s));
        }

        return new String[] {initial};
    }

    public static String[] getAccept(String s, String[] states) throws MyException {
        if (s.startsWith("accepting=[") && s.endsWith("]")) {
            s = s.replace("accepting=[", "");
            s = s.replace("]", "");
        }
        else {
            throw new MyException("E0: Input file is malformed");
        }

        if (s.isEmpty()) return new String[]{};

        String[] accepting = s.split(",");
        for (String accept: accepting) {
            if (!accept.matches("[A-z0-9_]+")) {
                throw new MyException("E0: Input file is malformed");
            }
            if (!Arrays.asList(states).contains(accept)) {
                throw new MyException(String.format("E1: A state '%s' is not in the set of states", accept));
            }
        }

        return accepting;
    }

    public static String[] getTransitions(String s, String[] alphas, String[] states) throws MyException {
        if (s.startsWith("trans=[") && s.endsWith("]")) {
            s = s.replace("trans=[", "");
            s = s.replace("]", "");
        }
        else {
            throw new MyException("E0: Input file is malformed");
        }

        if (s.isEmpty()) return new String[]{};

        String[] transes = s.split(",");
        for (String tran: transes) {
            if (!tran.matches("[A-z0-9_]+>[A-z0-9_]+>[A-z0-9_]+")) {
                throw new MyException("E0: Input file is malformed");
            }
            String start_st = tran.split(">")[0];
            String alpha = tran.split(">")[1];
            String end_st = tran.split(">")[2];
            if (!Arrays.asList(states).contains(start_st)) {
                throw new MyException(String.format("E1: A state '%s' is not in the set of states", start_st));
            }
            if (!Arrays.asList(states).contains(end_st)) {
                throw new MyException(String.format("E1: A state '%s' is not in the set of states", end_st));
            }
            if (!Arrays.asList(alphas).contains(alpha)) {
                throw new MyException(String.format("E3: A transition '%s' is not represented in the alphabet", alpha));
            }
        }

        return transes;
    }

    static class MyException extends Exception {
        public MyException(String message) {
            super(message);
        }

        public MyException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }
}

/*
                 __,__
        .--.  .-"     "-.  .--.
       / .. \/  .-. .-.  \/ .. \
      | |  '|  /   Y   \  |'  | |
      | \   \  \ 0 | 0 /  /   / |
       \ '- ,\.-"`` ``"-./, -' /
        `'-' /_   ^ ^   _\ '-'`
        .--'|  \._   _./  |'--.
      /`    \   \ `~` /   /    `\
     /       '._ '---' _.'       \
    /           '~---~'   |       \
   /        _.             \       \
  /   .'-./`/        .'~'-.|\       \
 /   /    `\:       /      `\'.      \
/   |       ;      |         '.`;    /
\   \       ;      \           \/   /
 '.  \      ;       \       \   `  /
   '._'.     \       '.      |   ;/_
jgs  /__>     '.       \_ _ _/   ,  '--.
   .'   '.   .-~~~~~-. /     |--'`~~-.  \
  // / .---'/  .-~~-._/ / / /---..__.'  /
 ((_(_/    /  /      (_(_(_(---.__    .'
           | |     _              `~~`
           | |     \'.
            \ '....' |
             '.,___.'
 */

class Vertex<E> {
    public E name;

    public Vertex(E value) {
        this.name = value;
    }
}

class Edge<E, V> {

    public Edge(Vertex<E> from, Vertex<E> to, ArrayList<V> weight) {
        this.from = from;
        this.to = to;
        this.weights = weight;
    }

    public void addWeight(V weight) {
        weights.add(weight);
    }

    public ArrayList<V> weights;
    public Vertex<E> from;
    public Vertex<E> to;
}

interface Graph<E, V> {
    Vertex<E> addVertex(E value);
    Vertex<E> removeVertex(Vertex<E> v);
    Edge<E, V> addEdge(Vertex<E> from, Vertex<E> to, V weight);
//    Edge<E, V> removeEdge(Edge<E, V> e);
    ArrayList<Edge<E, V>> edgesFrom(Vertex<E> v);
    ArrayList<Edge<E, V>> edgesTo(Vertex<E> v);
    Vertex<E> findVertex(E value);
    Edge<E, V> findEdge(E from_value, E to_value);
    boolean hasEdge(Vertex<E> u, Vertex<E> v);
}

class AdjacencyMatrixGraph<E, V> implements Graph<E, V> {

    public HashMap<Vertex<E>, HashMap<Vertex<E>, Edge<E, V>>> matrix;
    private HashMap<Vertex<E>, Integer> visited;

    // members to discover cycle in the graph

    private HashMap<E, Vertex<E>> findVertexMap; // additional hashmap to make findVertex for O(1)

    AdjacencyMatrixGraph() {
        matrix = new HashMap<>();
        findVertexMap = new HashMap<>();
        visited = new HashMap<>();
    }

    @Override
    public Vertex<E> addVertex(E value) {
        Vertex<E> v = new Vertex<>(value);

        if (findVertex(value) == null) {
            matrix.put(v, new HashMap<>());
            findVertexMap.put(value, v);
            return v;
        }

        return null;
    }

    @Override
    public Vertex<E> removeVertex(Vertex<E> v) {
        matrix.remove(v);
        findVertexMap.remove(v.name);

        // looking for that vertex like adjacent
        for (Vertex<E> k: matrix.keySet()) {
            matrix.get(k).remove(v);
        }

        return v;
    }

    @Override
    public Edge<E, V> addEdge(Vertex<E> from, Vertex<E> to, V weight) {

        Edge<E, V> edge = findEdge(from.name, to.name);
        if (edge == null) {
            edge = new Edge<>(from, to, new ArrayList<>());
        }
        edge.addWeight(weight);

        if (matrix.get(from) != null) { // safe from possible exception
            matrix.get(from).put(to, edge);
        }

        return edge;
    }

//    @Override
//    public Edge<E, V> removeEdge(Edge<E, V> e) {
//        if (matrix.get(e.from) != null) { // safe from possible exception
//            matrix.get(e.from).remove(e.to);
//        }
//
//        return e;
//    }

    /**
     * return a collection or edge objects that are going from vertex v;
     */
    @Override
    public ArrayList<Edge<E, V>> edgesFrom(Vertex<E> v) {
        return new ArrayList<>(matrix.get(v).values());
    }

    /**
     * return a collection or edge objects that are going into vertex v;
     */
    @Override
    public ArrayList<Edge<E, V>> edgesTo(Vertex<E> v) {
        ArrayList<Edge<E, V>> ret = new ArrayList<>();

        for (Vertex<E> k: matrix.keySet()) {
            if (v != k && matrix.get(k).containsKey(v)) {
                ret.add(matrix.get(k).get(v));
            }
        }

        return ret;
    }

    @Override
    public Vertex<E> findVertex(E value) {
        return findVertexMap.get(value);
    }

    @Override
    public Edge<E, V> findEdge(E from_value, E to_value) {
        Vertex<E> from = findVertex(from_value);
        Vertex<E> to = findVertex(to_value);

        return (from != null && to != null) ? matrix.get(from).get(to) : null;
    }

    @Override
    public boolean hasEdge(Vertex<E> u, Vertex<E> v) {
        return matrix.get(u) != null && matrix.get(u).containsKey(v);
    }

    public boolean isConected(Vertex<E> start) {
        for (Vertex<E> v: matrix.keySet()) {
            visited.put(v, 0);
        }
        DFS(start);
        for (Vertex<E> v: visited.keySet()) {
            if (visited.get(v) == 0) return false;
        }

        return true;
    }

    // pretty simple standard DFS
    private void DFS(Vertex<E> start) {
        visited.put(start, 1); // visited

        // check all adjacent
        for (Vertex<E> to: matrix.get(start).keySet()) {
            // not visited
            if (visited.get(to) == 0) {
                DFS(to);
            }
        }
    }

    public void printEdges(){
        System.out.println("Edges: ");
        for(Map.Entry<Vertex<E>, HashMap<Vertex<E>, Edge<E, V>>> row: matrix.entrySet()){
            for(Map.Entry<Vertex<E>, Edge<E, V>> column: row.getValue().entrySet()){
                System.out.print("    "+column.getValue().from.name+" --");
                for (V weight: column.getValue().weights) {
                    System.out.print(weight + " ");
                };
                System.out.println("-- "+column.getValue().to.name);
            }
        }
    }
}
