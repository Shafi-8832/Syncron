/*
 * Emergency Communication Network — Divide & Conquer
 * CSE 106 Offline Assignment, BUET
 *
 * Algorithm:
 *   DIVIDE  : Sort cities by x-coordinate; split at the midpoint.
 *   CONQUER : When subgroup ≤ M cities (base case), build MST with
 *             Kruskal's on all O(M²) pairs using Union-Find.
 *   MERGE   : Compare only the rightmost M cities of the left half with
 *             the leftmost M cities of the right half → pick cheapest edge.
 *
 * Recurrence: T(N) = 2·T(N/2) + O(M²)
 *   Since M is a small constant, merge cost is O(1) per recursion level.
 *   Sorting is O(N log N).  Total: O(N log N).
 */

#include <bits/stdc++.h>
using namespace std;

// ─────────────────────────── Data Types ───────────────────────────

struct City {
    int    id;
    double x, y;
};

struct Edge {
    double cost;
    int    u, v;           // city IDs (1-indexed as given in input)
    bool operator<(const Edge& o) const { return cost < o.cost; }
};

// ─────────────────────────── Union-Find ───────────────────────────

struct UnionFind {
    unordered_map<int, int> parent, rank_;

    void init(const vector<City>& cities) {
        for (auto& c : cities) {
            parent[c.id] = c.id;
            rank_[c.id]  = 0;
        }
    }

    int find(int x) {
        if (parent[x] != x)
            parent[x] = find(parent[x]);   // path compression
        return parent[x];
    }

    // Returns true if x and y were in different sets (i.e. union happened)
    bool unite(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        if (rank_[px] < rank_[py]) swap(px, py);
        parent[py] = px;
        if (rank_[px] == rank_[py]) rank_[px]++;
        return true;
    }
};

// ─────────────────────────── Helpers ──────────────────────────────

inline double euclidean(const City& a, const City& b) {
    double dx = a.x - b.x, dy = a.y - b.y;
    return sqrt(dx * dx + dy * dy);
}

// ─────────────────────────── Divide & Conquer ─────────────────────

/*
 * Recursively connects the cities in `cities[lo..hi)`.
 * Returns the list of edges added for this subproblem.
 * `M` is the base-case threshold.
 */
vector<Edge> solve(vector<City>& cities, int lo, int hi, int M) {

    int n = hi - lo;
    vector<Edge> result;

    // ── BASE CASE ────────────────────────────────────────────────
    if (n <= M) {
        // Collect all O(M²) edges, sort by cost, apply Kruskal's
        vector<Edge> candidates;
        for (int i = lo; i < hi; i++)
            for (int j = i + 1; j < hi; j++)
                candidates.push_back({euclidean(cities[i], cities[j]),
                                      cities[i].id, cities[j].id});

        sort(candidates.begin(), candidates.end());

        UnionFind uf;
        uf.init(vector<City>(cities.begin() + lo, cities.begin() + hi));

        for (auto& e : candidates)
            if (uf.unite(e.u, e.v))
                result.push_back(e);

        return result;
    }

    // ── DIVIDE ───────────────────────────────────────────────────
    // cities[lo..hi) is already sorted by x; split at midpoint
    int mid = lo + n / 2;

    // ── CONQUER ──────────────────────────────────────────────────
    auto leftEdges  = solve(cities, lo,  mid, M);
    auto rightEdges = solve(cities, mid, hi,  M);

    result.insert(result.end(), leftEdges.begin(),  leftEdges.end());
    result.insert(result.end(), rightEdges.begin(), rightEdges.end());

    // ── MERGE ────────────────────────────────────────────────────
    // Left half:  cities[lo .. mid-1]   — rightmost M are cities[mid-M .. mid-1]
    // Right half: cities[mid .. hi-1]   — leftmost  M are cities[mid .. mid+M-1]
    //
    // We only compare these boundary cities (O(M²) = O(1) pairs).
    // Because both halves are internally connected, a single cross-edge
    // is enough to make the entire subproblem's network connected.

    int leftStart  = max(lo,  mid - M);   // rightmost M of left half
    int rightEnd   = min(hi,  mid + M);   // leftmost  M of right half

    Edge best = {1e18, -1, -1};
    for (int i = leftStart; i < mid; i++)
        for (int j = mid; j < rightEnd; j++) {
            double c = euclidean(cities[i], cities[j]);
            if (c < best.cost)
                best = {c, cities[i].id, cities[j].id};
        }

    result.push_back(best);
    return result;
}

// ─────────────────────────── main ─────────────────────────────────

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    int N, M;
    cin >> N >> M;

    vector<City> cities(N);
    for (int i = 0; i < N; i++)
        cin >> cities[i].id >> cities[i].x >> cities[i].y;

    // Sort by x-coordinate (divide step prerequisite)
    sort(cities.begin(), cities.end(),
         [](const City& a, const City& b) { return a.x < b.x; });

    // Run Divide & Conquer
    vector<Edge> edges = solve(cities, 0, N, M);

    // Compute total cost
    double totalCost = 0;
    for (auto& e : edges) totalCost += e.cost;

    // ── Output ───────────────────────────────────────────────────
    cout << fixed << setprecision(2);
    cout << "Total Cost: " << totalCost << "\n";
    cout << "Edges:\n";
    for (auto& e : edges)
        cout << e.u << " " << e.v << "\n";

    return 0;
}
