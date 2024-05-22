from typing import Dict, List, Optional
import tracemalloc
import time

class Constraint:
    def __init__(self, variables: List[str]):
        self.variables = variables

    def satisfied(self, assignment: Dict[str, int]) -> bool:
        pass

class CSP:
    def __init__(self, variables: List[str], domains: Dict[str, List[int]]):
        self.variables = variables
        self.domains = domains
        self.constraints = {variable: [] for variable in variables}

    def add_constraint(self, constraint: Constraint):
        for variable in constraint.variables:
            self.constraints[variable].append(constraint)

    def consistent(self, variable: str, assignment: Dict[str, int]) -> bool:
        for constraint in self.constraints[variable]:
            if not constraint.satisfied(assignment):
                return False
        return True


    def backtracking_search(self, assignment: Dict[str, int] = {}) -> Optional[Dict[str, int]]:
        if len(assignment) == len(self.variables):
            return assignment

        unassigned = [v for v in self.variables if v not in assignment]
        first_unassigned = unassigned[0]

        for value in self.domains[first_unassigned]:
            updated_assignment = assignment.copy()
            updated_assignment[first_unassigned] = value

            if self.consistent(first_unassigned, updated_assignment):
                inferences = self.inference(updated_assignment)
                if inferences is not None:
                    backup = self.apply_inferences(inferences)
                    result = self.backtracking_search(updated_assignment)
                    if result is not None:
                        return result
                    self.undo_inferences(backup)

        return None

    def inference(self, assignment: Dict[str, int]) -> Optional[Dict[str, List[int]]]:
        inferences = {}

        for variable in self.variables:
            if variable not in assignment:
                new_domain = [value for value in self.domains[variable] if value not in assignment.values()]
                inferences[variable] = new_domain

        return inferences

    def apply_inferences(self, inferences: Dict[str, List[int]]):
        backup = self.domains.copy()
        for variable, domain in inferences.items():
            self.domains[variable] = domain
        return backup

    def undo_inferences(self, backup):
        self.domains = backup.copy()


class EquationConstraint(Constraint):
    def __init__(self, variables: List[str], equation: str):
        super().__init__(variables)
        self.equation = equation

    # convert from string to int
    def to_number(self, word: str) -> int:
        base = 1
        number = 0
        for letter in reversed(word):
            number += int(letter) * base
            base *= 10
        return number
    
    # check OP whether - or +
    def checkOp(self, operator: str) -> bool:
        if operator == '-':
            return True
        return False
    
    # sum/subtract from left to right
    def split_sum(self, equation: str) -> str:
        total_sum = 0
        flag = 0
        isMinus = False # remember the state of current number (+/-)
        i = 1 # start from 1, in case first char is -

        # check if first char is -
        if equation[0] == '-':
            isMinus = True
            flag += 1
        elif equation[0] == '+':
            isMinus = False
            flag += 1
        
        # processing, only add/substract if char is not a digit (means operators), so it must processes last time below
        while i < len(equation):
            char = equation[i]
            if not char.isdigit():
                if (isMinus):
                    total_sum -= self.to_number(equation[flag : i])
                    flag = i + 1
                    isMinus = self.checkOp(char)
                else:
                    total_sum +=  self.to_number(equation[flag : i])
                    flag = i + 1
                    isMinus = self.checkOp(char)
            if (equation[flag] == '0'):
                return ""
            i += 1
        
        # process the last time
        if (isMinus):
            total_sum -= self.to_number(equation[flag : i])
        else:
            total_sum += self.to_number(equation[flag : i])
       
        return str(total_sum)
    
    # split two sides of equationm, convert to int type and then mutiply
    def split_mutiply(self, equation: str) -> str:
        mutiply_index = equation.find('*')
        total = self.to_number(equation[: mutiply_index]) * self.to_number(equation[mutiply_index + 1 :])
        return str(total)
    
    # check if assignment is satisfied
    def satisfied(self, assignment: Dict[str, int]) -> bool:
        if len(set(assignment.values())) < len(assignment):
            return False

        if len(assignment) < len(self.variables):
            return True

        # replace alpha equation with digit equation
        equation = self.equation
        for var, value in assignment.items():
                equation = equation.replace(var, str(value))
                
        # separate left equation and right equation
        equal_index = equation.find("=")
        left_equation = equation[ : equal_index]
        right_equation = equation[equal_index+1 : ]
    
        # parentheses
        stack = []
        i = 0
        while i < len(left_equation):
            char = left_equation[i]
            if char == "(":
                stack.append(i)
            elif char == ")":
                start = stack.pop()
                sub_expression = left_equation[start + 1 : i]
                result = self.split_sum(sub_expression)
                left_equation = left_equation[: start] + result + left_equation[i + 1 :]
                i = start
            i += 1  
        
        # mutiplications    
        i = 0 
        while i < len(left_equation):
            char = left_equation[i]
            if char == "*":
                left = i - 1
                right = i + 1
                while (left >= 0 and left_equation[left].isdigit()):
                    left -= 1
                while (right < len(left_equation) and left_equation[right].isdigit()):
                    right += 1
                sub_expression = left_equation[left + 1 : right]
                result = self.split_mutiply(sub_expression)
                left_equation = left_equation[: left + 1] + result + left_equation[right :]
                i = left + 1
            i += 1       
    
        return self.split_sum(left_equation) == right_equation

def solve_equation(equation: str) -> Optional[Dict[str, int]]:
    variables = set(char for char in equation if char.isalpha())
    variables_list = sorted(list(variables))
    domains = {var: list(range(10)) for var in variables_list}

    csp = CSP(variables_list, domains)
    csp.add_constraint(EquationConstraint(variables_list, equation))

    solution = csp.backtracking_search()
    return solution

if __name__ == '__main__':
    # start time and memory calculator
    tracemalloc.start()
    start_time = time.time_ns()
    
    equation = "AB*(CA-DE)+EF=BFC" # main equation
    solution = solve_equation(equation)
    if solution is None:
        print("No solution found!")
    else:
        for i in sorted(solution.values()):
            print(i,end='')
    
        # this line will print whole dict (for testing only!)
        # print(sorted(solution.items()))
        
    # stop time and memory calculator
    memory = tracemalloc.get_traced_memory()[1]
    total_time = (time.time_ns() - start_time) / 1000000
    tracemalloc.stop()
    print()
    print("Memory used: ", memory / 1024**2, "MB")
    print("Time used: ", total_time, "ms")
