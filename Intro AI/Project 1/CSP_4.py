import tracemalloc
import time

class Solution:
    # Convert dict to string
    def toStr(self, li=dict()):
        res = ""
        for i in li.values():
            res += str(i)
        return res

    # Constructor to initialize the data from the input equation
    def __init__(self, data=str()):
        self.subtree = list()
        self.scope = list()
        self.subres = list()
        self.order = list()
        self.assignment = dict()
        self.state_space = set()
        self.block=[]

        # Initializing the starting state
        for i in data:
            if i.isalpha():
                self.assignment.update({i: -1})

        data = data.replace('*', ' ')

        temp = data.split('=')  # Split equation into operands and result

        operands = temp[0].split(' ')  # Split the operands
        for operand in operands:
            self.block.append(operand[0])
        self.block.append(temp[1][0])
        MaxLenOperand = 0
        for i in range(0, len(operands)):
            operands[i] = operands[i][::-1]  # Reverse each operand
            MaxLenOperand = max(MaxLenOperand, len(operands[i]))

        global result
        result = temp[1]
        result = result[::-1]  # Reverse the result


        for i in range(0, len(result)):
            self.subtree.append(list())
            self.scope.append(dict())

        for i in range(0, len(operands[0])):
            for j in range(0, len(operands[1])):
                id = i + j
                char1 = operands[0][i]
                char2 = operands[1][j]

                if char1 not in self.subtree[id]:
                    self.subtree[id].append(char1)
                if char2 not in self.subtree[id]:
                    self.subtree[id].append(char2)

                if char1 not in self.scope[id]:
                    self.scope[id].update({char1: {char2: 1}})
                else:
                    if char2 not in self.scope[id][char1]:
                        self.scope[id][char1].update({char2: 1})
                    else:
                        temp = self.scope[id][char1][char2] + 1
                        self.scope[id][char1].update({char2: temp})

        # Add the characters (nodes) of the result to the corresponding subtrees
        for i in range(0, len(result)):
            if result[i] not in self.subtree[i]:
                self.subtree[i].append(result[i])


    # Check if the assignment of a subproblem check_subtreeisfies the condition
    # If it check_subtreeisfies, return the carry
    # Otherwise, return None
    def check_subtree(self, problem=list(), assign=dict(), subRes="", factor=dict(), preCarry=0):
        for i in self.assignment:
            if i in self.block and self.assignment[i]==0:
                return None

        pos = 0

        for char1 in problem:
            if char1 in factor:
                for item in factor[char1].items():
                    char2 = item[0]
                    imp = item[1]

                    pos += assign[char1] * assign[char2] * imp

        pos += preCarry

        if pos % 10 == assign[subRes]:
            return int(pos / 10)

        return None

    # Solve the subproblem
    def solve_subtree(self, idSP=int(), carry=int(), id=int(), localState=dict()):
        if id == len(self.subtree[idSP]):
            temp = self.check_subtree(self.subtree[idSP], localState, result[idSP], self.scope[idSP], carry)

            if temp != None:
                res = self.Cryptarithmetic(idSP + 1, temp)

                if res != None:
                    return res

            return None

        char = self.subtree[idSP][id]
        res = dict()

        if localState[char] == -1:
            for val in range(0, 10):
                if val not in localState.values():
                    localState[char] = val

                    res = self.solve_subtree(idSP, carry, id + 1, localState)
                    if res != None:
                        return res

                    localState[char] = -1
        else:
            res = self.solve_subtree(idSP, carry, id + 1, localState)

        return res

    # Main function to solve the equation
    def Cryptarithmetic(self, idSP=int(), carry=int()):
        if len(temp.assignment) > 10:
            return

        if idSP == len(self.subtree):
            if carry == 0:
                return temp.assignment
            else:
                return None

        strState = self.toStr(temp.assignment)

        if strState in self.state_space:
            return None

        res = self.solve_subtree(idSP, carry, 0, temp.assignment)
        self.state_space.add(strState)

        return res

# start time and memory calculator
tracemalloc.start()
start_time = time.time_ns()

temp = Solution("ABCD*EF=HAGSL")

res = temp.Cryptarithmetic(0, 0)
if res != None:
    for item in sorted(res.items(), key=lambda x: x[0]):
        print(item[0],item[1], end="")
else:
    print("NO SOLUTION")
    
# stop time and memory calculator
memory = tracemalloc.get_traced_memory()[1]
total_time = (time.time_ns() - start_time) / 1000000
tracemalloc.stop()
print()
print("Memory used: ", memory / 1024**2, "MB")
print("Time used: ", total_time, "ms")
