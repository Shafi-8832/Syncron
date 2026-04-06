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
#define int long long


int32_t main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr); cout.tie(nullptr);

    int I, D, R; cin >> I >> D >> R;

    string S, T;
    cin.ignore();
    getline(cin, S);
    getline(cin, T);
    
    int n = S.size(), m = T.size();
    
    vector<vector<int>>dp(n + 1, vector<int>(m + 1, 0));

    int i, j;
    // precompute the base cases
    for (i=1; i<=n; i++) dp[i][0] = i * D;
    for (j=1; j<=m; j++) dp[0][j] = j * I;

    for (i=1; i<=n; i++) {
        for (j=1; j<=m; j++) {
            int replace = dp[i-1][j-1] + ((S[i-1] != T[j-1]) ? R : 0);

            int del = dp[i-1][j] + D;
            int insert = dp[i][j-1] + I;

            dp[i][j] = min({replace, del, insert});
        }
    }

    // backtracking
    //          op    S    T
    list<tuple<char, int, int>>path;

    // 1-indexing for dp table but 0-indexing for string
    i = n, j = m;

    while (i > 0 && j > 0) {
        if (S[i-1] == T[j-1]) {
            path.push_front({'M', i-1, j-1});
            i--, j--;
        }
        else if (dp[i-1][j-1] + R == dp[i][j]) {
            path.push_front({'R', i-1, j-1});
            i--, j--;
        }
        else if (dp[i-1][j] + D == dp[i][j]) {
            path.push_front({'D', i-1, j});
            i--;
        } 
        else if (dp[i][j-1] + I == dp[i][j]) {
            path.push_front({'I', i, j-1});
            j--;
        }
    }

    while (i > 0) path.push_front({'D', i-1, 0}), i--;
    while (j > 0) path.push_front({'I', 0, j-1}), j--;
    
    cout << "Minimum Cost: " << dp[n][m] << '\n';
    cout << "Operations:\n";

    for (auto op : path) {
        char c = get<0>(op);
        int si = get<1>(op);
        int tj = get<2>(op);

        if (c == 'R') cout << "Replace " << S[si] << " with " << T[tj] << '\n';
        if (c == 'M') cout << "Match " << S[si] << '\n';
        if (c == 'D') cout << "Delete " << S[si] << '\n';
        if (c == 'I') cout << "Insert " << T[tj] << '\n';
    }
    

    return 0;
}