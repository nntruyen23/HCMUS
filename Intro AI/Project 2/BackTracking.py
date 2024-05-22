from itertools import combinations
from pysat.solvers import Glucose3
import time

class ToCnf:
    def __init__(self, board):
        self.board = board
        self.KB = []
        self.subBoard = [[None for _ in range(
            len(board[0]))] for _ in range(len(board))]
        for i in range(len(board)):
            for j in range(len(board[0])):
                if board[i][j] != 0:
                    self.subBoard[i][j] = False
                    position = self.indexToPosition(i, j)
                    self.KB.append([-position])

    def indexToPosition(self, i, j):
        return i * len(self.board[1]) + j+1

    def positionToIndex(self, index):
        index -= 1
        i = index // len(self.board[1])
        j = index % len(self.board[1])
        return i, j

    def getAdjacent(self, i, j):
        m = len(self.board)
        n = len(self.board[0])
        adjacent_positions = []

        directions = [(-1, -1), (-1, 0), (-1, 1), (0, -1),
                      (0, 1), (1, -1), (1, 0), (1, 1), (0, 0)]

        for di, dj in directions:
            if 0 <= i + di < m and 0 <= j + dj < n:
                adjacent_positions.append((i + di, j + dj))

        return adjacent_positions

    def getUnassignAdjacent(self, i, j):
        m = len(self.board)
        n = len(self.board[0])
        adjacent_positions = []

        directions = [(-1, -1), (-1, 0), (-1, 1), (0, -1),
                      (0, 1), (1, -1), (1, 0), (1, 1), (0, 0)]

        for di, dj in directions:
            if 0 <= i + di < m and 0 <= j + dj < n and self.subBoard[i+di][j+dj] == None:
                adjacent_positions.append((i + di, j + dj))

        return adjacent_positions

    def caclUnAssign(self, adjacent):
        count = 0
        for i in range(len(adjacent)):
            if self.subBoard[adjacent[i][0]][adjacent[i][1]] == None:
                count += 1
        return count

    def calcMineRemain(self, i, j, adjacent):
        total = self.board[i][j]
        for i in range(len(adjacent)):
            if self.subBoard[adjacent[i][0]][adjacent[i][1]] == True:
                total -= 1
        return total

    def generateCNF(self, i, j):
        adjacent = self.getAdjacent(i, j)
        unAssign = self.getUnassignAdjacent(i, j)
        n = self.caclUnAssign(adjacent)
        k = self.calcMineRemain(i, j, adjacent)
        unAsgn = []
        for i in unAssign:
            unAsgn.append(self.indexToPosition(i[0], i[1]))
        if (k == 0):
            for i in unAsgn:
                self.KB.append([-i])
                index = self.positionToIndex(i)
                self.subBoard[index[0]][index[1]] = False
            return
        elif (k+1 > n):
            for i in unAsgn:
                self.KB.append([i])
                index = self.positionToIndex(i)
                self.subBoard[index[0]][index[1]] = True
            return

        tmp = list(combinations(unAsgn, k+1))
        U = list()
        for i in tmp:
            l = []
            for j in i:
                l.append(-j)
            self.KB.append(l)

        tmp = list(combinations(unAsgn, n-k+1))
        for i in tmp:
            self.KB.append(i)

    def pySAT(self):
        g = Glucose3()
        for clause in self.KB:
            g.add_clause(clause)

        if not g.solve():
            print('No solution')
        else:
            model = g.get_model()
            print(model)

    def generateKB(self):
        for i in range(len(self.board)):
            for j in range(len(self.board[0])):
                if self.board[i][j] != 0:
                    self.generateCNF(i, j)
        return self.KB
#backtracking
def is_satisfied(clause, assignment):
    for literal in clause:
        var = abs(literal)
        if var in assignment:
            if literal > 0 and assignment[var]:
                return True
            elif literal < 0 and not assignment[var]:
                return True
    return False


def all_clauses_satisfied(cnf_formula, assignment):
    return all(is_satisfied(clause, assignment) for clause in cnf_formula)


def get_unassigned_variable(cnf_formula, assignment):
    for clause in cnf_formula:
        for literal in clause:
            if abs(literal) not in assignment:
                return abs(literal)
    return None


def backtrack_satisfy(cnf_formula, assignment):
    if all_clauses_satisfied(cnf_formula, assignment):
        return assignment.copy()
    variable = get_unassigned_variable(cnf_formula, assignment)
    if variable is None:
        return None
    for value in [True, False]:
        assignment[variable] = value
        result = backtrack_satisfy(cnf_formula, assignment)
        if result:
            return result
        del assignment[variable]
    return None

def output(broad, solution):
    l = len(board)
    for i in range(l):
        for j in range(l):
            k = i * l + j + 1
            if solution.get(k, None) == True:
                broad[i][j] = 'X'
            elif board[i][j]==0:
                    board[i][j]='-'
    return board

def read_2d_array_from_file(file_path):
    try:
        with open(file_path, 'r') as file:
            lines = file.readlines()

        array_2d = [list(map(int, line.strip().split())) for line in lines]
        return array_2d
    except FileNotFoundError:
        print("File not found")
        return None
    except Exception as e:
        print("Error:", e)
        return None
    
def save_2d_array_to_file(array_2d, file_path):
    try:
        with open(file_path, 'w') as file:
            for row in array_2d:
                row_str = ' '.join(map(str, row))
                file.write(row_str + '\n')
        print("Save success.")
    except Exception as e:
        print("Error:", e)   


              
# Sample usage
board=read_2d_array_from_file('input.txt')
mine_sweeper = ToCnf(board)
cnf_formula  = mine_sweeper.generateKB()
start_time = time.time()
solution = backtrack_satisfy(cnf_formula, {})
end_time = time.time()
execution_time = end_time - start_time
if solution is None:
    print("No solution")
else:
    print(solution)
    save_2d_array_to_file(output(board,solution),'output.txt')
    print("Time executing: {:.6f} second".format(execution_time))

        

