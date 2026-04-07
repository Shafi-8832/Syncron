import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


class Operation {
    String operation;
    int SourceIndex;
    int TargetIndex;

    public Operation(String operation, int sourceIndex, int targetIndex) {
        this.operation = operation;
        SourceIndex = sourceIndex;
        TargetIndex = targetIndex;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int I = scanner.nextInt();
        int D = scanner.nextInt();
        int R = scanner.nextInt();

        String Source = scanner.next();
        String Target = scanner.next();

        int n = Source.length();
        int m = Target.length();

        int[][] dp = new int[n + 1][m + 1];

        
        // precompute the base cases
        int i, j;
        for (i = 1; i <= n; i++) dp[i][0] = i * D;
        for (j = 1; j <= m; j++) dp[0][j] = j * I;

        for (i=1; i<=n; i++) {
            for (j=1; j<=m; j++) {
                int replace = dp[i-1][j-1] + ((Source.charAt(i-1) != Target.charAt(j-1)) ? R : 0);
                int delete = dp[i-1][j] + D;
                int insert = dp[i][j-1] + I;

                dp[i][j] = Math.min(Math.min(insert, delete), replace);
            }
        }

        // backtracking
        LinkedList<Operation> path = new LinkedList<>();

        i = n;
        j = m;

        while (i > 0 && j > 0) {
            if (Source.charAt(i - 1) == Target.charAt(j - 1)) {
                path.addFirst(new Operation("M", i - 1, j - 1));
                i--;
                j--;
            } else if (dp[i - 1][j - 1] + R == dp[i][j]) {
                path.addFirst(new Operation("R", i - 1, j - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] + D == dp[i][j]) {
                path.addFirst(new Operation("D", i - 1, j - 1));
                i--;
            } else if (dp[i][j - 1] + I == dp[i][j]) {
                path.addFirst(new Operation("I", i - 1, j - 1));
                j--;
            }
        }

        while (i > 0) {
            path.addFirst(new Operation("D", i-1, 0));
            i--;
        }
        while (j > 0) {
            path.addFirst(new Operation("I", 0, j-1));
            j--;
        }

        System.out.println("Minimum Cost: " + dp[n][m]);
        System.out.println("Operations: ");
        for (i=0; i<path.size(); i++) {
            String op = path.get(i).operation;
            int SourceIndex = path.get(i).SourceIndex;
            int TargetIndex = path.get(i).TargetIndex;

            if (op.equals("M")) System.out.println("Match " + Source.charAt(SourceIndex));
            if (op.equals("R")) System.out.println("Replace " + Source.charAt(SourceIndex) + " with " + Target.charAt(TargetIndex));
            if (op.equals("D")) System.out.println("Delete " + Source.charAt(SourceIndex));
            if (op.equals("I")) System.out.println("Insert " + Target.charAt(TargetIndex));
        }

        scanner.close();

    }
}