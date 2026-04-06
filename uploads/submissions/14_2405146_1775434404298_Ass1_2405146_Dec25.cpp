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


class Figure {
    private:
        int row, col;
        string name;
        int** matrix;

        void allocateMatrix() {
            matrix = new int*[row];
            for (int i=0; i<row; i++)
                matrix[i] = new int[col];
        }
        
        void deallocate() {
            for (int i = 0; i < row; i++) {
                delete[] matrix[i];
            }
            delete[] matrix;
            matrix = nullptr;
        }

        void setName() {

            string suf, pref;
            if (col == 1) suf = "1D";
            else if (col == 2) suf = "2D";
            else if (col == 3) suf = "3D";
             
            if (row == 1) pref = "Point";
            else if (row == 2) pref = "Line";
            else if (row == 3) pref = "Triangle";
            else if (row == 4) pref = "Quadrilateral";
            else pref = "Polygon";

            this->name = pref + suf;
        }


    public:

        Figure() {
            row = 0, col = 0, name = ""; matrix = nullptr;
        }

        Figure(int r, int c, int* array) {
            if (r < 0 || c < 0) {
                cerr << "Error: Invalid dimensions in Figure::Figure(int r, int c, int* array)\n";
                return;
            }
            row = r, col = c;

            allocateMatrix();

            // for (int i = 0; i < array.size(); i += c) {
            //     matrix[i/c][0] = array[i];
            //     matrix[i/c][1] = array[i+1];
            //     if (c == 3) matrix[i/c][2] = a[i+2];
            // }

            for (int i=0; i<row; i++) {
                for (int j=0; j<col; j++) {
                    matrix[i][j] = array[i * col + j];
                }
            }
            
            setName();
        }

        Figure(const Figure& object) {
            row = object.row;
            col = object.col;
            name = object.name; // + "Clone";

            if (object.matrix == nullptr) {
                matrix = nullptr;
                return;
            }

            allocateMatrix();

            for (int i=0; i<row; i++) {
                for (int j=0; j<col; j++) {
                    matrix[i][j] = object.matrix[i][j];
                }
            }

        }

        ~Figure() {
            if (matrix != nullptr) {
                deallocate();

                cout << '\n' << name << " has been destroyed\n";
                row = 0, col = 0, name = "";
            }
        }

        int getSum(int r, int c) {
            if (matrix == nullptr) {
                cout << "Figure is empty\n";
                return 0;
            }

            if (r < 0 || c < 0 || r > row || c > col) {
                cerr << "Error: Invalid dimensions (row: 2-4, col: 2-3)\n";
                return -1;
            }

            int sum = 0;
            for (int i=0; i<r; i++)
                for (int j=0; j<c; j++) sum += matrix[i][j];
            return sum;
        }

        int getSum() {
            return getSum(row, col); // code reusing
        }

        void display() {
            if (matrix != nullptr) {
                for (int i=0; i<row; i++) {
                    for (int j=0; j<col; j++)
                        cout << matrix[i][j] << " ";
                    cout << '\n';
                }
            }
            else cout << "Figure is empty\n";

            // cout << name << '\n';
            // cout << row << " X " << col << '\n'; 
        }

        // getters
        int getRow() {return row;}
        int getColumn() {return col;}
        string getName() {return name;}

};



int32_t main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr); cout.tie(nullptr);

    Figure emptyObj;
    cout << emptyObj.getRow() << "X" << emptyObj.getRow() << '\n';
    cout << emptyObj.getName() << '\n';
    emptyObj.display();
    emptyObj.getSum();
    emptyObj.getSum(5, 5);
    cout << '\n';


    int line2D[] = {1, 2, 3, 4};
    Figure lineObj2D(2, 2, line2D);
    lineObj2D.display();
    cout << "Figure : " << lineObj2D.getName() << '\n';
    cout << "Dimension : " << lineObj2D.getRow() << "X" << lineObj2D.getColumn() << '\n';
    cout << "Sum of all elements : " << lineObj2D.getSum() << '\n';
    
    int r = 1, c = 2;
    cout << "Sum of " << r << "X" << c << " submatrix : " << lineObj2D.getSum(r, c) << "\n\n";
    
    
    
    int triangle2D[] = {1, 2, 5, 8, 10, 6};
    Figure triangleObj2D(3, 2, triangle2D);
    triangleObj2D.display();
    cout << "Figure : " << triangleObj2D.getName() << '\n';
    cout << "Dimension : " << triangleObj2D.getRow() << "X" << triangleObj2D.getColumn() << '\n';
    cout << "Sum of all elements : " << triangleObj2D.getSum() << '\n';

    r = 2, c = 2;
    cout << "Sum of " << r << "X" << c << " submatrix : " << triangleObj2D.getSum(r, c) << "\n\n";


    
    int quadrilateral2D[] = {1, 1, 5, 6, 7, 10, 20, 21};
    Figure quadrilateralObj2D(4, 2, quadrilateral2D);
    quadrilateralObj2D.display();
    cout << "Figure : " << quadrilateralObj2D.getName() << '\n';
    cout << "Dimnesion : " << quadrilateralObj2D.getRow() << " X " << quadrilateralObj2D.getColumn() << '\n';
    cout << "Sum of all elements : " << quadrilateralObj2D.getSum() << '\n';

    r = 3, c = 2;
    cout << "Sum of " << r << "X" << c << " submatrix : " << quadrilateralObj2D.getSum(r, c) << "\n\n";



    int line3D[] = {1, 1, 5, 6, 7, 10};
    Figure lineObj3D(2, 3, line3D);
    lineObj3D.display();
    cout << "Figure : " << lineObj3D.getName() << '\n';
    cout << "Dimnesion : " << lineObj3D.getRow() << " X " << lineObj3D.getColumn() << '\n';
    cout << "Sum of all elements : " << lineObj3D.getSum() << '\n';

    r = 2, c = 3;
    cout << "Sum of " << r << "X" << c << " submatrix : " << lineObj3D.getSum(r, c) << "\n\n";



    int triangle3D[] = {1, 1, 5, 6, 7, 10, 20, 25, 50};
    Figure triangleObj3D(3, 3, triangle3D);
    triangleObj3D.display();
    cout << "Figure : " << triangleObj3D.getName() << '\n';
    cout << "Dimnesion : " << triangleObj3D.getRow() << " X " << triangleObj3D.getColumn() << '\n';
    cout << "Sum of all elements : " << triangleObj3D.getSum() << '\n';

    r = 3, c = 2;
    cout << "Sum of " << r << "X" << c << " submatrix : " << triangleObj3D.getSum(r, c) << "\n\n";



    int quadrilateral3D[] = {1, 1, 5, 6, 7, 10, 20, 21, 40, 30, 35, 50};
    Figure quadObj3D(4, 3, quadrilateral3D);
    quadObj3D.display();
    cout << "Figure : " << quadObj3D.getName() << '\n';
    cout << "Dimnesion : " << quadObj3D.getRow() << " X " << quadObj3D.getColumn() << '\n';
    cout << "Sum of all elements : " << quadObj3D.getSum() << '\n';

    r = 3, c = 2;
    cout << "Sum of " << r << "X" << c << " submatrix : " << quadObj3D.getSum(r, c) << "\n\n";


    // cloning
    Figure triangleClone(triangleObj2D);
    cout << "Original triangle : " << '\n';
    triangleObj2D.display();
    cout << "Cloned triangle : " << '\n';
    triangleClone.display();
    
    // cloning
    Figure lineClone(lineObj2D);
    cout << "Original line : " << '\n';
    lineObj2D.display();
    cout << "Cloned line : " << '\n';
    lineClone.display();


    return 0;
}