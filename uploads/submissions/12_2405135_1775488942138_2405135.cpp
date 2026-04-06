#include<bits/stdc++.h>
using namespace std;

#define all(x) (x).begin(), (x).end()
using ll = long long;
using ull = unsigned long long;
using pii = pair<int, int>;
using pll = pair<ll, ll>;
#define f(t, i, x, y) for (t (i)=(x); (i)<(y); (i)++)
#define fe(t, i, x, y) for (t (i)=(x); (i)<=(y); (i)++)

#define pb push_back
#define ppb pop_back
#define pf push_front
#define ppf pop_front
#define intmin INT64_MIN


vector<vector<int>>adj_list;
vector<vector<int>>adj_mat;
vector<bool>visited;

void dfs(int source, vector<int>& component) { // O(V + E)
    visited[source] = true;
    component.push_back(source);

    for (int& child : adj_list[source]) {
        if (!visited[child]) dfs(child, component);
    }
}



int32_t main() {
    // freopen("input.txt", "r+", stdin);
    // freopen("output.txt", "a+", stdout);

    ios::sync_with_stdio(false);
    cin.tie(nullptr); cout.tie(nullptr);
    
    int n; cin >> n;
    
    adj_list.resize(n);
    visited.resize(n, false);
    adj_mat.resize(n, vector<int>(n, 0));

    char b1, b2, comma;
    int a, b;

    while (cin >> b1 >> a >> comma >> b >> b2) {

        adj_list[a].push_back(b);
        adj_list[b].push_back(a); // treating them undirected for easy dfs

        adj_mat[a][b] = adj_mat[b][a] = 1;
    }

    vector<vector<int>>groups; // connected components

    for (int i=0; i<n; i++) {
        if (!visited[i]) {
            vector<int>one_group; // 1 component
            dfs(i, one_group); // let dfs make a group with all the nodes it can traverse
            groups.push_back(one_group);
        }
    }

    vector<vector<pair<int, int>>>matches_left(groups.size()); // missing edges

    for (int grpIdx = 0; grpIdx<groups.size(); grpIdx++) {

        vector<int>& group = groups[grpIdx];

        // brute-forcing for all missing edges
        for (int i=0; i<group.size(); i++) {
            for (int j=i+1; j<group.size(); j++) {
                int a = group[i], b = group[j];
                if (!adj_mat[a][b]) {
                    matches_left[grpIdx].push_back({a, b});
                }
            }
        }
        // brute-forcing for all missing edges

    }

    // OUTPUT

    int t = groups.size();
    cout << t << '\n';
    

    for (int i=0; i<groups.size(); i++) {
        // groups show
        cout << "Group " << (i+1) << ": "; // Group 1: 

        cout << "{";
        for (int j=0; j<groups[i].size(); j++) {
            cout << groups[i][j];
            if (j != groups[i].size()-1) cout << ", ";
        }
        cout << "}";

        cout << " | ";

        int matchesLeftInGroup = matches_left[i].size(); // matches left in ith group

        if (matchesLeftInGroup == 0) {
            cout << "none\n";
            continue;
        }

        for (int j=0; j<matchesLeftInGroup; j++) {
            cout << "[" << matches_left[i][j].first << ", ";
            cout << matches_left[i][j].second << "]";

            if (j != matchesLeftInGroup-1) cout << ", ";
        }

        cout << '\n';
    }


    return 0;
}