import random
import re
import tracemalloc
import time

class GE:
    def __init__(self, full_words, reduced_words, result, n=10, mutation_rate=0.85, max_generation=10000):
        self.population_size = n
        self.max_generation = max_generation
        self.mutation_rate = mutation_rate
        self.full_words = full_words
        self.block=[]
        for x in reduced_words:
            if x[0] not in self.block:
                self.block.append(x[0])
        if result[0] not in self.block:
            self.block.append(result[0])
        self.result = result
        self.population = self.initialization(reduced_words)
        self.population=list(self.population.items())
        self.population=sorted(self.population,key=lambda x:x[1])

    def avoid(self,gen):
        while gen[0] in self.block:
            index=random.randint(1,len(gen)-1)
            gen[0],gen[index]=gen[index],gen[0]
        return gen

    def initialization(self, reduced_words):
        population = {}
        variables = set()
        for word in reduced_words:
            variables.update(word)
        variables.update(self.result)
        variables=list(variables)
        while len(variables)<10:
            variables.append('-')
        for _ in range(self.population_size):
            gen = list(variables)
            random.shuffle(gen)
            gen=self.avoid(gen)
            fitness = self.fitnessFunction(gen)
            population[tuple(gen)]=fitness

        return population

    def to_number(self, gen,word):
        base = 1
        number = 0
        for letter in reversed(word):
            number += gen.index(letter) * base
            base *= 10
        return number

    def fitnessFunction(self, gen):
        equation = ''
        for word in self.full_words:
            if word in ['(', ')', '+', '-', '*', '/']:
                equation += word
            else:
                number = self.to_number(gen, word)
                number = f"{number}"
                equation += number

        result = self.to_number(gen, self.result)
        result = f"{result}"
        equation = equation + '-' + result

        return abs(eval(equation))
    
    def calcProbability(self):
        probs = []
        total = 0

        for gen in self.population:
            fitness = gen[1]
            total += 1 / fitness
            prob = 1 / fitness
            probs.append(prob)

        for i in range(len(probs)):
            probs[i] = probs[i] * (1 / total)

        return probs

    def tournament_selection(self, tournament_size):
        selected = []
        for _ in range(tournament_size):
            individual = random.choice(self.population)
            selected.append(individual)
        selected = sorted(selected, key=lambda x: x[1])
        return selected[0][0]

    def selectNumbers(self, nums, probabilities, n):
        selected_nums = random.choices(nums, probabilities, k=n)
        return selected_nums
    
    

    def mutation(self, gen):
        mutated_gen = list(gen)
        if random.random() < self.mutation_rate:
            index1 = random.randint(0, len(mutated_gen) - 1)
            index2 = random.randint(0, len(mutated_gen) - 1)
            while(gen[index1]=='-'and gen[index2]=='-' or index1==index2):
                index1 = random.randint(0, len(mutated_gen) - 1)
                index2 = random.randint(0, len(mutated_gen) - 1)
            mutated_gen[index1], mutated_gen[index2] = mutated_gen[index2], mutated_gen[index1]

        mutated_gen = self.avoid(mutated_gen)
        return mutated_gen
    
    def isGoal(self):
        for gen in self.population:
            # If conflict = 0 -> goal
            if gen[1] == 0:  
                return gen
        return None
    
    def printGoal(self):
        gen = self.isGoal()
        if gen is not None:
            out = list(gen[0])
            char_dict = {char: index for index, char in enumerate(out)}
            char_dict = dict(sorted(char_dict.items()))

            print("Solution found:")
            print("Character indexes:")
            for char, index in char_dict.items():
                if char != '-':
                    print(index)
            print("Original order:", gen)
        else:
            print("No solution found.")
    
    # def printGoal(self):
    #     gen = self.isGoal()
    #     if gen is not None:
    #         out = list(gen[0])
    #         char_dict = {char: index for index, char in enumerate(out)} 
    #         char_dict=dict(sorted(char_dict.items()))
    #         for i in len(char_dict):
    #             if char_dict[i].keys()!='-':
    #                 print(char_dict[i].values())
    #         print(gen)
    #     else:
    #         print("No solution found.")

    def GA(self):
        for _ in range(self.max_generation):
            if self.isGoal() is not None:
                break

            probabilities = self.calcProbability()
            combined_population = self.population
            selected = self.selectNumbers(self.population, probabilities, 5)

            for i in range(0,5):
                cur=selected[i][0]
                child_1=self.mutation(cur)
                child_2=self.mutation(cur)
                fitness_1 = self.fitnessFunction(child_1)
                fitness_2 = self.fitnessFunction(child_2)
                combined_population.append((child_1, fitness_1))
                combined_population.append((child_2, fitness_2))

        combined_population = sorted(combined_population, key=lambda x: x[1])
        self.population = combined_population[:self.population_size]



        return self

def split_equation2(equation):
    equation = equation.replace(" ", "")

    fullWords = []
    reducedWords = []
    result = ""

    pattern = r'(\w+|[()+*-])'
    matches = re.findall(pattern, equation)
    matches = matches[:-1]

    fullWords = [x for x in matches if x.isalpha() or x in ['(', ')', '+', '-', '*', '/']]
    reducedWords = [x for x in matches if x.isalpha()]

    equal_index = equation.index('=')
    result = equation[equal_index+1:].strip()

    return fullWords, reducedWords, result

# start time and memory calculator
tracemalloc.start()
start_time = time.time_ns()

str = "(A*A+B*B)*(C+D)-E*E*E-(F*G)=H"
fullWords, reducedWords, result = split_equation2(str)
problem = GE(fullWords, reducedWords, result)
solution = problem.GA()
solution.printGoal()

# stop time and memory calculator
memory = tracemalloc.get_traced_memory()[1]
total_time = (time.time_ns() - start_time) / 1000000
tracemalloc.stop()
print()
print("Memory used: ", memory / 1024**2, "MB")
print("Time used: ", total_time, "ms")