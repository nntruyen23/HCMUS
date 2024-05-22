from typing import Dict
import tracemalloc
import time

class Solution:
    @staticmethod
    def toStr(li: dict) -> str:
        res = ""
        for i in li.values():
            res += str(i)
        return res

    def process_equation(self,equation):
        new_equation = ''
        sign_reverse = 0
        for char in equation:
            if char.isalpha():
                new_equation += char
            else:
                if char == '(':
                    if len(new_equation) > 0 and new_equation[-1] == '-':
                        sign_reverse = 1
                    continue
                if char == ')':
                    sign_reverse = 0
                    continue

                if sign_reverse == 1:
                    if char == '+':
                        new_equation += '-'
                    else:
                        if char == '-':
                            new_equation += '+'
                else:
                    new_equation += char

        return new_equation
    
    def __init__(self,equation):
        #Open parasis
        self.equation=self.process_equation(equation)

        self.assignment=dict()
        self.scope = list()
        self.subtrees = list()
        self.operations=['+']
        self.state_space=set()
        self.block=[]

        #Init assignment
        for char in equation:
            if char.isalpha():
                self.assignment.update({char:-1})

        #Init operations
        self.operations+=[x for x in equation if x in ['+','-']]
        
        #Split equation into words
        self.equation=self.equation.replace('+',' ').replace('-',' ')

        words, result = self.equation.split('=')
        words=words.split(' ')
        for word in words:
            self.block.append(word[0])
        #Create sub tree
        max_length=0
        for word in words:
            max_length=max(max_length,len(word))

        max_length=max(max_length,len(result))

        for i in range(0,max_length):
            self.subtrees.append(list())
            self.scope.append(dict())

        #Add character of each operand into approriate subtree
        for i in range(0,len(words)):
            word=words[i]

            for j in range(0,len(word)):
                #Reverse index from word to subtree
                index=len(word)-j-1

                #Char not in dict
                if not word[j] in self.scope[index]:
                    if self.operations[i]=='+':
                        pos=1
                        neg=0
                    else:
                        pos=0
                        neg=1

                    self.subtrees[index].append(word[j])
                    self.scope[index].update({word[j]:(pos,neg)})
                #Char in dict
                else:

                    pos=self.scope[index][word[j]][0]
                    neg=self.scope[index][word[j]][1]

                    if self.operations[i]=='+':
                        pos+=1
                    else:
                        neg+=1
                    
                    self.scope[index].update({word[j]:(pos,neg)})
        
        #Add character of result into approriate subtree
        for i in range (0,len(result)):
            char=result[len(result)-i-1]

            if char not in self.subtrees[i]:
                self.subtrees[i].append(char)
                self.scope[i].update({char:(0,1)})
            else:
                pos=self.scope[i][char][0]
                neg=self.scope[i][char][1]+1
                self.scope[i].update({char:(pos,neg)})

    def check_subtree(self,problem,assignment,factor,pre_carry=0):
        for i in self.assignment:
            if i in self.block and self.assignment[i]==0:
                return None
        pos,neg=0,0

        for char in problem:
            pos+=assignment[char]*factor[char][0]
            neg+=assignment[char]*factor[char][1]

        temp_1=pos+pre_carry
        temp_2=neg

        if(temp_1<0):
            return None
        
        temp_3=temp_1%10-temp_2%10

        if(temp_3==0):
            return int(temp_1/10)-int(temp_2/10)
        
        return None
    
    def solve_subtree(self,index_tree,carry,index,cur_state):
        if index==len(self.subtrees[index_tree]):
            temp=self.check_subtree(self.subtrees[index_tree],cur_state,self.scope[index_tree],carry)

            if temp!=None:
                res=self.Cryptarithmetic(index_tree+1,temp)

                if res!=None:
                    return res
            return None
        
        char=self.subtrees[index_tree][index]
        res=dict()

        if cur_state[char]==-1:
            for val in range(10):
                if val not in cur_state.values():
                    cur_state[char]=val

                    res=self.solve_subtree(index_tree,carry,index+1,cur_state)
                    if res!=None:
                        return res
                    
                    cur_state[char]=-1
        else:
            res=self.solve_subtree(index_tree,carry,index+1,cur_state)

        return res


    def Cryptarithmetic(self,index_tree,carry):
        if len(temp.assignment)>10:
            return None
        
        if index_tree==len(self.subtrees):
            if carry==0:
                return temp.assignment
            else:
                return None
        
        cur_state=self.toStr(temp.assignment)

        if cur_state in self.state_space:
            return None
        
        res=self.solve_subtree(index_tree,carry,0,temp.assignment)
        self.state_space.add(cur_state)

        return res

# start time and memory calculator
tracemalloc.start()
start_time = time.time_ns()

temp = Solution("FIND+MORE=MONEY")

res = temp.Cryptarithmetic(0, 0)

if res != None:
    for item in sorted(res.items(), key = lambda x: x[0]):
        print(item[1], end = "")
else:
    print("NO SOLUTION")
    
# stop time and memory calculator
memory = tracemalloc.get_traced_memory()[1]
total_time = (time.time_ns() - start_time) / 1000000
tracemalloc.stop()
print()
print("Memory used: ", memory / 1024**2, "MB")
print("Time used: ", total_time, "ms")